package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistoryDO {

    private int id;

    private int postingId;

    private String content;

    private LocalDateTime cTime;
}
