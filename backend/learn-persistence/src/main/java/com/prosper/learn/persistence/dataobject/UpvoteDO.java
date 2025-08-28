package com.prosper.learn.persistence.dataobject;

import lombok.Data;

@Data
public class UpvoteDO {

    private Long id;

    private Long objectId;

    private Integer objectType;

    private Long userId;

    private Integer type;
}
