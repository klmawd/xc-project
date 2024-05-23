package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.dto.FindPasswordDto;
import com.xuecheng.ucenter.model.dto.RegisterDto;
import com.xuecheng.ucenter.service.XcUserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping
public class XcUserController {

    @Autowired
    XcUserService xcUserService;

    @ApiOperation("注册接口")
    @PostMapping("/register")
    public void register(@RequestBody @Validated RegisterDto registerDto){

        xcUserService.register(registerDto);
    }

    @ApiOperation("找回密码接口")
    @PostMapping("/findpassword")
    public void findPassword(@RequestBody @Validated FindPasswordDto findPassword){
        xcUserService.findPassword(findPassword);
    }
}
