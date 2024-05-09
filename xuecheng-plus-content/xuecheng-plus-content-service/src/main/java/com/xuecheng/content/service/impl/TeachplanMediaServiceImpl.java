package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class TeachplanMediaServiceImpl extends ServiceImpl<TeachplanMediaMapper, TeachplanMedia> implements TeachplanMediaService {

    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;

    @Autowired
    private TeachplanService teachplanService;

    //教学计划-媒资绑定提交数据
    @Override
    @Transactional
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {

        //先删除
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(teachplanId != null, TeachplanMedia::getTeachplanId, teachplanId);
        teachplanMediaMapper.delete(queryWrapper);

        //插入数据
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto, teachplanMedia);
        //设置课程id
        Teachplan teachplan = teachplanService.getById(teachplanId);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFileName(bindTeachplanMediaDto.getFileName());

        teachplanMediaMapper.insert(teachplanMedia);
    }
}
