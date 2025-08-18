-- 为评论表添加评分字段
-- 用于存储基于点赞数、回复数和时间权重的综合评分

ALTER TABLE comment ADD COLUMN score DOUBLE DEFAULT 0.0 COMMENT '评论综合评分，基于点赞数、回复数和时间权重计算';

-- 为 score 字段添加索引以优化按评分排序的查询
CREATE INDEX idx_comment_score ON comment(score DESC);

-- 更新现有评论的评分（可选，也可以通过应用程序逐步更新）
-- 暂时设置为基础评分：点赞数 * 2 + 回复数 * 1
UPDATE comment SET score = (upvote_count * 2.0 + replyCount * 1.0) WHERE score = 0.0;
