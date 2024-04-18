package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 课程-教师关系表 前端控制器
 * </p>
 * 、
 *
 * @author itcast
 */
@Slf4j
@RestController
@RequestMapping("courseTeacher")
public class CourseTeacherController {

    @Autowired
    private CourseTeacherService courseTeacherService;

    @GetMapping("/list/{courseId}")
    public List<CourseTeacher> list(@PathVariable("courseId") Long courseId) {
        return courseTeacherService.getByCouserId(courseId);
    }

    //新增和修改课程教师
    @PostMapping
    public void addCourseTeacher(@RequestBody @Validated CourseTeacher courseTeacher) {
        courseTeacherService.addCourseTeacher(courseTeacher);
    }

    //删除课程老师
    @DeleteMapping("/course/{courseId}/{teacherId}")
    public void deleteCourseTeacher(@PathVariable("courseId") Long courseId, @PathVariable("teacherId") Long teacherId) {
        courseTeacherService.deleteCourseTeacher(courseId,teacherId);
    }
}
