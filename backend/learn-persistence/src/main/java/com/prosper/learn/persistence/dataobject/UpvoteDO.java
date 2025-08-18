package com.prosper.learn.persistence.dataobject;

import lombok.Data;

@Data
public class UpvoteDO {

    private int id;

    private int objectId;

    private int objectType;

    private int userId;

    private int type;
}
