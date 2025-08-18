package com.prosper.learn.dto;

import lombok.Data;

@Data
public class HistoryDTO {

    private int id;

    private int postingId;

    private String content;

    private String createTime;
}
