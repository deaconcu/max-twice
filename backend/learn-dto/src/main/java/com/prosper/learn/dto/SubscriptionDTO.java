package com.prosper.learn.dto;

import lombok.Data;

@Data
public class SubscriptionDTO {

    private int id;

    private String name;

    public SubscriptionDTO() {
        // 默认构造函数，供Jackson反序列化使用
    }

    public SubscriptionDTO(int id, String name) {
        this.id = id;
        this.name = name;
    }

}
