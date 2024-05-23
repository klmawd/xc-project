package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


    @Autowired
    XcOrdersMapper xcOrdersMapper;
    @Autowired
    XcOrdersGoodsMapper xcOrdersGoodsMapper;
    @Autowired
    XcPayRecordMapper xcPayRecordMapper;
    @Autowired
    OrderService currentProxy;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    //购买课程
    @Override
    @Transactional
    public PayRecordDto buyCourse(String userId, AddOrderDto addOrderDto) {

        //创建订单
        XcOrders xcOrders = CreatOrder(userId, addOrderDto);

        //添加订单明细
        addOrderGoods(xcOrders.getId(), addOrderDto);

        //生成支付记录
        XcPayRecord xcPayRecord = CreatPayRecord(userId, addOrderDto, xcOrders);

        Long payNo = xcPayRecord.getPayNo();
        //生成支付二维码
        String qrcode = generatePayCode("http://rrc2nx.natappfree.cc/orders/requestpay?payNo=" + payNo);

        //返回值
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(xcPayRecord, payRecordDto);
        payRecordDto.setQrcode(qrcode);

        return payRecordDto;

    }

    //查询支付记录
    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>()
                .eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    //body:
    //{"alipay_trade_query_response":{
    // "code":"10000",
    // "msg":"Success",
    // "buyer_logon_id":"rgp***@sandbox.com",
    // "buyer_pay_amount":"0.00",
    // "buyer_user_id":"2088722035954144",
    // "buyer_user_type":"PRIVATE",
    // "invoice_amount":"0.00",
    // "out_trade_no":"1793527007357083648",
    // "point_amount":"0.00",
    // "receipt_amount":"0.00",
    // "send_pay_date":"2024-05-23 14:23:35",
    // "total_amount":"19.90",
    // "trade_no":"2024052322001454140503014445",
    // "trade_status":"TRADE_SUCCESS"},
    // "sign":"MUUczrBIhjtMDLfymSphIdku5BtR5K7vhO38W1o2bi+Bq+G7CtxA+8uw5E+1S7KGC8ZQfOiWT7hv5f+LohngGCis1+LMSg+Fji/K0LExFbwMekxvBkzDYTc7ZftdJ9AbPqWOra4Eez/NAVFpoavn8GDfIuUsKdU35CKG+AV7cxuW/5pq7SlSSKnreP9bDuP46aItgeIn12IaP6fC1US08NsvNhl7LMOSfFhOIDrI7g9vWlfvgdz83pKmyatyBD4NdspglwkC5XE2YJrsqMGGBPBNyKYhuJ1ZXO2tnGMK0rKA0fJUk9spv2BIC8E2841QtU/R7cO3jI70GLUs1YjZcw=="}
    //查询支付结果
    @Override
    public PayRecordDto qureyPayResult(String payNo) {

        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        //bizContent.put("trade_no", "2014112611001004680073956707");
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;

        PayRecordDto payRecordDto = new PayRecordDto();
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (response.isSuccess()) {

            String resultJson = response.getBody();
            //转map
            Map resultMap = JSON.parseObject(resultJson, Map.class);
            Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
            //支付结果
            String trade_status = (String) alipay_trade_query_response.get("trade_status");

            if (trade_status.equals("TRADE_SUCCESS")) {
                //支付成功
                String tradeNo = (String) alipay_trade_query_response.get("trade_no");
                String totalAmount = (String) alipay_trade_query_response.get("total_amount");

                PayStatusDto payStatusDto = new PayStatusDto();
                payStatusDto.setTrade_status("601002");//支付状态为已支付
                payStatusDto.setOut_trade_no(payNo);//支付记录号
                payStatusDto.setTrade_no(tradeNo);//第三方支付id
                payStatusDto.setApp_id(APP_ID);
                payStatusDto.setTotal_amount(totalAmount);//总金额
                //更新支付状态
                currentProxy.updatePayStatus(payStatusDto);

            } else {
                throw new XueChengPlusException("未支付");
            }

        } else {
            throw new XueChengPlusException("调用失败");
        }
        return payRecordDto;
    }

    //更新支付状态
    @Transactional
    @Override
    public PayRecordDto updatePayStatus(PayStatusDto payStatusDto) {

        Long payNo = Long.valueOf(payStatusDto.getOut_trade_no());
        String tradeNo = payStatusDto.getTrade_no();
        String status = payStatusDto.getTrade_status();

        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>()
                .eq(XcPayRecord::getPayNo, payNo));

        //更新支付记录表
        xcPayRecord.setOutPayNo(tradeNo);//第三方支付id
        xcPayRecord.setStatus(status);//更新状态为已支付
        xcPayRecord.setPaySuccessTime(LocalDateTime.now());
        xcPayRecordMapper.updateById(xcPayRecord);

        //更新订单表
        Long orderId = xcPayRecord.getOrderId();
        xcOrdersMapper.update(null, new LambdaUpdateWrapper<XcOrders>()
                .eq(XcOrders::getId, orderId).set(XcOrders::getStatus, "600002"));

        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(xcPayRecord, payRecordDto);
        return payRecordDto;
    }


    //创建订单
    @Override
    public XcOrders CreatOrder(String userId, AddOrderDto addOrderDto) {

        //选课记录id
        String outBusinessId = addOrderDto.getOutBusinessId();
        XcOrders xcOrders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>()
                .eq(XcOrders::getOutBusinessId, outBusinessId));

        //存在该订单直接返回
        if (xcOrders != null) {
            return xcOrders;
        }

        //创建订单
        xcOrders = new XcOrders();
        BeanUtils.copyProperties(addOrderDto, xcOrders);
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");
        xcOrders.setUserId(userId);
        xcOrdersMapper.insert(xcOrders);

        return xcOrders;
    }

    //添加订单明细
    private List<XcOrdersGoods> addOrderGoods(Long orderId, AddOrderDto addOrderDto) {

        //存在该订单明细直接返回
        List<XcOrdersGoods> xcOrdersGoods = xcOrdersGoodsMapper.selectList(new LambdaQueryWrapper<XcOrdersGoods>()
                .eq(XcOrdersGoods::getOrderId, orderId));
        if (xcOrdersGoods.size() > 0) {
            return xcOrdersGoods;
        }

        //添加订单明细
        String orderDetail = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> ordersGoods = JSON.parseArray(orderDetail, XcOrdersGoods.class);
        ordersGoods.stream().forEach(ordersGood -> {
            ordersGood.setOrderId(orderId);
            xcOrdersGoodsMapper.insert(ordersGood);
        });
        return ordersGoods;
    }


    //生成支付记录
    @Override
    public XcPayRecord CreatPayRecord(String userId, AddOrderDto addOrderDto, XcOrders xcOrders) {

        Long ordersId = xcOrders.getId();
        //查询是否存在该订单支付记录
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>()
                .eq(XcPayRecord::getOrderId, ordersId));
        if (xcPayRecord != null) {
            return xcPayRecord;
        }

        if ("601002".equals(xcOrders.getStatus())) {
            //已支付
            throw new RuntimeException("已交付，无需重新支付");
        }

        xcPayRecord = new XcPayRecord();
        xcPayRecord.setOrderId(ordersId);//订单号
        xcPayRecord.setOrderName(xcOrders.getOrderName());//订单名称
        xcPayRecord.setUserId(userId);//用户id
        xcPayRecord.setTotalPrice(addOrderDto.getTotalPrice());//总价
        xcPayRecord.setCurrency("CHY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");  //未支付
        xcPayRecord.setOutPayChannel("603002");//第三方支付渠道，支付宝
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());//支付交易号,雪花算法生成

        xcPayRecordMapper.insert(xcPayRecord);
        return xcPayRecord;
    }


    //生成支付二维码
    @Override
    public String generatePayCode(String content) {
        String result = new String();
        try {
            result = QRCodeUtil.createQRCode(content, 200, 200);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
