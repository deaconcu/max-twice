package com.prosper.learn.api.web;

import com.prosper.learn.api.client.UpvoteClient;
import com.prosper.learn.common.Enums;
import com.prosper.learn.dto.Response;
import com.prosper.learn.domain.service.UpvoteService;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.UpvoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class UpvoteController implements UpvoteClient {

    private final UpvoteMapper upvoteMapper;

    private final PostMapper postMapper;

    private final UpvoteService upvoteService;

    @Override
    public List<Integer> getUpvotedList(int userId, List<Integer> postingIds) {
        // todo not used
        return null;
        //return upvoteMapper.getPostingIds(userId, postingIds);
    }

    @Override
    public boolean isUpvoted(int userId, int postingId) {
        UpvoteDO upvoteDO = upvoteMapper.get(userId, postingId, Enums.ObjectType.post.value);
        if (upvoteDO == null) return false;
        return true;
    }

    @Override
    // not used
    public Response upvote(int id, int userId, int type) {
        PostDO postDO = postMapper.get(id);
        if (postDO == null) return Response.notFound;

        //upvoteService.upvote(postingDO, userId, type);
        return Response.success;
    }

    @Override
    public Response cancelVote(int id) {
        /*
        PostingDO postingDO = postingMapper.getById(id);
        if (postingDO == null) return Response.notFound;

        // todo userId
        int userId = 1;
        UpvoteDO upvoteDO = upvoteMapper.get(userId, id);
        if (upvoteDO == null)  return Response.success;

        upvoteService.cancelVote(postingDO, upvoteDO);
        return Response.success;
         */
        return Response.success;
    }
}
