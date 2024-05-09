package com.xuecheng.content.jobhandler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.utils.JsonUtil;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.model.dto.CourseIndex;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private CoursePublishService coursePublishService;
    @Autowired
    private SearchServiceClient searchServiceClient;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex=" + shardIndex + ",shardTotal=" + shardTotal);

        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", 30, 60);


    }


    //课程发布任务
    @Override
    public boolean execute(MqMessage mqMessage) {

        try {
            //课程静态化
            generateCourseHtml(mqMessage);

            //课程索引
            saveCourseIndex(mqMessage);

            //课程缓存
            saveCourseCache(mqMessage);
        } finally {
            mqMessageService.updateById(mqMessage);
        }

        return true;
    }

    //课程静态化
    private void generateCourseHtml(MqMessage mqMessage) {
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        Integer stageState1 = Integer.valueOf((mqMessage.getStageState1()));
        if (stageState1 > 0) {
            //已经处理过第一阶段任务
            return;
        }
        //生成html静态页面
        File courseHtmlFile = coursePublishService.generateCourseHtml(courseId);
        if (courseHtmlFile != null) {
            //上传到minio
            coursePublishService.uploadCourseHtml(courseId, courseHtmlFile);
        }

        //第一阶段任务完成
        mqMessage.setStageState1("1");
    }

    //课程索引
    private void saveCourseIndex(MqMessage mqMessage) {
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        Integer stageState2 = Integer.valueOf((mqMessage.getStageState2()));
        if (stageState2 > 0) {
            //已经处理过第二阶段任务
            return;
        }

        CourseIndex courseIndex = new CourseIndex();
        //从课程发布表查询
        CoursePublish coursePublish = coursePublishService.getById(courseId);
        BeanUtils.copyProperties(coursePublish, courseIndex);

        //课程营销信息
        String courseMarketJson = coursePublish.getMarket();
        if(StringUtils.isNotEmpty(courseMarketJson)){
            CourseMarket courseMarket = JsonUtil.jsonToObject(courseMarketJson, CourseMarket.class);
            BeanUtils.copyProperties(courseMarket, courseIndex);
        }

        searchServiceClient.add(courseIndex);

        //第二阶段任务完成
        mqMessage.setStageState1("1");
    }

    //课程缓存
    private void saveCourseCache(MqMessage mqMessage) {

        Integer stageState3 = Integer.valueOf((mqMessage.getStageState3()));
        if (stageState3 > 0) {
            //已经处理过第三阶段任务
            return;
        }

        //第三阶段任务完成
        mqMessage.setStageState1("1");
    }
}
