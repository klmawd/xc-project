package com.xuecheng.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.media.model.po.MediaDivFiles;

import java.util.List;

public interface MediaDivFilesService extends IService<MediaDivFiles> {

    void saveDivFile(String bucket, String filePath, Long validity);

    void clearChunkFiles(String chunkFileFolderPath, int chunkTotal);

    List<MediaDivFiles> getDivFile(int shardIndex, int shardTotal);
}
