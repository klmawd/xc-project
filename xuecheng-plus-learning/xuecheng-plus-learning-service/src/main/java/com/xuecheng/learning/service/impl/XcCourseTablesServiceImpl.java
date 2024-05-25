package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.XcCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@Slf4j
public class XcCourseTablesServiceImpl extends ServiceImpl<XcCourseTablesMapper, XcCourseTables> implements XcCourseTablesService {

    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;


    //添加选课
    @Override
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {

        //查询课程发布表
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            throw new XueChengPlusException("该课程不存在");
        }
        String marketJson = coursepublish.getMarket();
        CourseMarket courseMarket = JSON.parseObject(marketJson, CourseMarket.class);

        //开始添加选课
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(coursepublish, xcChooseCourseDto);
        xcChooseCourseDto.setCourseId(coursepublish.getId());
        xcChooseCourseDto.setUserId(userId);
        xcChooseCourseDto.setCourseName(coursepublish.getName());
        xcChooseCourseDto.setCoursePrice(courseMarket.getPrice());
        xcChooseCourseDto.setCreateDate(LocalDateTime.now());
        xcChooseCourseDto.setValidDays(courseMarket.getValidDays());//有效期
        xcChooseCourseDto.setValidtimeStart(LocalDateTime.now());//有效期开始时间
        xcChooseCourseDto.setValidtimeEnd(LocalDateTime.now().plusDays(courseMarket.getValidDays()));//有效期结束时间

        XcChooseCourse chooseCourse = new XcChooseCourse();
        if (courseMarket.getCharge().equals("201000")) {

            xcChooseCourseDto.setOrderType("700001");//免费课程
            xcChooseCourseDto.setLearnStatus("702001");//学习状态设为正常学习
            xcChooseCourseDto.setStatus("701001");//选课状态为成功


            //向选课记录表xc_choose_course添加记录
            chooseCourse = insertChooseCourse(xcChooseCourseDto);

            //向我的课程表xc_course_tables添加记录
            insertCourseTables(chooseCourse);
        } else {

            xcChooseCourseDto.setOrderType("700002");//收费课程
            xcChooseCourseDto.setLearnStatus("702002"); //学习状态设为选课后没有支付
            xcChooseCourseDto.setStatus("701002");//选课状态未支付

            //向选课记录表xc_choose_course添加记录
            chooseCourse = insertChooseCourse(xcChooseCourseDto);

        }

        BeanUtils.copyProperties(chooseCourse, xcChooseCourseDto);
        return xcChooseCourseDto;
    }

    //查询查询学习资格
    @Override
    public XcCourseTablesDto getLearnstatus(String userId, Long courseId) {

        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        //查询课程表
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getCourseId, courseId)
                .eq(XcCourseTables::getUserId, userId));

        if (xcCourseTables == null) {
            //未选课或未支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }

        boolean before = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (before) {
            //过期了
            xcCourseTablesDto.setLearnStatus("702003");
        } else {
            //可以正常学习
            xcCourseTablesDto.setLearnStatus("702001");
        }
        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
        return xcCourseTablesDto;
    }

    //保存选课成功状态
    @Override
    @Transactional
    public boolean saveChooseCourseStatus(String chooseCourseId) {

        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (xcChooseCourse == null) {
            log.debug("选课id:{}.不存在对应的选课记录{}", chooseCourseId);
            return false;
        }


        //更新选课状态
        xcChooseCourse.setStatus("701001");//选课状态设为选课成功
        int i = xcChooseCourseMapper.updateById(xcChooseCourse);
        if (i <= 0) {
            log.error("更新选课状态失败:{}", xcChooseCourse);
            throw new XueChengPlusException("更新选课状态失败");
        }

        //在我的课程表添加记录
        insertCourseTables(xcChooseCourse);
        return true;


    }

    //向选课记录表xc_choose_course添加记录
    private XcChooseCourse insertChooseCourse(XcChooseCourseDto xcChooseCourseDto) {

        //判断课程是否已经添加
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectOne(new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getCourseId, xcChooseCourseDto.getCourseId())
                .eq(XcChooseCourse::getUserId, xcChooseCourseDto.getUserId()));
        if (xcChooseCourse != null) {
            //throw new XueChengPlusException("课程已添加，无法重复添加");
            return xcChooseCourse;
        }

        //添加
        xcChooseCourse = new XcChooseCourse();
        BeanUtils.copyProperties(xcChooseCourseDto, xcChooseCourse);

        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;
    }

    //向我的课程表xc_course_tables添加记录
    private XcCourseTables insertCourseTables(XcChooseCourse chooseCourse) {

        //判断课程是否已经添加
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getCourseId, chooseCourse.getCourseId())
                .eq(XcCourseTables::getUserId, chooseCourse.getUserId()));
        if (xcCourseTables != null) {
            throw new XueChengPlusException("课程已添加，无法重复添加");
        }

        //添加
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(chooseCourse, xcCourseTables);
        xcCourseTables.setChooseCourseId(chooseCourse.getId());
        xcCourseTablesMapper.insert(xcCourseTables);
        return xcCourseTables;
    }


}
