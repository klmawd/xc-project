package com.xuecheng;

import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.messagesdk.mapper.MqMessageMapper;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;

//@SpringBootTest
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

    @Test
    public void test1() {



        A sa = new A(1, "a");
        A sb = new A(1, "a");

        System.out.println(sa);
        System.out.println(sb);

        System.out.println(sa == sb);
        System.out.println(sa.equals(sb));
    }

    public class A {
        Integer i;
        String s;

        public A(Integer i, String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public String toString() {
            return "A{" +
                    "i=" + i +
                    ", s='" + s + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            A a = (A) o;
            return Objects.equals(i, a.i) && Objects.equals(s, a.s);
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, s);
        }
    }


    @Test
    public void test2(){
        String a=" ";
        boolean blank = StringUtils.isBlank(a);
        System.out.println(blank);
        blank=StringUtils.isEmpty(a);
        System.out.println(blank);
    }

}
