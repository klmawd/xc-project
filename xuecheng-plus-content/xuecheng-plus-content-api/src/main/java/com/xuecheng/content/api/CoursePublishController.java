package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * <p>
 * 课程发布 前端控制器
 * </p>
 *
 * @author itcast
 */
@Slf4j
@RestController
public class CoursePublishController {

    @Autowired
    private CoursePublishService coursePublishService;

    @ApiOperation("课程预览")
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId) {

        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model", coursePreviewInfo);
        modelAndView.setViewName("course_template");
        return modelAndView;

    }

    @ApiOperation("提交审核")
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId, courseId);
    }

    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.coursepublish(companyId, courseId);
    }

    @ApiOperation("课程下架")
    @ResponseBody
    @GetMapping("/courseoffline/{courseId}")
    public void courseoffline(@PathVariable("courseId") Long courseId) {
        Long companyId = 1232141425L;
        coursePublishService.courseoffline(companyId, courseId);
    }

}
