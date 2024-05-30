package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.config.CoursePublishConfig;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CourseBaseDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.*;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 课程发布 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CoursePublishServiceImpl extends ServiceImpl<CoursePublishMapper, CoursePublish> implements CoursePublishService {

    @Autowired
    private CourseBaseService courseBaseService;
    @Autowired
    private TeachplanService teachplanService;
    @Autowired
    private CoursePublishMapper coursePublishMapper;
    @Autowired
    private CourseCategoryService courseCategoryService;
    @Autowired
    private CourseMarketService courseMarketService;
    @Autowired
    private CourseTeacherService courseTeacherService;
    @Autowired
    private CoursePublishPreService coursePublishPreService;
    @Autowired
    private CoursePublishService coursePublishService;
    @Autowired
    private MqMessageService mqMessageService;
    @Autowired
    private MediaServiceClient mediaServiceClient;
    @Autowired
    private SearchServiceClient searchServiceClient;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    TransactionTemplate transactionTemplate;

    //课程预览
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        //课程基本信息、营销信息
        CourseBaseDto courseBaseInfo = courseBaseService.getByIdCourseBaseDto(courseId);

        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.qureyCourseplanByCourseId(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    //提交课程审核
    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {

        CourseBase courseBase = courseBaseService.getById(courseId);
        //1、对已提交审核的课程不允许提交审核。
        if (courseBase.getAuditStatus().equals("202003")) {
            throw new XueChengPlusException("该课程已经提交审核");
        }
        //2、本机构只允许提交本机构的课程。
        if (!courseBase.getCompanyId().equals(companyId)) {
            throw new XueChengPlusException("只能提交本机构的课程");
        }
        //3、没有上传图片不允许提交审核。
        if (StringUtils.isEmpty(courseBase.getPic())) {
            throw new XueChengPlusException("请先上传课程图片");
        }
        //4、没有添加课程计划不允许提交审核。
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        List<Teachplan> teachplans = teachplanService.list(queryWrapper);
        if (teachplans == null) {
            throw new XueChengPlusException("请先添加课程计划");
        }

        //向课程预览表添加数据
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        //课程基本信息
        BeanUtils.copyProperties(courseBase, coursePublishPre);
        //大分类名称
        String mtName = courseCategoryService.getById(courseBase.getMt()).getName();
        //小分类名称
        String stName = courseCategoryService.getById(courseBase.getSt()).getName();
        coursePublishPre.setStName(stName);
        coursePublishPre.setMtName(mtName);

        //课程营销信息
        CourseMarket courseMarket = courseMarketService.getById(courseId);
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);

        //课程计划信息
        String teachplansJson = JSON.toJSONString(teachplans);
        coursePublishPre.setTeachplan(teachplansJson);

        //课程教师信息
        LambdaQueryWrapper<CourseTeacher> courseTeacherWrapper = new LambdaQueryWrapper<>();
        courseTeacherWrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> teachers = courseTeacherService.list(courseTeacherWrapper);
        String teachersJson = JSON.toJSONString(teachers);
        coursePublishPre.setTeachers(teachersJson);

        //设置课程状态已提交
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCreateDate(LocalDateTime.now());

        CoursePublishPre publishPre = coursePublishPreService.getById(courseId);
        if (publishPre != null) {
            //更新
            coursePublishPreService.updateById(coursePublishPre);
        } else {
            //插入
            coursePublishPreService.save(coursePublishPre);
        }

        //设置课程状态已提交
        courseBase.setAuditStatus("202003");
        courseBaseService.updateById(courseBase);

    }

    //课程发布
    @Override
   // @Transactional
    public void coursepublish(Long companyId, Long courseId) {

        CoursePublishPre publishPre = coursePublishPreService.getById(courseId);
        //只有本机构能发布该课程
        if (!publishPre.getCompanyId().equals(companyId)) {
            throw new XueChengPlusException("不能发布其他机构的课程");
        }
        //课程审核通过才能发布
        if (!publishPre.getStatus().equals("202004")) {
            throw new XueChengPlusException("该课程还未通过审核");
        }

        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(publishPre, coursePublish);
        coursePublish.setCreateDate(LocalDateTime.now());
        coursePublish.setOnlineDate(LocalDateTime.now());
        coursePublish.setStatus("202004");


        MqMessage mqMessage = transactionTemplate.execute(new TransactionCallback<MqMessage>() {
            @Override
            public MqMessage doInTransaction(TransactionStatus transactionStatus) {
                try {
                    //向course_publish表插入数据
                    if (coursePublishService.getById(courseId) != null) {
                        coursePublishService.updateById(coursePublish);
                    } else {
                        coursePublishService.save(coursePublish);
                    }

//        //删除course_publish_pre表数据
//        coursePublishPreService.removeById(courseId);

                    //将course_base表中相关记录的发布状态改为已发布
                    CourseBase courseBase = courseBaseService.getById(courseId);
                    courseBase.setStatus("203002");
                    courseBaseService.updateById(courseBase);

                    //向mq_message表插入数据
                    MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
                    return mqMessage;
                } catch (Exception e) {
                    transactionStatus.setRollbackOnly();
                    return null;
                }
            }
        });


        //通知课程已发布
        notifyCoursePublish(mqMessage.getId());

    }

    //通知课程已发布
    public void notifyCoursePublish(Long mqMessageId) {

        //1、消息体，转json
        String msg = JSON.toJSONString(mqMessageId);
        //设置消息持久化
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(mqMessageId.toString());
        // 3.添加callback,
        correlationData.getFuture().addCallback(
                result -> {
                    if (result.isAck()) {
                        // 3.1.ack，消息成功
                        log.debug("课程发布消息发送成功, ID:{}", correlationData.getId());
                        //删除消息表中的记录
                        mqMessageService.completed(mqMessageId);
                    } else {
                        // 3.2.nack，消息失败
                        log.error("课程发布消息发送失败, ID:{}, 原因{}", correlationData.getId(), result.getReason());
                    }
                },
                ex -> log.error("消息发送异常, ID:{}, 原因{}", correlationData.getId(), ex.getMessage())
        );

        //发送消息
        rabbitTemplate.convertAndSend(CoursePublishConfig.COURSEPUBLISH_EXCHANGE_FANOUT, CoursePublishConfig.COURSEPUBLISH_QUEUE, msgObj, correlationData);
    }


    //生成课程预览静态页面
    @Override
    public File generateCourseHtml(Long courseId) {
        //静态化文件
        File htmlFile = null;
        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            //创建静态化文件
            htmlFile = File.createTempFile("course", ".html");
            log.debug("课程静态化，生成静态文件:{}", htmlFile.getAbsolutePath());
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}", e.toString());
            throw new XueChengPlusException("课程静态化异常");
        }

        return htmlFile;
    }

    //上传课程预览静态页面
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadFile(multipartFile, "course/" + courseId + ".html");

        //删除临时文件
        if (file != null && file.exists()) {
            file.delete();
        }
        if (course == null) {
            throw new XueChengPlusException("上传静态文件异常");
        }
    }

    //课程下架
    @Override
    @Transactional
    public void courseoffline(Long companyId, Long courseId) {
        CourseBase courseBase = courseBaseService.getById(courseId);
        if (!companyId.equals(courseBase.getCompanyId())) {
            throw new XueChengPlusException("不能下架其他机构的课程");
        }

        //修改course_base表为下架状态
        courseBase.setStatus("203001");
        courseBaseService.updateById(courseBase);

        //删除课程发布表数据
        coursePublishService.removeById(courseId);

        //删除redis中缓存的数据
        redisTemplate.delete("course:" + courseId);

        //删除minio中静态页面
        mediaServiceClient.fileRemove(courseId + ".html");

        //删除es中索引信息
        searchServiceClient.delete(courseId);
    }

    //查询课程发布
    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish;
    }

    //从缓存查询课程信息
    @Override
    public CoursePublish getCoursePublishCache(Long courseId) {

        String coursePublishJson = (String) redisTemplate.opsForValue().get("course:" + courseId);
        if (!StringUtil.isEmpty(coursePublishJson)) {
            //redis存在
            return JSON.parseObject(coursePublishJson, CoursePublish.class);
        }
        CoursePublish coursePublish;

        //从数据库查
        synchronized (this) {
            coursePublishJson = (String) redisTemplate.opsForValue().get("course:" + courseId);
            if (!StringUtil.isEmpty(coursePublishJson)) {
                //redis存在
                return JSON.parseObject(coursePublishJson, CoursePublish.class);
            }

            //从数据库查
            coursePublish = getCoursePublish(courseId);
            if (coursePublish != null) {
                coursePublishJson = JSON.toJSONString(coursePublish);
                redisTemplate.opsForValue().set("course:" + courseId, coursePublishJson, 10L + new Random().nextInt(5), TimeUnit.MINUTES);
            } else {
                //数据库不存在，缓存"null":缓存穿透问题
                redisTemplate.opsForValue().set("course:" + courseId, "null", 1L, TimeUnit.MINUTES);
            }
        }
        return coursePublish;
    }
}
