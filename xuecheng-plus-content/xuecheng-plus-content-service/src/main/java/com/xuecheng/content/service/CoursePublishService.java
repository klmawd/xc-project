package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

/**
 * <p>
 * 课程发布 服务类
 * </p>
 *
 * @author itcast
 * @since 2024-04-11
 */
public interface CoursePublishService extends IService<CoursePublish> {
    CoursePreviewDto getCoursePreviewInfo(Long courseId);
    void commitAudit(Long companyId, Long courseId);
    void coursepublish(Long companyId, Long courserId);
    File generateCourseHtml(Long courseId);
    void uploadCourseHtml(Long courseId, File file);
    void courseoffline(Long companyId, Long courseId);
}
