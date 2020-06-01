package com.zhp.websocket.service;

import com.alibaba.fastjson.JSON;
import com.zhp.websocket.common.callback.MqttRecieveCallback;
import com.zhp.websocket.common.dto.ReturnData;
import com.zhp.websocket.common.vo.EncoderClassVo;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @Description：WebSocketServer
 * @Author: zhp
 */

@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{sid}", subprotocols = {"mqtt"}, encoders = {EncoderClassVo.class })
public class WebSocketServer {

    // JUC包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    // 用ConcurrentHashMap也是可以的。说白了就是类似线程池中的BlockingQueue那样作为一个容器
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();
    private static ConcurrentHashMap<String, CopyOnWriteArraySet<String>> topics = new ConcurrentHashMap<>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 接收sid
    private String sid;

    private boolean partake = false;

    private int topicNum;

    private static MqttClient mqttClient = null;
    private static MemoryPersistence memoryPersistence = null;
    private static MqttConnectOptions mqttConnectOptions = null;

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 建立websocket连接
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("sid")String sid, Session session){
        this.session = session;
        if("mqtt".equals(sid)){
            this.sid = UUID.randomUUID().toString().replaceAll("-", "");

            init(sid);

            webSocketSet.add(this);

            sendMessage("Connection has created.");

            sendToIndex(new ReturnData("websocketNum", webSocketSet.size() - 1));
            sendAverage();
            sendActualConnect();
        }else{
            this.sid = sid;

            webSocketSet.add(this);

            initIndex();
        }
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
        if(!"zhp".equals(sid)){
            cleanTopics(sid);

            sendToIndex(new ReturnData("websocketNum", webSocketSet.size() - 2));
            sendAverage();
            sendActualConnect();
            sendtopics();
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
        if(!"zhp".equals(sid)){
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
        mqttConnectOptions.setKeepAliveInterval(24*60*60);

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
                partake = true;
                topicNum++;

                sendSubscribe();
                sendToIndex(new ReturnData("topicsNum", topics.size()));
                sendAverage();
                sendActualConnect();
                sendtopics();

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
                topicNum--;
                if(0 == topicNum){
                    partake = false;
                }
                sendSubscribe();
                sendToIndex(new ReturnData("topicsNum", topics.size()));
                sendAverage();
                sendActualConnect();
                sendtopics();

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
                        sendToIndex(new ReturnData("publishNum", topics.get(pubTopic).size()));
                        sendToIndex(new ReturnData("message", pubTopic + "-" + message));
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
                    webSocketServer.sendMessage("接收主题（" + topic + "）消息：" + message);
                    log.info("Mass messaging. the message({}) has sended to sid:{}.", message, webSocketServer.sid);
                }
            }
        }
    }

    /**
     * 指定发送数据至zhp客户端
     */
    public static void sendToIndex(ReturnData returnData){
        for(WebSocketServer webSocketServer : webSocketSet) {
            if ("zhp".equals(webSocketServer.sid))  {
                try {
                    webSocketServer.session.getBasicRemote().sendObject(returnData);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (EncodeException e) {
                    e.printStackTrace();
                }
                log.info("send message{} to zhp", returnData);
            }
        }
    }

    /**
     * 删除主题中连接
     */
    private void cleanTopics(String sid) {
        for(Map.Entry<String, CopyOnWriteArraySet<String>> entry : topics.entrySet()){
            CopyOnWriteArraySet<String> arraySet = entry.getValue();
            if(arraySet.contains(sid)){
                arraySet.remove(sid);
            }
        }
    }

    /**
     * 发布平均订阅量
     */
    private void sendAverage() {
        int websocketSize = webSocketSet.size() - 1;
        if(websocketSize == 1 && this.sid.equals("zhp")){
            sendToIndex(new ReturnData("averageSubscribeNum", 0));
        }else{
            if(websocketSize == 0){
                sendToIndex(new ReturnData("averageSubscribeNum", 0));
            }else{
                sendToIndex(new ReturnData("averageSubscribeNum", topics.size()/websocketSize));
            }
        }
    }

    /**
     * 发布有效连接
     */
    private void sendActualConnect() {
        int websocketSize = webSocketSet.size() - 1;
        double ActualNum = 0.00;
        for(WebSocketServer webSocketServer : webSocketSet){
            if(webSocketServer.partake){
               ActualNum++;
            }
        }
        if(ActualNum == 0){
            sendToIndex(new ReturnData("actualConnectNum", 0));
        }else{
            sendToIndex(new ReturnData("actualConnectNum", String.format("%.2f", ActualNum/websocketSize)));
        }
    }

    /**
     * 发布主题图谱
     */
    private void sendtopics() {
        LinkedList<HashMap<String, String>> topicsList = new LinkedList<>();
        for(Map.Entry<String, CopyOnWriteArraySet<String>> entry : topics.entrySet()){
            HashMap<String, String> topicsmap = new HashMap<>();
            topicsmap.put("name", entry.getKey());
            topicsmap.put("value", String.valueOf(entry.getValue().size()));
            topicsList.add(topicsmap);
        }
        sendToIndex(new ReturnData("topics", JSON.toJSONString(topicsList)));
    }

    /**
     * 发布客户端订阅主题量
     */
    private void sendSubscribe() {
        LinkedList<Integer> list = new LinkedList<>();
        for(WebSocketServer webSocketServer : webSocketSet){
            if(!webSocketServer.sid.equals("zhp")){
                list.add(webSocketServer.topicNum);
            }
        }
        sendToIndex(new ReturnData("subscribeData", JSON.toJSONString(list)));
    }

    /**
     * 初始化页面数据
     */
    private void initIndex() {
        sendToIndex(new ReturnData("websocketNum", webSocketSet.size() - 1));
        sendToIndex(new ReturnData("topicsNum", topics.size()));
        sendAverage();
        sendActualConnect();
        sendtopics();
        sendSubscribe();
    }
}