package com.prosper.learn.dto;

import lombok.Data;

@Data
public class SubscriptionDTO {

    private Long id;

    private String name;

    public SubscriptionDTO() {
        // 默认构造函数，供Jackson反序列化使用
    }

    public SubscriptionDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

}
