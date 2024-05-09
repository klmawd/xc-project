package com.xuecheng.content.api;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.dto.UpdateCourseDto;
import com.xuecheng.content.model.po.CourseBase;

import com.xuecheng.content.service.CourseBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.Resources;

@Api(value = "课程基本信息接口" ,tags = "课程基本信息接口" )
@RestController
@RequestMapping("/course")
public class CourseBaseController {

    @Autowired
    private CourseBaseService courseBaseService;

    //分页查询课程信息
    @ApiOperation("分页查询课程信息接口")
    @PostMapping("/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {

        return courseBaseService.list(pageParams, queryCourseParamsDto);
    }

    //新增课程
    @PostMapping
    public CourseBaseDto addCourse(@RequestBody @Validated AddCourseDto addCourseDto) {

        Long companyId = 12332141425L;
        return courseBaseService.addCourse(companyId, addCourseDto);
    }

    //根据课程id查询课程
    @GetMapping("/{courseId}")
    public CourseBaseDto queryCourse(@PathVariable("courseId") Long courseId) {
        return courseBaseService.getByIdCourseBaseDto(courseId);
    }

    //修改课程
    @PutMapping
    public CourseBaseDto updateCourse(@RequestBody @Validated UpdateCourseDto updateCourseDto){
        Long companyId = 12332141425L;
        courseBaseService.updateCourse(companyId, updateCourseDto);
        return null;
    }
}
