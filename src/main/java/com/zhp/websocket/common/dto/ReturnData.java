package com.zhp.websocket.common.dto;

public class ReturnData {
    private String id;
    private Object data;

    public ReturnData(String id, Object data) {
        this.id = id;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ReturnData{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}