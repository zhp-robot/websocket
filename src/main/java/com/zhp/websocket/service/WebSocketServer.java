package com.zhp.websocket.service;

import com.zhp.websocket.common.callback.MqttRecieveCallback;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @Description：WebSocketServer
 * @Author: zhp
 */

@Component
@Slf4j
@ServerEndpoint(value = "/websocket", subprotocols = {"mqtt"})
public class WebSocketServer {

    // JUC包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    // 用ConcurrentHashMap也是可以的。说白了就是类似线程池中的BlockingQueue那样作为一个容器
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();
    private static ConcurrentHashMap<String, CopyOnWriteArraySet<String>> topics = new ConcurrentHashMap<>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 接收sid
    private String sid = "";

    private static MqttClient mqttClient = null;
    private static MemoryPersistence memoryPersistence = null;
    private static MqttConnectOptions mqttConnectOptions = null;

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 建立websocket连接
     * @param session
     */
    @OnOpen
    public void onOpen(Session session){
        this.session = session;
        this.sid = UUID.randomUUID().toString().replaceAll("-", "");

        init(sid);

        webSocketSet.add(this);

        sendMessage("Connection has created.");
    }

    /**
     * 关闭websocket连接
     */
    @OnClose
    public void onClose(){
        //关闭存储方式
        if(null != memoryPersistence) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        //关闭连接
        if(null != mqttClient) {
            if(mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        webSocketSet.remove(this);

        log.info("there is an wsConnect has close.");
    }

    /**
     * websocket连接出现问题时的处理
     */
    @OnError
    public void onError(Session session, Throwable error){
        log.error("there is an error has happen! error:{}", error);
    }

    /**
     * websocket的server端用于接收消息的（目测是用于接收前端通过Socket.onMessage发送的消息）
     * @param message
     */
    @OnMessage
    public void onMessage(String message){
        log.info("webSocketServer has received a message:{} from {}", message, this.sid);
        String result = "尊敬的用户你好！";

        String[] split = message.split("[|]");
        if("subscribe".equals(split[0])){
            result = subTopic(split[1]);
        }else if("unsubscribe".equals(split[0])){
            result = cleanTopic(split[1]);
        }else if("publish".equals(split[0])){
            result = publishMessage(split[1], split[2], 2);
        }
        this.sendMessage(result);
    }

    /**
     * 初始化mqttClient连接
     */
    public void init(String sid){
        //初始化连接设置对象
        mqttConnectOptions = new MqttConnectOptions();
        //true可以安全地使用内存持久性作为客户端断开连接时清除的所有状态
        mqttConnectOptions.setCleanSession(true);
        //设置连接超时
        mqttConnectOptions.setConnectionTimeout(30);
        mqttConnectOptions.setKeepAliveInterval(45);

        //设置持久化方式
        memoryPersistence = new MemoryPersistence();
        try {
            mqttClient = new MqttClient("tcp://127.0.0.1:1883", sid, memoryPersistence);
        } catch (MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //设置连接和回调
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                //创建回调函数
                MqttRecieveCallback mqttReceriveCallback = new MqttRecieveCallback();
                //客户端添加回调函数
                mqttClient.setCallback(mqttReceriveCallback);
                //创建连接
                try {
                    mqttClient.connect(mqttConnectOptions);
                    sendMessage("mqtt has connected:" + mqttClient.isConnected());
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * websocket订阅消息
     */
    public String subTopic(String topic) {
        if(null != mqttClient && mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(topic, 2);
                if(null != topics.get(topic)){
                    topics.get(topic).add(sid);
                }else{
                    CopyOnWriteArraySet<String> sids = new CopyOnWriteArraySet<>();
                    sids.add(sid);
                    topics.put(topic, sids);
                }
                return "订阅成功:" + topic;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        return "订阅失败！";
    }

    /**
     * websocket取消订阅
     */
    public String cleanTopic(String topic) {
        if(null != mqttClient && mqttClient.isConnected()) {
            try {
                if(!topics.get(topic).contains(sid)){
                    return "您未订阅该主题：" + topic;
                }else{
                    mqttClient.unsubscribe(topic);
                    topics.get(topic).remove(sid);
                }
                return "取消订阅成功:" + topic;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        return "取消订阅失败！";
    }

    /**
     * websocket发布消息
     */
    public String publishMessage(String pubTopic, String message, int qos) {
        log.info("发布消息:" + pubTopic);
        log.info("id:" + mqttClient.getClientId());
        if(null != mqttClient && mqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());

            MqttTopic topic = mqttClient.getTopic(pubTopic);

            if(null != topic) {
                MqttDeliveryToken publish = null;
                try {
                    publish = topic.publish(mqttMessage);
                    publish.waitForCompletion();
                    if(publish.isComplete()) {
                        return "消息发布成功!";
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }else {
            reConnect();
        }
        return "消息发布失败！";
    }

    /**
     * mqttClient重新连接
     */
    public void reConnect(){
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                if (null != mqttConnectOptions) {
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }else {
            init(sid);
        }
    }

    /**
     * 服务器主动推送消息的方法
     */
    public void sendMessage(String message){
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.warn("there is an IOException:{}!", e.toString());
        }
    }

    /**
     * 发送订阅消息的方法
     */
    public static void sendInfo(String topic, String message){
        for (String sid : topics.get(topic)) {
            for (WebSocketServer webSocketServer : webSocketSet){
                if(sid.equals(webSocketServer.sid)){
                    webSocketServer.sendMessage("接收主题" + topic + "消息：" + message);
                    log.info("Mass messaging. the message({}) has sended to sid:{}.", message, webSocketServer.sid);
                }
            }
        }
    }
}