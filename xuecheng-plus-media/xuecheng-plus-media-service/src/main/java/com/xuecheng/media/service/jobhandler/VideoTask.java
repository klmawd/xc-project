package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaDivFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaDivFilesService;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.MediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VideoTask {

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private MediaProcessService mediaProcessService;

    @Autowired
    private MediaDivFilesService mediaDivFilesService;

    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    //定时处理视频文件
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //查询任务
        List<MediaProcess> mediaProcessList = mediaProcessService.getMediaProcessList(shardIndex, shardTotal, 5);
        //任务数量
        int size = mediaProcessList.size();

        //cpu核心数
        int processors = Runtime.getRuntime().availableProcessors();
        //计数器，保证所有线程执行完才退出
        CountDownLatch countDownLatch = new CountDownLatch(size);

        //创建线程池处理任务
        ExecutorService executorService = Executors.newFixedThreadPool(processors);
        mediaProcessList.forEach(mediaProcess -> {
            //提交任务
            executorService.execute(() -> {
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //执行任务
                    boolean startTask = mediaProcessService.startTask(taskId);
                    if (!startTask) {
                        log.debug("任务抢占失败，任务id：{}", taskId);
                        return;
                    }
                    //视频文件的md5值
                    String fileId = mediaProcess.getFileId();
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();

                    //视频转码
                    //将要处理的视频文件下载到服务器上
                    File originalFile = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (originalFile == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", bucket.concat(objectName));
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载待处理文件失败");
                        return;
                    }


                    //源avi文件路径
                    String originalPath = originalFile.getAbsolutePath();
                    //转码后的mp4文件名称，MD5+".mp4"
                    String mp4Name = fileId + ".mp4";
                    //转码后的mp4文件url
                    String url = getFilePath(fileId, ".mp4");

                    //创建临时文件，作为转码后的文件
                    File minio = null;
                    try {
                        minio = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件失败，{}", e.getMessage());
                    }
                    String mp4Path = minio.getAbsolutePath();
                    //开始视频转换
                    Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegpath, originalPath, mp4Name, mp4Path);
                    String result = mp4VideoUtil.generateMp4();
                    if (!result.equals("success")) {
                        //转换失败
                        log.debug("视频转换失败，原因：{}，bucket：{}，objectName：{}", result, bucket, objectName);
                        //保存失败结果
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, result);
                        //删除临时文件
                        if (minio != null && minio.exists()) {
                            minio.delete();
                        }
                        //删除临时文件
                        if (originalFile != null && originalFile.exists()) {
                            originalFile.delete();
                        }
                        return;
                    }
                    //转换成功
                    //上传到minio
                    boolean upload = mediaFileService.addMediaFilesToMinIO(mp4Path, "video/mp4", bucket, mp4Name);
                    if (!upload) {
                        log.debug("上传文件到minio失败，任务id：{}", taskId);
                        //删除临时文件
                        if (minio != null && minio.exists()) {
                            minio.delete();
                        }
                        //删除临时文件
                        if (originalFile != null && originalFile.exists()) {
                            originalFile.delete();
                        }
                        return;
                    }
                    //删除临时文件
                    if (minio != null && minio.exists()) {
                        minio.delete();
                    }
                    //删除临时文件
                    if (originalFile != null && originalFile.exists()) {
                        originalFile.delete();
                    }
                    //保存任务处理结果
                    mediaProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                } finally {
                    countDownLatch.countDown();
                }
            });
        });

        //等待所有线程处理完成
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    //定时清理分块文件
    @XxlJob("videoCleanChunkHandler")
    public void videoCleanChunkHandler() {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //扫描media_div_files表
        List<MediaDivFiles> divFiles = mediaDivFilesService.getDivFile(shardIndex, shardTotal);
        divFiles.forEach(divFile -> {
            //保存分块文件的时间
            LocalDateTime createDate = divFile.getCreateDate();
            long createTime = createDate.toEpochSecond(ZoneOffset.ofHours(8));
            //当前时间
            long nowTime = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
            //有效期（s）
            Long validity = divFile.getValidity();

            if (nowTime - createTime > validity) {
                //已经超过有效期,应该删除分块文件
                String filePath = divFile.getFilePath();
                int divFileCount = divFile.getCount();
                mediaDivFilesService.clearChunkFiles(filePath,divFileCount);
            }
        });
    }

}
