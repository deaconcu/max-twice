package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import lombok.Builder;
import lombok.Getter;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;

@Getter
public class Article implements Aggregate<Integer> {

    private Integer id;

    private int nodeId;

    private int userId;

    private int vote;

    private List<ArticleCommit> commits;

    private Date createTime;

    public Article() {}

    public Article(Integer id, int nodeId, int userId) {
        this.id = id;
        this.nodeId = nodeId;
        this.userId = userId;
        commits = new LinkedList<>();
    }

    public Article(Integer id, int nodeId, int userId, String content) {
        this(id, nodeId, userId);
        addCommit(content);
    }

    public void addCommit(String content) {
        commits.add(new ArticleCommit(content));
    }
}
