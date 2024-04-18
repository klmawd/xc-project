package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.dto.UpdateCourseDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * -+
 * <p>
 * 课程基本信息 服务类
 * </p>
 *
 * @author itcast
 * @since 2024-04-11
 */
public interface CourseBaseService extends IService<CourseBase> {
    PageResult<CourseBase> list(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    CourseBaseDto addCourse(Long companyId, AddCourseDto addCourseDto);

    CourseBaseDto getByIdCourseBaseDto(Long id);

    CourseBaseDto updateCourse(Long companyId, UpdateCourseDto updateCourseDto);
}
