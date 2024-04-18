package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;

import java.util.List;

/**
 * <p>
 * 课程计划 服务类
 * </p>
 *
 * @author itcast
 * @since 2024-04-11
 */
public interface TeachplanService extends IService<Teachplan> {
    List<TeachplanDto> qureyCourseplanByCourseId(Long courseId);


    void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    void deleteTeachplan(Long teachplanId);

    void teacheplanMoveup(Long teachplanId);

    void teacheplanMovedown(Long teachplanId);
}
