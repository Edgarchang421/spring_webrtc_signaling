package com.example.webrtc;

// 處理透過webScoeket交換的資料
public class WebScoketMessage {
    // New、Candidate、Offer、AnsWer
    private String Type;
    private String Data;

    public WebScoketMessage(){}

    public String getType(){
        return Type;
    }

    public void setType(String type){
        Type = type;
    }

    public String getData(){
        return Data;
    }

    public void setData(String data){
        Data = data;
    }
}
