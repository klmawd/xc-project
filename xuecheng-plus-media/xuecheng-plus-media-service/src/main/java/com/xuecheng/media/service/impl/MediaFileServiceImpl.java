package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.sun.javafx.scene.control.skin.VirtualFlow.ArrayLinkedList;
import com.xuecheng.base.ecxeption.XueChengPlusException;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaDivFiles;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaDivFilesService;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl extends ServiceImpl<MediaFilesMapper, MediaFiles> implements MediaFileService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MediaFileService getCurrentProxy;
    @Autowired
    private MediaFileService currentProxy;
    @Autowired
    private MediaProcessMapper mediaProcessMapper;
    @Autowired
    private MediaDivFilesService mediaDivFilesService;
    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;

    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    //根据扩展名获取mimeType
    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;

    }

    //上传文件到minio
    @Override
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)//桶
                    .filename(localFilePath) //指定本地文件路径
                    .object(objectName)//对象名 放在子目录下
                    .contentType(mimeType)//设置媒体文件类型
                    .build();
            //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{},错误信息:{}", bucket, objectName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}", bucket, objectName, e.getMessage());
        }
        return false;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //上传文件
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //先得到扩展名
        String extension = filename.substring(filename.lastIndexOf("."));

        //得到mimeType
        String mimeType = getMimeType(extension);

        //子目录,日期
        String defaultFolderPath = getDefaultFolderPath();
        //文件的md5值
        String fileMd5 = getFileMd5(new File(localFilePath));
        String objectName = defaultFolderPath + fileMd5 + extension;
        //上传文件到minio
        boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_mediafiles, objectName);
        if (!result) {
            throw new XueChengPlusException("上传文件失败");
        }


        //入库文件信息
        MediaFiles mediaFiles = getCurrentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
        if (mediaFiles == null) {
            throw new XueChengPlusException("文件上传后保存信息失败");
        }
        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);

        return uploadFileResultDto;
    }


    //将文件信息添加数据库
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //将文件信息保存到数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket);
            //file_path
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            //插入数据库
            int insert = mediaFilesMapper.insert(mediaFiles);

            if (insert <= 0) {
                log.debug("向数据库保存文件信息失败,bucket:{},objectName:{}", bucket, objectName);
                return null;
            }
            //记录待处理任务
            addWaitingTask(mediaFiles);

            return mediaFiles;
        }

        return mediaFiles;
    }

    //添加待处理任务
    private void addWaitingTask(MediaFiles mediaFiles) {
        String filename = mediaFiles.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        if (mimeType.equals("video/x-msvideo")) {
            //向media_process表写入数据
            MediaProcess mediaProcess = new MediaProcess();

            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);

            mediaProcessMapper.insert(mediaProcess);
        }
    }

    //检查文件是否存在
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //查询数据库
        MediaFiles mediaFile = this.getById(fileMd5);
        if (mediaFile != null) {
            //数据库存在。在minio中查询
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(mediaFile.getBucket())
                    .object(mediaFile.getFilePath())
                    .build();
            try (FilterInputStream inputStream = minioClient.getObject(getObjectArgs)) {
                if (inputStream != null) {
                    //文件存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //文件不存在
            return RestResponse.error(false);
        }
        //数据库中没有，文件不存在
        return RestResponse.error(false);
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    //检查分块文件
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(getChunkFileFolderPath(fileMd5) + chunkIndex)
                .build();
        try (FilterInputStream inputStream = minioClient.getObject(getObjectArgs)) {
            if (inputStream != null) {
                //分块文件存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //分块文件不存在
        return RestResponse.error(false);
    }

    //上传分块文件
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {

        String mimeType = getMimeType(null);

        //分块分件上传路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5);
        String objectName = chunkFilePath + chunk;

        boolean upload = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, objectName);

        if (!upload) {
            return RestResponse.validfail(false, "上传分块文件失败");
        }

        //分块文件信息入库
        mediaDivFilesService.saveDivFile(bucket_video, chunkFilePath, 60 * 30L);
        return RestResponse.success(true);
    }

    //根据文件的md5值和拓展名得到合并后的文件名
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    //合并分块文件
    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {

        //获取分块文件路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

        //组成将分块文件路径组成 List<ComposeSource>
        List<ComposeSource> source = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_video)
                        .object(chunkFileFolderPath + i)
                        .build())
                .collect(Collectors.toList());

        //源文件名
        String filename = uploadFileParamsDto.getFilename();
        //拓展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //合并后的文件名
        String objectName = getFilePathByMd5(fileMd5, extension);

        //指定合并后的objectName的信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName)//合并后的文件
                .sources(source)//源文件
                .build();

        //合并文件
        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错，bucket:{},objectName:{},错误信息:{}", bucket_video, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件异常");
        }

        //校验文件合并后是否与源文件一致，比较md5值
        //先从minio下载文件
        File file = downloadFileFromMinIO(bucket_video, objectName);

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            //计算合并后文件的md5
            String mergeFileMd5 = DigestUtils.md5Hex(fileInputStream);

            //比较原md5和mergeFileMd5
            if (!mergeFileMd5.equals(fileMd5)) {
                log.error("检验合并文件md5值不相同，原始文件md5:{},合并文件md5:{}", fileMd5, mergeFileMd5);
                return RestResponse.validfail(false, "文件校验失败");
            }

            //设置文件大小
            uploadFileParamsDto.setFileSize(file.length());
        } catch (Exception e) {
            return RestResponse.validfail(false, "文件校验失败");
        } finally {
            //删除下载的临时文件
            if (file != null && file.exists()) {
                file.delete();
            }
        }

        //文件信息入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "文件入库失败");
        }

        //清理分块文件
        mediaDivFilesService.clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);
    }

    //从minio上下载文件
    @Override
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //删除临时文件
            if (minioFile != null && minioFile.exists()) {
                minioFile.delete();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


}