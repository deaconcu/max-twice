package com.prosper.learn.domain.repository;

import com.prosper.learn.common.Repository;
import com.prosper.learn.domain.entity.Article;

public interface ArticleRepository extends Repository<Article, Integer> {
    
    /**
     * 统计活跃文章数量
     * 
     * @return 文章总数
     */
    Long countActiveArticles();
}
