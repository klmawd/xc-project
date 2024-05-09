package com.xuecheng;

import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.mapper.MqMessageMapper;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import org.junit.Test;
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

    @Test
    public void select() {
        CoursePublish coursePublish = coursePublishService.getById(2L);
        System.out.println(coursePublish);
    }
}
