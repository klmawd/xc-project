package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.media.mapper.MediaDivFilesMapper;
import com.xuecheng.media.model.po.MediaDivFiles;
import com.xuecheng.media.service.MediaDivFilesService;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class MediaDivFilesServiceImpl extends ServiceImpl<MediaDivFilesMapper, MediaDivFiles> implements MediaDivFilesService {

    @Autowired
    private MediaDivFilesMapper mediaDivFilesMapper;

    @Autowired
    MinioClient minioClient;

    //保存分块文件信息
    @Override
    public synchronized void saveDivFile(String bucket, String filePath, Long validity) {
        //先判断media_div_files中是否有记录
        LambdaQueryWrapper<MediaDivFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(filePath != null, MediaDivFiles::getFilePath, filePath);
        MediaDivFiles divFiles = mediaDivFilesMapper.selectOne(queryWrapper);
        if (divFiles == null) {
            //没有则插入
            MediaDivFiles mediaDivFile = new MediaDivFiles();

            mediaDivFile.setFilePath(filePath);
            mediaDivFile.setBucket(bucket);
            mediaDivFile.setValidity(validity);
            mediaDivFile.setCreateDate(LocalDateTime.now());
            mediaDivFile.setCount(1);

            mediaDivFilesMapper.insert(mediaDivFile);
        } else {
            //有则count+1再更新
            divFiles.setCount(divFiles.getCount() + 1);
            divFiles.setCreateDate(LocalDateTime.now());

            mediaDivFilesMapper.updateById(divFiles);
        }
    }

    //清理分块文件
    @Override
    public void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {

        //清理文件
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath + i))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);

            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }

        //清理media_div_files表中数据
        LambdaQueryWrapper<MediaDivFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(chunkFileFolderPath != null, MediaDivFiles::getFilePath, chunkFileFolderPath);
        mediaDivFilesMapper.delete(queryWrapper);
    }

    //查询分开文件信息
    @Override
    public List<MediaDivFiles> getDivFile(int shardIndex, int shardTotal) {
        return mediaDivFilesMapper.selectDivFile(shardTotal,shardIndex);
    }
}
