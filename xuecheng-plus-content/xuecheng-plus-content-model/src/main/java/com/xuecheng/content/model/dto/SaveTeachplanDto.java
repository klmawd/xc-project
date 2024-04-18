
package com.xuecheng.content.model.dto;


import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;


/**
 * @author Mr.M
 * @version 1.0
 * @description 保存课程计划dto，包括新增、修改
 * @date 2022/9/9 10:27
 */
@Data
public class SaveTeachplanDto {

    /***
     * 教学计划id
     */
    private Long id;

    /**
     * 课程计划名称
     */
    @NotEmpty
    private String pname;

    /**
     * 课程计划父级Id
     */
    @NotNull
    private Long parentid;

    /**
     * 层级，分为1、2、3级
     */
    @NotNull
    private Integer grade;

    /**
     * 课程类型:1视频、2文档
     */
    private String mediaType;

    /**
     * 课程标识
     */
    @NotNull
    private Long courseId;

    /**
     * 课程发布标识
     */
    private Long coursePubId;


    /**
     * 是否支持试学或预览（试看）
     */
    private String isPreview;


}
