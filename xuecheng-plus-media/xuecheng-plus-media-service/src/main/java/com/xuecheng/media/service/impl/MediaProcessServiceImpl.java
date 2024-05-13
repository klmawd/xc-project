package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.MediaProcessService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MediaProcessServiceImpl extends ServiceImpl<MediaProcessMapper, MediaProcess> implements MediaProcessService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;



    //查询待处理任务，shardIndex:处理器序号,shardTotal:处理器总数,count:最大任务数量
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    //return true开启任务成功，false开启任务失败
    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result <= 0 ? false : true;
    }

    //保存任务结果
    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {

        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) {
            return;
        }

        //任务执行失败
        if (status.equals("3")) {
            //更新media_process表
            mediaProcess.setStatus(status);
            mediaProcess.setFailCount(mediaProcess.getFailCount() + 1);//失败次数+1
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            return;
        }

        //任务执行成功
        //更新media_files表
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);

        //将media_process表相关记录插入到media_process_history表并删除
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();

        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistory.setUrl(url);
        mediaProcessHistory.setStatus(status);
        mediaProcessHistory.setCreateDate(LocalDateTime.now());
        mediaProcessHistory.setFinishDate(LocalDateTime.now());

        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        mediaProcessMapper.deleteById(mediaProcess);
    }
}
