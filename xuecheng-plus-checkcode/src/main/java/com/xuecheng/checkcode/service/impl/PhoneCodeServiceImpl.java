package com.xuecheng.checkcode.service.impl;

import com.xuecheng.checkcode.service.PhoneCodeService;
import com.xuecheng.checkcode.util.SMSUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PhoneCodeServiceImpl implements PhoneCodeService {
    @Autowired
    RedisCheckCodeStore redisCheckCodeStore;
    @Autowired
    NumberLetterCheckCodeGenerator numberLetterCheckCodeGenerator;


    //发送短信验证码
    @Override
    public void sendMsgService(String phoneNumber) {

        String code = numberLetterCheckCodeGenerator.generate(4);
        log.info("code:{}", code);

       // SMSUtils.sendMessage(SMSUtils.SIGN_NAME, SMSUtils.TEMPLATE_CODE, phoneNumber, code);

        //将手机号和验证码存redis
        redisCheckCodeStore.set(phoneNumber, code, 1 * 60);

    }
}
