package com.xuecheng.ucenter.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class FindPasswordDto {

    //手机号
    @NotEmpty
    String cellphone;

    //验证码
    @NotEmpty
    String checkcode;

    //验证码key
    String checkcodekey;

    //新密码
    @NotEmpty
    String password;

    //二次确认新密码
    @NotEmpty
    String confirmpwd;

    //邮箱
    String email;
}
