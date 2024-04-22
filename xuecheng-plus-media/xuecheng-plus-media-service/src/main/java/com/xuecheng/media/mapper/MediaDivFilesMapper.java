package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaDivFiles;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MediaDivFilesMapper extends BaseMapper<MediaDivFiles> {

    @Select("select * from media_div_files where id % #{shardTotal} = #{shardIndex}")
    List<MediaDivFiles> selectDivFile(@Param("shardTotal") int shardTotal, @Param("shardIndex") int shardIndex);
}
