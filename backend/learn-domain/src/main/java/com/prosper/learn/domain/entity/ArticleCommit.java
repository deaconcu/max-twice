package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import lombok.Data;

import java.util.Date;

@Data
public class ArticleCommit implements Aggregate<Integer> {

    private Integer id;

    private String content;

    private Date createTime;

    public ArticleCommit(String content) {
        this.content = content;
    }
}
