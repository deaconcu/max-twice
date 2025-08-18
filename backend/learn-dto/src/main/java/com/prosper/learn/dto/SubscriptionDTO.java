package com.prosper.learn.dto;

import lombok.Data;

@Data
public class SubscriptionDTO {

    private int id;

    private String name;

    public SubscriptionDTO(int id, String name) {
        this.id = id;
        this.name = name;
    }

}
