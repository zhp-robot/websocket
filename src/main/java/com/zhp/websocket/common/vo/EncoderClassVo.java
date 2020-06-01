package com.zhp.websocket.common.vo;

import com.alibaba.fastjson.JSON;
import com.zhp.websocket.common.dto.ReturnData;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class EncoderClassVo implements Encoder.Text<ReturnData>{

    @Override
    public void init(EndpointConfig config) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }
    @Override
    public String encode(ReturnData returnData) throws EncodeException {
        return JSON.toJSONString(returnData);
    }
}