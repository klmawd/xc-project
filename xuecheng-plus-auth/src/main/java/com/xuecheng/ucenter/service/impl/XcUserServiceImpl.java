package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.FindPasswordDto;
import com.xuecheng.ucenter.model.dto.RegisterDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.XcUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class XcUserServiceImpl extends ServiceImpl<XcUserMapper, XcUser> implements XcUserService {

    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    PasswordEncoder passwordEncoder;


    //注册
    @Override
    @Transactional
    public void register(RegisterDto registerDto) {

        //检验验证码是否正确
        String cellphone = registerDto.getCellphone();
        String code = registerDto.getCheckcode();
        ValueOperations opsForValue = redisTemplate.opsForValue();
        String checkCode = (String) opsForValue.get(cellphone);
        if (!code.equals(checkCode)) {
            throw new RuntimeException("验证码错误");
        }
        redisTemplate.delete(cellphone);

        //检验密码
        String passWord = registerDto.getPassword();
        if (!passWord.equals(registerDto.getConfirmpwd())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        //检验用户是否存在
        String userName = registerDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>()
                .eq(XcUser::getUsername, userName));
        if (user != null) {
            throw new RuntimeException("该用户已存在");
        }

        //向xc_user写
        user = new XcUser();
        BeanUtils.copyProperties(registerDto, user);
        user.setName(registerDto.getNickname());
        user.setPassword(passwordEncoder.encode(passWord));
        user.setStatus("1");
        user.setUtype("101001");
        user.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(user);

        //向xc_user_role写
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setUserId(user.getId());
        xcUserRole.setRoleId("17");
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);


    }

    //找回密码
    @Override
    public void findPassword(FindPasswordDto findPassword) {

        //检验验证码是否正确
        String cellphone = findPassword.getCellphone();
        String code = findPassword.getCheckcode();
        ValueOperations opsForValue = redisTemplate.opsForValue();
        String checkCode = (String) opsForValue.get(cellphone);
        if (!code.equals(checkCode)) {
            throw new RuntimeException("验证码错误");
        }
        redisTemplate.delete(cellphone);

        //检验密码
        String passWord = findPassword.getPassword();
        if (!passWord.equals(findPassword.getConfirmpwd())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        //检验用户是否存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>()
                .eq(XcUser::getCellphone, cellphone));
        if (xcUser == null) {
            throw new RuntimeException("用户不存在");
        }

        //修改密码
        xcUser.setPassword(passwordEncoder.encode(passWord));
        xcUserMapper.updateById(xcUser);
    }
}
