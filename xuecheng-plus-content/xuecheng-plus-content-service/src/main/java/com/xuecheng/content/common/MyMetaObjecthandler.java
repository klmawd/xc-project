package com.xuecheng.content.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjecthandler implements MetaObjectHandler {


    //插入操作时的公共字段自动填充
    @Override
    public void insertFill(MetaObject metaObject) {
        metaObject.setValue("createDate", LocalDateTime.now());
        metaObject.setValue("changeDate", LocalDateTime.now());

    }

    //更新操作时的公共字段自动填充
    @Override
    public void updateFill(MetaObject metaObject) {

        metaObject.setValue("changeDate", LocalDateTime.now());

    }
}