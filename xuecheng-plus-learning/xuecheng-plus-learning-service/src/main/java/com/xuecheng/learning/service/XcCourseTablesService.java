package com.xuecheng.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;


public interface XcCourseTablesService extends IService<XcCourseTables> {

    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    XcCourseTablesDto getLearnstatus(String userId, Long courseId);
}
