package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;


/**
 * @author Mr.M
 * @version 1.0
 * @description 微信扫码认证
 * @date 2022/9/29 12:12
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    XcUserRoleMapper xcUserRoleMapper;
    @Autowired
    WxAuthServiceImpl currentProxy;


    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        //账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }

    //请求微信申请令牌，拿到令牌查询用户信息，将用户信息写入数据库
    @Override
    public XcUser wxAuth(String code) {

        //请求微信申请令牌
        Map<String, String> access_token_map = getAccessToken(code);
        String access_token = access_token_map.get("access_token");
        String openId = access_token_map.get("openid");

        //拿到令牌查询用户信息
        Map<String, String> userInfoMap = getUserinfo(access_token, openId);

        //将用户信息写入数据库
        XcUser xcUser = currentProxy.addWxUser(userInfoMap);

        return xcUser;
    }


    /**
     * 请求微信申请令牌
     * <p>
     * http请求方式: GET
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     * 返回结果：
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "UNIONID"
     * }
     *
     * @param code
     * @return
     */
    private Map<String, String> getAccessToken(String code) {

        //拼接url地址
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String url = String.format(url_template, appid, secret, code);

        //发送http请求
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

        //解析结果
        String result = exchange.getBody();
        Map<String, String> map = JSON.parseObject(result, Map.class);

        return map;
    }

    /**
     * 查询用户信息
     * <p>
     * http请求方式: GET
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * 返回结果：
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * <p>
     * }
     *
     * @param access_token
     * @param openid
     * @return
     */
    private Map<String, String> getUserinfo(String access_token, String openid) {

        //拼接url地址
        String url_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(url_template, access_token, openid);

        //发送http请求
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

        //解析结果
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        Map<String, String> map = JSON.parseObject(result, Map.class);

        return map;
    }

    //将用户信息写入数据库
    @Transactional
    public XcUser addWxUser(Map<String, String> userInfoMap) {

        String unionid = userInfoMap.get("unionid");
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>()
                .eq(XcUser::getWxUnionid, unionid));

        if (user != null) {
            return user;
        }
        //插入xc_user
        XcUser xcUser = new XcUser();
        xcUser.setWxUnionid(unionid);
        xcUser.setNickname(userInfoMap.get("nickname").toString());
        xcUser.setUserpic(userInfoMap.get("headimgurl").toString());
        xcUser.setName(userInfoMap.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);

        //插入xc_user_role
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setUserId(xcUser.getId());
        xcUserRole.setRoleId("17");//学生角色
        xcUserRole.setCreateTime(LocalDateTime.now());
        xcUserRoleMapper.insert(xcUserRole);

        return xcUser;
    }

}
