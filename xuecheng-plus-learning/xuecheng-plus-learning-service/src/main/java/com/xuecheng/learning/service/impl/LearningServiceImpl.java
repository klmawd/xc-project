package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.XcCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LearningServiceImpl implements LearningService {

    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    XcCourseTablesService xcCourseTablesService;
    @Autowired
    MediaServiceClient mediaServiceClient;


    //获取视频
    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {

        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            return RestResponse.validfail("课程不存在");
        }

        //判断视频是否可以试学
        String teachplanJson = coursepublish.getTeachplan();
        List<Teachplan> teachplans = JSON.parseArray(teachplanJson, Teachplan.class);
        for (Teachplan teachplan : teachplans) {
            if (teachplan.getId().equals(teachplanId)) {
                if(teachplan.getIsPreview().equals("1")){
                    //可以试学，远程调用媒资服务返回视频url
                    return mediaServiceClient.getPlayUrlByMediaId(mediaId);
                }
                break;
            }
        }

        String marketJson = coursepublish.getMarket();
        CourseMarket courseMarket = JSON.parseObject(marketJson, CourseMarket.class);

        //学习资格判断
        if (courseMarket.getCharge().equals("201000")) {
            //免费课程，正常学习，远程调用媒资服务返回视频url
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }
        //收费课程，需要登录并且购买
        if (userId == null) {
            return RestResponse.validfail("未登录");
        }
        //学习资格状态
        // [{"code":"702001","desc":"正常学习"},
        // {"code":"702002","desc":"没有选课或选课后没有支付"},
        // {"code":"702003","desc":"已过期需要申请续期或重新支付"}]
        XcCourseTablesDto xcCourseTablesDto = xcCourseTablesService.getLearnstatus(userId, courseId);
        String learnStatus = xcCourseTablesDto.getLearnStatus();
        if (learnStatus.equals("702002")) {
            return RestResponse.validfail("没有选课或选课后没有支付");
        }
        if (learnStatus.equals("702003")) {
            return RestResponse.validfail("课程已过期需要申请续期或重新支付");
        }

        //正常学习，远程调用媒资服务返回视频url
        return mediaServiceClient.getPlayUrlByMediaId(mediaId);
    }
}
