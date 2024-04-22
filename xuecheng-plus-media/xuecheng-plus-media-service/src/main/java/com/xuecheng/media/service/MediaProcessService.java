package com.xuecheng.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

public interface MediaProcessService extends IService<MediaProcess> {

    List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    boolean startTask(long id);

    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);

}
