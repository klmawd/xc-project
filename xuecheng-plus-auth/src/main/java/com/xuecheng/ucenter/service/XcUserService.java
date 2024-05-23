package com.xuecheng.ucenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.ucenter.model.dto.FindPasswordDto;
import com.xuecheng.ucenter.model.dto.RegisterDto;
import com.xuecheng.ucenter.model.po.XcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

public interface XcUserService extends IService<XcUser> {
    void register(RegisterDto registerDto);

    void findPassword(FindPasswordDto findPassword);
}
