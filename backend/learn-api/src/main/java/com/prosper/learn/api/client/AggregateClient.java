package com.prosper.learn.api.client;

import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(value = "learn-service", contextId = "aggregate")
public interface AggregateClient {

    @GetMapping(value = "/read", params = {"courseId"})
    Response<Object> readByPath(@RequestParam int courseId, @RequestParam(required = false) String path);

    @GetMapping(value = "/read", params = "nodeId")
    Response<Object> readByNode(@RequestParam int nodeId);

    @GetMapping(value = "/read", params = "postId")
    Response<Object> readByPost(@RequestParam int postId);

    @GetMapping(value = "/read", params = "commentId" )
    Response<Object> readByComment(@RequestParam int commentId);

    @GetMapping("/postings")
    Response<Object> getPostings(@RequestParam(value = "id", required = false) List<Integer> ids,
                                 @RequestParam(value = "nodeId", required = false) int nodeId,
                                 @RequestParam(value = "lastScore", required = false) double lastScore,
                                 @RequestParam(value = "lastId", required = false) int lastPostingId);

    @PostMapping("/contents")
    @ResponseBody
    Response<Object> postContents(@RequestParam("path") String path,
                                  @RequestParam("courseId") int courseId,
                                  @RequestParam("postingId") int postingId,
                                  @RequestParam("action") int action,
                                  Model model);

    @PostMapping("/upvote")
    @ResponseBody
    Response<Object> upvote(@RequestParam("objectId") int postingId,
                            @RequestParam("objectType") int objectType,
                            @RequestParam("type") int type);


    @PostMapping("/openai")
    @ResponseBody
    Response<Object> chatWithGPT(@RequestParam String prompt, @RequestParam String model) throws Exception;

    @GetMapping("/message/new-course")
    Response<Map<String, Object>> getApplCourseList(@RequestParam("page") int page, @RequestParam("length") int length);

    @PostMapping("/message/new-course")
    Response applyCourse(@RequestParam("title") String title,
                         @RequestParam("summary") String summary,
                         @RequestParam("explanation") String explanation,
                         @RequestParam("parentId") int parentId);

    @GetMapping("/message")
    Response<List<MessageDTO>> getMessageList(@RequestParam int userId,
                                              @RequestParam int type,
                                              @RequestParam int lastId,
                                              @RequestParam int conversation);

    @PostMapping("/message/system")
    Response postSystemMessage(@RequestParam("type") int type,
                               @RequestParam("to") int userId,
                               @RequestParam("content") String content);

    @PutMapping("/message/system")
    Response modifyCourseApply(@RequestParam("id") int id,
                               @RequestParam("reply") String reply);

}
