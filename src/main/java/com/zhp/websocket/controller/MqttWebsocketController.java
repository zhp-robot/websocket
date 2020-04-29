package com.zhp.websocket.controller;

import com.zhp.websocket.common.dto.ResponseReturn;
import com.zhp.websocket.service.WebSocketServer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Description： 仿真mqtt
 * @Author: zhp
 */
@Controller
@RequestMapping("/websocket/mqtt")
public class MqttWebsocketController {

   /* @Autowired
    private WebSocketServer webSocketServer;
    *//**
     * 调用WebsocketServer的消息推送方法，从而进行消息推送
     * @param sid 连接WebsocketServer的前端的唯一标识。如果sid为空，即表示向所有连接WebsocketServer的前端发送相关消息
     * @param message 需要发送的内容主体
     * @return
     *//*
    @ResponseBody
    @RequestMapping("/push")
    public ResponseReturn pushToWeb(@RequestParam(name = "sid", defaultValue = "") String sid, @RequestParam(name = "message") String message) {
        WebSocketServer.sendInfo(sid, message);
        if(StringUtils.isBlank(sid)){
            sid = "all";
        }
        return ResponseReturn.success(message + " have sent to " + sid);
    }

    *//**
     * 订阅主题
     * @param topic
     * @return ResponseReturn
     *//*
    @RequestMapping(value = "/subscribe", method = RequestMethod.POST)
    @ResponseBody
    public ResponseReturn subscribe(@RequestParam(value = "topic") String topic){
        try {
            return webSocketServer.subTopic(topic);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseReturn.unknownError(e.getLocalizedMessage());
        }
    }

    *//**
     * 取消订阅
     * @param topic
     * @return ResponseReturn
     *//*
    @RequestMapping(value = "/unsubscribe", method = RequestMethod.POST)
    @ResponseBody
    public ResponseReturn unsubscribe(@RequestParam(value = "topic") String topic){
        try {
            return webSocketServer.cleanTopic(topic);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseReturn.unknownError("取消订阅失败！");
        }
    }

    *//**
     * 发布消息
     * @param topic
     * @param message
     * @return ResponseReturn
     *//*
    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    @ResponseBody
    public ResponseReturn publish(@RequestParam(value = "topic") String topic,
                                  @RequestParam(value = "message") String message){
        try {
            return webSocketServer.publishMessage(topic, message, 2);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseReturn.unknownError("发布消息失败！");
        }
    }*/
}