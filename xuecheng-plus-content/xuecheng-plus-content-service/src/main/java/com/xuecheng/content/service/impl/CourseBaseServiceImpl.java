package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.ecxeption.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.dto.UpdateCourseDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseService;
import com.xuecheng.content.service.CourseCategoryService;
import com.xuecheng.content.service.CourseMarketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 课程基本信息 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseBaseServiceImpl extends ServiceImpl<CourseBaseMapper, CourseBase> implements CourseBaseService {
    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseMarketService courseMarketService;

    @Autowired
    private CourseCategoryService courseCategoryService;

    //分页查询课程信息
    @Override
    public PageResult<CourseBase> list(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        Long pageNo = pageParams.getPageNo();
        Long pageSize = pageParams.getPageSize();

        //分页构造器
        Page<CourseBase> pageInfo = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();


        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());

        this.page(pageInfo, queryWrapper);

        List<CourseBase> courseBases = pageInfo.getRecords();
        long total = pageInfo.getTotal();
        PageResult<CourseBase> pageResult = new PageResult<>(courseBases, total, pageNo, pageSize);

        return pageResult;
    }

    //新增课程
    @Override
    @Transactional
    public CourseBaseDto addCourse(Long companyId, AddCourseDto addCourseDto) {

        //课程基本信息表数据
        CourseBase courseBase = new CourseBase();
        courseBase.setCompanyId(companyId);
        BeanUtils.copyProperties(addCourseDto, courseBase);
        //设置审核状态
        courseBase.setAuditStatus("202002");
        //设置发布状态
        courseBase.setStatus("203001");
        //机构id
        courseBase.setCompanyId(companyId);

        boolean save = this.save(courseBase);
        if (save == false) {
            throw new XueChengPlusException("新增课程失败");
        }

        //课程营销表数据
        CourseMarket courseMarket = new CourseMarket();
        Long courseId = courseBase.getId();
        courseMarket.setId(courseId);
        BeanUtils.copyProperties(addCourseDto, courseMarket);
        if (!addCourseMarket(courseMarket)) {
            throw new XueChengPlusException("新增课程失败");
        }

        return getByIdCourseBaseDto(courseId);
    }

    //向课程营销表添加数据
    private boolean addCourseMarket(CourseMarket courseMarket) {

        String charge = courseMarket.getCharge();

        //收费
        if (charge.equals("201001")) {
            if (courseMarket.getPrice() == null || courseMarket.getPrice() <= 0) {
                throw new XueChengPlusException("价格为空或不大于0");
            }
        }
        //先查看课程营销表是否有记录，有则改，无则增
        CourseMarket market = courseMarketService.getById(courseMarket.getId());
        if (market == null) {
            //插入数据
            return courseMarketService.save(courseMarket);
        } else {
            //更新数据
            return courseMarketService.updateById(courseMarket);
        }
    }

    //根据课程id查询课程基本信息，包括基本信息和营销信息
    @Override
    public CourseBaseDto getByIdCourseBaseDto(Long id) {

        CourseBaseDto courseBaseDto = new CourseBaseDto();

        //课程基本信息
        CourseBase courseBase = this.getById(id);
        if (courseBase == null) {
            return null;
        }
        BeanUtils.copyProperties(courseBase, courseBaseDto);

        //设置大分类名称
        CourseCategory courseCategoryMt = courseCategoryService.getById(courseBase.getMt());
        courseBaseDto.setMtName(courseCategoryMt.getName());

        //设置小分类名称
        CourseCategory courseCategorySt = courseCategoryService.getById(courseBase.getSt());
        courseBaseDto.setStName(courseCategorySt.getName());

        //课程营销信息
        CourseMarket courseMarket = courseMarketService.getById(id);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseDto);
        }

        return courseBaseDto;
    }

    //修改课程信息
    @Transactional
    @Override
    public CourseBaseDto updateCourse(Long companyId, UpdateCourseDto updateCourseDto) {

        Long courseId = updateCourseDto.getId();
        CourseBase courseBase = this.getById(courseId);
        if (courseBase == null) {
            throw new XueChengPlusException("不存在该课程");
        }

        if (!companyId.equals(courseBase.getCompanyId())) {
            throw new XueChengPlusException("只能修改本机构的课程");
        }

        //课程基本信息
        BeanUtils.copyProperties(updateCourseDto, courseBase);
        if (!this.updateById(courseBase)) {
            throw new XueChengPlusException("修改课程失败");
        }

        //课程营销信息
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(updateCourseDto, courseMarket);
        if (!addCourseMarket(courseMarket)) {
            throw new XueChengPlusException("修改课程失败");
        }

        return getByIdCourseBaseDto(courseId);
    }
}
