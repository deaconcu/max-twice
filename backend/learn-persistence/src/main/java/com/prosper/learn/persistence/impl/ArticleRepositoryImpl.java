package com.prosper.learn.persistence.impl;

//@Repository
public class ArticleRepositoryImpl { //implements ArticleRepository {

    /*
    @Autowired
    private PostingMapper articleMapper;
    @Autowired
    private HistoryMapper articleCommitMapper;
    @Autowired
    private NodeMapper nodeMapper;

    /*
    public ArticleRepositoryImpl(
        ArticleMapper articleMapper, HistoryMapper articleCommitMapper, NodeMapper nodeMapper) {
        this.articleMapper = articleMapper;
        this.articleCommitMapper = articleCommitMapper;
        this.nodeMapper = nodeMapper;
    }
    */

    /*
    @Override
    public Article find(Integer id) {
        //return Converter.INSTANCE.toEntity(contentsMapper.getById(id));
        // todo
        return null;
    }

    @Override
    public List<Article> listByPage(int count, int offset) {
        //return Converter.INSTANCE.toContentsEntityList(contentsMapper.list(count, offset));
        // todo
        return null;
    }

    @Override
    public void remove(Article article) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(Article article) {
        int articleId = 0;
        if (article.getId() == null) {
            PostingDO articleDo = Converter.INSTANCE.articleToDo(article);
            NodeDO nodeDo = nodeMapper.getById(article.getNodeId());
            if (nodeDo == null) throw new IllegalArgumentException("节点不存在");
            articleMapper.insert(articleDo);
            articleId = articleDo.getId();
        } else {
            PostingDO existArticle = articleMapper.getById(article.getId());
            if (existArticle == null) throw new IllegalArgumentException("文章不存在");
            articleId = existArticle.getId();
        }

        for (ArticleCommit commit: article.getCommits()) {
            HistoryDO articleCommitDo = Converter.INSTANCE.commitToDo(commit);
            articleCommitDo.setArticleId(articleId);
            articleCommitMapper.insert(articleCommitDo);
        }
    }
    */
}
