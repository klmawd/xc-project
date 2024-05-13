package com.xuecheng;

import com.xuecheng.base.utils.JsonUtil;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.model.dto.CourseIndex;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.mapper.MqMessageMapper;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CTest {
    @Autowired
    private MqMessageService mqMessageService;

    @Autowired
    private MqMessageMapper mqMessageMapper;

    @Autowired
    private CoursePublishService coursePublishService;
    @Autowired
    private SearchServiceClient searchServiceClient;

    @Test
    public void select() {
        CoursePublish coursePublish = coursePublishService.getById(2L);
        System.out.println(coursePublish);
    }

    @Test
    public void f(){
        CourseIndex courseIndex = new CourseIndex();
        //从课程发布表查询
        CoursePublish coursePublish = coursePublishService.getById(190L);
        BeanUtils.copyProperties(coursePublish, courseIndex);

        //课程营销信息
        String courseMarketJson = coursePublish.getMarket();
        if(StringUtils.isNotEmpty(courseMarketJson)){
            CourseMarket courseMarket = JsonUtil.jsonToObject(courseMarketJson, CourseMarket.class);
            BeanUtils.copyProperties(courseMarket, courseIndex);
        }

        searchServiceClient.add(courseIndex);
    }
}
