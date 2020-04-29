package com.zhp.websocket.common.dto;


import java.util.HashMap;
import java.util.Map;

public class ResponseReturn {

    /**
     * 返回结果
     */
    private boolean success;

    /**
     * 返回信息
     */
    private int code;

    /**
     * 返回数据
     */
    private Object data;

    public ResponseReturn(boolean success) {
        super();
        this.success = success;
    }

    public ResponseReturn(boolean success, int code) {
        super();
        this.success = success;
        this.code = code;
    }

    public ResponseReturn(boolean success, Object data) {
        super();
        this.success = success;
        this.data = data;
    }

    public ResponseReturn(boolean success, int code, Object data) {
        super();
        this.success = success;
        this.code = code;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static ResponseReturn success(Object object) {
        return new ResponseReturn(true, ResponseCode.SUCCESS.getCode(), object);
    }

    public static ResponseReturn success() {
        return new ResponseReturn(true, ResponseCode.SUCCESS.getCode());
    }

    public static ResponseReturn unknownError() {
        return new ResponseReturn(false, ResponseCode.UNKNOWN_ERROR.getCode(), ResponseCode.UNKNOWN_ERROR.getDes());
    }

    public static ResponseReturn unknownError(Object object) {
        return new ResponseReturn(false, ResponseCode.UNKNOWN_ERROR.getCode(), object);
    }

    public static ResponseReturn paramError() {
        return new ResponseReturn(false, ResponseCode.PARAM_ILLEGAL.getCode(), ResponseCode.PARAM_ILLEGAL.getDes());
    }

    public static ResponseReturn paramError(Object o) {
        return new ResponseReturn(false, ResponseCode.PARAM_ILLEGAL.getCode(), o);
    }

    public static ResponseReturn paramNull() {
        return new ResponseReturn(false, ResponseCode.PARAM_NULL.getCode(), ResponseCode.PARAM_NULL.getDes());
    }
    public static ResponseReturn paramNull(Object paramName) {
        Map map = new HashMap<>();
        map.put("message", paramName);
        return new ResponseReturn(false, ResponseCode.PARAM_NULL.getCode(), map);
    }

    enum ResponseCode {
        SUCCESS(200, "成功"),
        PARAM_NULL(3001, "参数为空"),
        PARAM_ILLEGAL(3002, "参数非法"),
        UNKNOWN_ERROR(-1, "未知错误");

        private int code;
        private String des;

        ResponseCode(int code, String des) {
            this.code = code;
            this.des = des;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getDes() {
            return des;
        }

        public void setDes(String des) {
            this.des = des;
        }
    }
}
