package com.prosper.learn.web.client;

import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.dto.response.Response;
import com.prosper.learn.dto.response.ReadDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(value = "learn-service", contextId = "aggregate")
public interface AggregateClient {

    //@GetMapping(value = "/read", params = {"courseId"})
    Response<ReadDTO> readByPath(@RequestParam Long courseId, @RequestParam(required = false) String path);

    //@GetMapping(value = "/read", params = "nodeId")
    Response<ReadDTO> readByNode(@RequestParam Long nodeId);

    //@GetMapping(value = "/read", params = "postId")
    Response<ReadDTO> readByPost(@RequestParam Long postId);

    //@GetMapping(value = "/read", params = "commentId" )
    Response<ReadDTO> readByComment(@RequestParam Long commentId);

    //@GetMapping("/postings")
    Response<Object> getPostings(@RequestParam(value = "id", required = false) List<Long> ids,
                                 @RequestParam(value = "nodeId", required = false) Long nodeId,
                                 @RequestParam(value = "lastScore", required = false) double lastScore,
                                 @RequestParam(value = "lastId", required = false) Long lastId);

    //@PostMapping("/contents")
    @ResponseBody
    Response<Object> postContents(@RequestParam("path") String path,
                                  @RequestParam("courseId") Long courseId,
                                  @RequestParam("postingId") Long postingId,
                                  @RequestParam("action") int action,
                                  Model model);

    //@PostMapping("/upvote")
    @ResponseBody
    Response<Object> upvote(@RequestParam("objectId") Long postingId,
                            @RequestParam("objectType") int objectType,
                            @RequestParam("type") int type);


    //@PostMapping("/openai")
    @ResponseBody
    Response<Object> chatWithGPT(@RequestParam String prompt, @RequestParam String model) throws Exception;

    //@GetMapping("/message/new-course")
    Response<Map<String, Object>> getApplCourseList(@RequestParam("page") int page, @RequestParam("length") int length);

    //@PostMapping("/message/new-course")
    Response applyCourse(@RequestParam("title") String title,
                         @RequestParam("summary") String summary,
                         @RequestParam("explanation") String explanation,
                         @RequestParam("parentId") Long parentId);

    //@GetMapping("/message")
    Response<List<MessageDTO>> getMessageList(@RequestParam Long userId,
                                              @RequestParam int type,
                                              @RequestParam Long lastId,
                                              @RequestParam int conversation);

    //@PostMapping("/message/system")
    Response postSystemMessage(@RequestParam("type") int type,
                               @RequestParam("to") Long userId,
                               @RequestParam("content") String content);

    //@PutMapping("/message/system")
    Response modifyCourseApply(@RequestParam("id") Long id,
                               @RequestParam("reply") String reply);

}
