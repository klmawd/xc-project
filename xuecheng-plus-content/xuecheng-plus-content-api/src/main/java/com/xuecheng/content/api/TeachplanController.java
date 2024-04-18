package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 课程计划 前端控制器
 * </p>
 *
 * @author itcast
 */
@Slf4j
@RestController
public class TeachplanController {

    @Autowired
    private TeachplanService teachplanService;

    //查询课程计划
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> queryCourseplan(@PathVariable("courseId") Long courseId) {

        return teachplanService.qureyCourseplanByCourseId(courseId);
    }

    //新增课程计划
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody @Validated SaveTeachplanDto saveTeachplanDto) {
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    //删除章节
    @DeleteMapping("/teachplan/{teachplanId}")
    public void deleteTeachplan(@PathVariable("teachplanId") Long teachplanId) {
        teachplanService.deleteTeachplan(teachplanId);
    }

    //上移章节
    @PostMapping("/teachplan/moveup/{teachplanId}")
    public void teachplanMoveup(@PathVariable("teachplanId") Long teachplanId){
        teachplanService.teacheplanMoveup(teachplanId);
    }

    //下移章节
    @PostMapping("/teachplan/movedown/{teachplanId}")
    public void teachplanMovedown(@PathVariable("teachplanId") Long teachplanId){
        teachplanService.teacheplanMovedown(teachplanId);
    }
}
