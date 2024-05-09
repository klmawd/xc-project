package com.xuecheng;

import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.messagesdk.mapper.MqMessageMapper;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CTest {

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private MqMessageService mqMessageService;

    @Autowired
    private MqMessageMapper mqMessageMapper;

    @Test
    public void test() {
        List<MediaFiles> list = mediaFileService.list();
        System.out.println(list);
        MqMessageService mqMessageService1 = mqMessageService;
        MqMessageMapper mqMessageMapper1 = mqMessageMapper;


        //List<MqMessage> list1 = mqMessageService.list();

        MqMessage mqMessage = mqMessageMapper.selectById("f29a3149742940be8a4e9909f32003b0");

        System.out.println(mqMessage);
    }
}
