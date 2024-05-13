package com.xuecheng.content.feignclient;

import com.xuecheng.content.model.dto.CourseIndex;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient> {


    @Override
    public SearchServiceClient create(Throwable throwable) {
        return new SearchServiceClient() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                log.error("添加课程索引失败，索引信息{},熔断异常:{}", courseIndex,throwable.getMessage());
                return false;
            }

            @Override
            public Boolean delete(Long courseId) {
                log.error("删除课程索引失败，课程id{},熔断异常:{}", courseId,throwable.getMessage());
                return false;
            }

        };
    }
}

