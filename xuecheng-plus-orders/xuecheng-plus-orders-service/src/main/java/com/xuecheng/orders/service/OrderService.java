package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcPayRecord;


public interface OrderService {

    String generatePayCode(String content);

    XcOrders CreatOrder(String userId, AddOrderDto addOrderDto);

    XcPayRecord CreatPayRecord(String userId, AddOrderDto addOrderDto, XcOrders xcOrders);

    PayRecordDto buyCourse(String userId, AddOrderDto addOrderDto);

    XcPayRecord getPayRecordByPayno(String payNo);

    PayRecordDto qureyPayResult(String payNo);

    PayRecordDto updatePayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     *
     * @param message
     */
    void notifyPayResult(MqMessage message);


}
