package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.ecxeption.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;

    @Autowired
    private TeachplanMediaService teachplanMediaService;

    //根据课程id查询教学计划
    @Override
    public List<TeachplanDto> qureyCourseplanByCourseId(Long courseId) {


        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);

//        Map<Long, TeachplanDto> teachplanDtoMap = teachplanDtos.stream().collect(Collectors.toMap(key -> key.getId(), vaule -> vaule));
//
//        List<TeachplanDto> teachplanDtoList = new ArrayList<>();
//        teachplanDtos.stream().forEach(item -> {
//
//            Long parentid = item.getParentid();
//            TeachplanDto teachplanDto = teachplanDtoMap.get(parentid);
//
//            //parentid为0是大章节
//            if (parentid == 0) {
//                teachplanDtoList.add(item);
//            } else {
//                //判断父节点是否存在
//                if (teachplanDto == null) {
//                    teachplanDto = new TeachplanDto();
//                }
//                teachplanDto.getTeachPlanTreeNodes().add(item);
//            }
//        });
        return teachplanDtos;
    }

    //得到同级章节,便于计算排序字段
    private List<Teachplan> getTeachplanList(long courseId, long parentId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        queryWrapper.eq(Teachplan::getParentid, parentId);
        queryWrapper.orderByAsc(Teachplan::getOrderby);
        return this.list(queryWrapper);
    }

    //新增课程计划,新增大章节，小章节，修改章节名
    @Override
    @Transactional
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {

        Long id = saveTeachplanDto.getId();

        if (id == null) {
            //新增章节
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);

            //计算排序字段
            List<Teachplan> teachplans = getTeachplanList(teachplan.getCourseId(), teachplan.getParentid());
            if (teachplans.size() == 0) {
                teachplan.setOrderby(1);
            } else {
                Teachplan teachplanLast = teachplans.get(teachplans.size() - 1);
                teachplan.setOrderby(teachplanLast.getOrderby() + 1);
            }

            this.save(teachplan);

        } else {
            //修改章节
            Long teachplanDtoId = saveTeachplanDto.getId();
            Teachplan teachplan = this.getById(teachplanDtoId);
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            this.updateById(teachplan);
        }
    }

    //删除章节
    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {

        //该章节存在至少一个子节点，不能删除
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid, teachplanId);
        if (this.getOne(queryWrapper) != null) {
            throw new XueChengPlusException("需要先删除该章节的小节");
        }

        //先删除该章节关联的视频信息
        LambdaQueryWrapper<TeachplanMedia> mediaLambdaQueryWrapper = new LambdaQueryWrapper<>();
        mediaLambdaQueryWrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
        teachplanMediaService.remove(mediaLambdaQueryWrapper);

        //删除该章节
        this.removeById(teachplanId);
    }

    //上移章节
    @Override
    @Transactional
    public void teacheplanMoveup(Long teachplanId) {
        Teachplan teachplan = this.getById(teachplanId);
        List<Teachplan> teachplanList = getTeachplanList(teachplan.getCourseId(), teachplan.getParentid());
        int index = teachplanList.indexOf(teachplan);
        if (index > 0) {
            //在该章节之前还有章节
            Teachplan teachplanUp = teachplanList.get(index - 1);

            //和上一章节交换orderby
            Integer orderBy = teachplan.getOrderby();
            teachplan.setOrderby(teachplanUp.getOrderby());
            teachplanUp.setOrderby(orderBy);

            this.updateById(teachplan);
            this.updateById(teachplanUp);
        } else {
            //该章节之前没有章节了
            throw new XueChengPlusException("该章节已无法上移");
        }
    }

    //下移章节
    @Transactional
    @Override
    public void teacheplanMovedown(Long teachplanId) {
        Teachplan teachplan = this.getById(teachplanId);
        List<Teachplan> teachplanList = getTeachplanList(teachplan.getCourseId(), teachplan.getParentid());
        int index = teachplanList.indexOf(teachplan);
        if (index < teachplanList.size() - 1) {
            //在该章节之后还有章节
            Teachplan teachplanUp = teachplanList.get(index + 1);

            //和下一章节交换orderby
            Integer orderBy = teachplan.getOrderby();
            teachplan.setOrderby(teachplanUp.getOrderby());
            teachplanUp.setOrderby(orderBy);

            this.updateById(teachplan);
            this.updateById(teachplanUp);
        } else {
            //该章节之后没有章节了
            throw new XueChengPlusException("该章节已无法下移");
        }
    }
}
