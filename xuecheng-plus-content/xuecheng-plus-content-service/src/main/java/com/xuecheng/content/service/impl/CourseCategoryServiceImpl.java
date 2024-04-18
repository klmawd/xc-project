package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 课程分类 服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> listCourseCategory(String id) {
        //查询分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        List<CourseCategoryTreeDto> categoryResults = new ArrayList<>();

        //将List转成Map，便于根据id获取节点
        Map<String, CourseCategoryTreeDto> categoryTreeDtoMap = courseCategoryTreeDtos.stream().collect(Collectors.toMap(Key -> Key.getId(), Value -> Value));

        //处理子节点
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {

            //判断是否是根节点的孩子
            if (item.getParentid().equals(id)) {
                categoryResults.add(item);
            }

            //得到该节点的父节点
            CourseCategoryTreeDto pTreeNode = categoryTreeDtoMap.get(item.getParentid());
            if (pTreeNode != null) {
                //判断父节点的是否存在孩子节点，没有需要初始化List
                if (pTreeNode.getChildrenTreeNodes() == null) {
                    pTreeNode.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //将该节点添加的父节点的孩子节点中
                pTreeNode.getChildrenTreeNodes().add(item);
            }

        });
        return categoryResults;
    }
}
