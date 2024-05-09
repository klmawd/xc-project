package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.po.TeachplanMedia;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author itcast
 * @since 2024-04-11
 */
public interface TeachplanMediaService extends IService<TeachplanMedia> {
    void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
