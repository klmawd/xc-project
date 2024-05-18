package com.xuecheng.ucenter.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class RegisterDto {
    //电话号码
    @NotEmpty
    String cellphone;

    //用户名
    @NotEmpty
    String username;

    //用户邮箱
    String email;

    //用户昵称
    @NotEmpty
    String nickname;

    //用户密码
    @NotEmpty
    String password;

    //确认密码
    @NotEmpty
    String confirmpwd;

    //验证码key
    String checkcodeckey;

    //验证码
    @NotEmpty
    String checkcode;

}
