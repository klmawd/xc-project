package com.xuecheng.content.feignclient;

import com.xuecheng.content.model.dto.CourseIndex;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "search", fallbackFactory = SearchServiceClientFallbackFactory.class)
public interface SearchServiceClient {

    @ApiOperation("添加课程索引")
    @PostMapping("/search/index/course")
    Boolean add(@RequestBody CourseIndex courseIndex);

    @ApiOperation("删除课程索引")
    @DeleteMapping("/search/index/course/courseId")
    Boolean delete(@RequestBody Long courseId);

}