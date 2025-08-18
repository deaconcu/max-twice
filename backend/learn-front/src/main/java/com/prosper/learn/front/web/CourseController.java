package com.prosper.learn.front.web;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prosper.learn.api.client.*;
import com.prosper.learn.common.Utils;
import com.prosper.learn.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@Slf4j
public class CourseController {

    /*
    private static final int COURSE_REQUEST_PAGE_SIZE = 10;
    // 课程每一页的数量
    private static final int COURSE_PAGE_SIZE = 16;
    private static final int USER_NODE_PAGE_SIZE = 20;

    final CourseClient courseClient;
    final NodeClient nodeClient;
    final PostClient postClient;
    final ContentsClient contentsClient;
    final UpvoteClient upvoteClient;
    final ObjectMapper objectMapper;
    final TemplateUtils utils;
    final CommentClient commentClient;

    public CourseController(
            CourseClient courseClient, NodeClient nodeClient, PostClient postClient,
            ContentsClient contentsClient, ObjectMapper objectMapper, TemplateUtils utils,
            UpvoteClient upvoteClient, CommentClient commentClient) {
        this.courseClient = courseClient;
        this.nodeClient = nodeClient;
        this.postClient = postClient;
        this.contentsClient = contentsClient;
        this.objectMapper = objectMapper;
        this.utils = utils;
        this.upvoteClient = upvoteClient;
        this.commentClient = commentClient;
    }

    @GetMapping("/course/list")
    @SaCheckLogin
    public String courseList(@RequestParam(value="page", defaultValue = "1") int page,  Model model) {
        //List<CourseDTO> courseDTOList = courseClient.list(CourseState.proved.value, 0, 0, page, COURSE_PAGE_SIZE).getData();
        //Response<List<CourseDTO>> response = courseClient.list(CourseState.proved.value, 0, 0, page, COURSE_PAGE_SIZE);
        List<CourseDTOV4> courseDTOV4List = null;//response.getData();
        model.addAttribute("courseList", courseDTOV4List);
        model.addAttribute("page", page < 1 ? 1 : page);
        model.addAttribute("course", new CourseDTOV4());
        return "course_list";
    }

    @PostMapping("/course")
    @SaCheckLogin
    public String add(@ModelAttribute(value="course") CourseDTOV4 courseDTOV4, Model model) {
        courseClient.post(courseDTOV4);
        model.addAttribute("redirectUrl", "/user/course");
        return "success";
    }

    @GetMapping("/course/{id}")
    @SaCheckLogin
    public String course(@PathVariable(value="id") int id, Model model) {
        CourseDTOV4 courseDTOV4 = courseClient.get(id).getData();
        model.addAttribute("course", courseDTOV4);

        List<CourseDTOV4> courseList = null; //courseClient.list(CourseState.proved.value, 0, id, 0, COURSE_PAGE_SIZE).getData();
        model.addAttribute("subcourseList", courseList);

        CourseDTOV4 newSubcourse = new CourseDTOV4();
        newSubcourse.setParent(id);
        model.addAttribute("newSubcourse", newSubcourse);

        return "course";
    }

    @GetMapping("/posting/{id}")
    @SaCheckLogin
    @ResponseBody
    public Response<Map> posting(@PathVariable int id) {
        // todo
        int userId = StpUtil.getLoginIdAsInt();
        PostDTO postDTO = postClient.get(id);
        Map<String, Object> data = new HashMap<>();
        data.put("upvoteCount", postDTO.getHelpful());

        boolean upvoted = upvoteClient.isUpvoted(userId, id);
        data.put("upvoted", upvoted);
        return new Response<>(data);
    }

    @GetMapping("/node/{nodeId}/posting/pullrefresh")
    @SaCheckLogin
    public String getPostingByPage(@PathVariable int nodeId,
                                   @RequestParam("courseId") int courseId,
                                   @RequestParam("lastId") int lastPostingId, Model model) {

        List<PostDTO> postDTOList = postClient.getByLastId(nodeId, lastPostingId);
        List<Integer> allPostingIds = new ArrayList<>();
        if (postDTOList != null) postDTOList.stream().forEach(item->{
            allPostingIds.add(item.getId());
        });

        Response<CourseTocDTO> response = contentsClient.get(1, courseId, true);
        CourseTocDTO courseTocDTO = response.getData();

        // todo
        int userId = StpUtil.getLoginIdAsInt();
        List<Integer> upvotedPostingIds = new ArrayList<>();
        if (allPostingIds.size() > 0) {
            //upvotedPostingIds = upvoteClient.getUpvotedList(userId, allPostingIds);
        }

        int lastId = allPostingIds.size() > 0 ? allPostingIds.get(allPostingIds.size() - 1) : 0;

        model.addAttribute("postings", postDTOList);
        model.addAttribute("type", 3);
        model.addAttribute("upvotedPostingIds", upvotedPostingIds);
        model.addAttribute("contentsNames", courseTocDTO.getNames());
        model.addAttribute("lastId", lastId);

        //return "content :: renderPosting";
        return "content_posting_pull_refresh";
    }

    @GetMapping("/node/{id}/posting")
    @SaCheckLogin
    public String content(@PathVariable int id,
                          @RequestParam(value="path", defaultValue = "") String path,
                          Model model) {
        NodeDTO node = nodeClient.get(id);
        CourseDTOV4 course = courseClient.get(node.getCourseId()).getData();

        int userId = StpUtil.getLoginIdAsInt();
        Response<CourseTocDTO> response = contentsClient.get(userId, course.getId(), true);
        CourseTocDTO courseTocDTO = response.getData();

        //Map<String, Object> contents;
        List<Object> contents;
        List<PostDTO> fixedPostings = null;
        PostDTO chosenPosting = null;
        List<Integer> fixedIds = null;
        try {
            contents = objectMapper.readValue(courseTocDTO.getContents(), List.class);
            JsonNode rootNode = objectMapper.readTree(courseTocDTO.getContents());
            JsonNode currentNode = Utils.getNodeByPath(rootNode, path).right();
            if (currentNode != null && currentNode.has("+")) {
                chosenPosting = postClient.get(currentNode.get("+").asInt());
            }
            if (currentNode != null && currentNode.has("^")) {
                ArrayNode idsNode = (ArrayNode)currentNode.get("^");
                fixedIds = objectMapper.convertValue(idsNode, List.class);
                //fixedPostings = postingClient.get(fixedIds);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<PostDTO> otherPostings = postClient.getPostings(id);

        // 移除已选择和置顶的帖子
        Iterator<PostDTO> iterator = otherPostings.iterator();
        while (iterator.hasNext()) {
            PostDTO postDTO = iterator.next();
            int currId = postDTO.getId();
            if((chosenPosting != null && currId == chosenPosting.getId()) ||
               (fixedIds != null) && fixedIds.contains(currId)){
                iterator.remove();
            }
        }

        List<Integer> allPostingIds = new ArrayList<>();
        if (chosenPosting != null) allPostingIds.add(chosenPosting.getId());
        if (fixedPostings != null) fixedPostings.stream().forEach(item->allPostingIds.add(item.getId()));
        if (otherPostings != null) otherPostings.stream().forEach(item->allPostingIds.add(item.getId()));

        List<Integer> upvotedPostingIds = new ArrayList<>();
        if (allPostingIds.size() > 0) {
             //upvotedPostingIds = upvoteClient.getUpvotedList(userId, allPostingIds);
        }

        PostDTO posting = new PostDTO();
        posting.setNodeId(node.getId());

        model.addAttribute("utils", utils);
        model.addAttribute("node", node);
        model.addAttribute("course", course);
        model.addAttribute("chosenPosting", chosenPosting);
        model.addAttribute("fixedPostings", fixedPostings);
        model.addAttribute("otherPostings", otherPostings);
        model.addAttribute("lastId", allPostingIds.get(allPostingIds.size() - 1));
        model.addAttribute("upvotedPostingIds", upvotedPostingIds);
        model.addAttribute("newPosting", posting);
        model.addAttribute("contents", contents);
        model.addAttribute("contentsNames", courseTocDTO.getNames());
        model.addAttribute("path", path);
        return "content";
    }

    @PostMapping("/node/{id}/posting")
    public String addPosting(@PathVariable int id, @ModelAttribute(value="newPosting") PostDTO postDTO, Model model) {
        postClient.create(postDTO);
        model.addAttribute("redirectUrl", "/node/" + id + "/posting");
        return "success";
    }

    @GetMapping("/user/course")
    public String userNodeList(@RequestParam(value="page", defaultValue = "1") int page,  Model model) {
        List<CourseDTOV4> courseDTOV4List = null; //courseClient.list(CourseState.all.value, 0, 0, 0,  COURSE_PAGE_SIZE).getData();
        model.addAttribute("courses", courseDTOV4List);
        return "user_course_list";
    }

    @PostMapping("/contents")
    @ResponseBody
    public Response<Object> postContents(@RequestParam("nodeId") int nodeId,
                               @RequestParam("path") String path,
                               @RequestParam("courseId") int courseId,
                               @RequestParam("postingId") int postingId,
                               @RequestParam("action") int action,
                               Model model) {
        int userId = StpUtil.getLoginIdAsInt();
        switch (action) {
            case 1:
                return contentsClient.choose(userId, path, courseId, postingId);
            case 2:
                return contentsClient.unchoose(userId, courseId, path);
            case 3:
                return contentsClient.pin(userId, courseId, path, postingId);
            case 4:
                return contentsClient.unpin(userId, courseId, path, postingId);
            default:
                return Response.success;
        }
    }

    @PostMapping("/posting/{id}/vote")
    @ResponseBody
    public Response<String> vote(@PathVariable int id, Model model) {
        return upvoteClient.upvote(id, StpUtil.getLoginIdAsInt(), 3);
    }

    @GetMapping("/comment")
    public String getCommentList(@RequestParam("postingId") int postingId,
                                 @RequestParam("offsetId") int offsetId, Model model) {
        List<CommentDTO> comments = commentClient.getByObject(postingId, 0, offsetId).getData();
        PostDTO posting = new PostDTO();

        model.addAttribute("comments", comments);
        model.addAttribute("posting", posting);
        return "comment";
    }


    @PostMapping("/comment")
    @ResponseBody
    public Response<Object> addComment(
            @RequestParam("content") String content, @RequestParam("postingId") int postingId,
            @RequestParam(name = "replyTo", defaultValue = "0", required = false) int replyTo,
            @RequestParam(name = "toUser", defaultValue = "0", required = false) int toUser) {

        int userId = StpUtil.getLoginIdAsInt();

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent(content);
        commentDTO.setObjectId(postingId);
        commentDTO.setReplyTo(replyTo);
        commentDTO.setToUser(toUser);
        commentDTO.setFromUser(userId);

        commentClient.create(commentDTO);
        return Response.success;
    }

    /*
    @RequestMapping("/course/content/{id}")
    public String courseContent(@PathVariable(value="id") int id, Model model) {
        CourseDTO course = courseClient.get(id);
        ContentsDTO contentsDTO = subcourseClient.getContents(id);

        model.addAttribute("course", courseDTO);
        model.addAttribute("subcourse", subcourse);
        model.addAttribute("contents", contentsDTO);
        return "subcourse";
    }

    @PostMapping("/course")
    public String proveCourseRequest(@PathVariable(value="id") int id,  Model model) {
        courseRequestClient.prove(id);
        model.addAttribute("redirectUrl", "/courseRequest/list");
        return "success";
    }

    @PostMapping("/courseRequest/decline/{id}")
    public String declineCourseRequest(@PathVariable(value="id") int id,  Model model) {
        courseRequestClient.decline(id);
        model.addAttribute("redirectUrl", "/courseRequest/list");
        return "success";
    }


    @RequestMapping("/subcourse/node-{id}/article/")
    public String article(@PathVariable int id, Model model) {
        SubcourseDTO subcourse = subcourseClient.get(id);
        CourseDTO courseDTO = courseClient.get(subcourse.getCourseId());
        ContentsDTO contentsDTO = subcourseClient.getContents(id);
        List<PostingDTO> nodeLists = nodeClient.getNodeTable(id);

        model.addAttribute("course", courseDTO);
        model.addAttribute("subcourse", subcourse);
        model.addAttribute("contents", contentsDTO);
        model.addAttribute("nodeLists", nodeLists);
        return "subcourse";
    }

    @GetMapping("/subcourse-{id}/node/create")
    public String addNode(@PathVariable int id, Model model) {
        SubcourseDTO subcourse = subcourseClient.get(id);
        CourseDTO courseDTO = courseClient.get(subcourse.getCourseId());
        model.addAttribute("course", courseDTO);
        model.addAttribute("subcourse", subcourse);
        model.addAttribute("node", new NodeDTO());
        return "create_node";
    }

    @PostMapping("/subcourse-{subcourseId}/node/create")
    public String addNode(@PathVariable int subcourseId, @ModelAttribute(value="node")NodeDTO nodeDTO, Model model) {
        nodeDTO.setSubcourseId(subcourseId);
        nodeClient.create(nodeDTO);
        model.addAttribute("redirectUrl", "/node/list");
        return "success";
    }

    @GetMapping("/user/node")
    public String userNodeList(@RequestParam(value="page", defaultValue = "1") int page,  Model model) {
        List<NodeDTO> nodes = nodeClient.getUserNodeList(page, USER_NODE_PAGE_SIZE);
        model.addAttribute("nodes", nodes);
        return "user_node_list";
    }

    @RequestMapping("/subcourse/node-{id}/nodeList/")
    public String nodeList(@PathVariable int id, Model model) {
        NodeDTO node = nodeClient.get(id);
        List<PostingDTO> nodeLists = nodeClient.getNodeTable(id);

        SubcourseDTO subcourse = subcourseClient.get(node.getSubcourseId());
        CourseDTO course = courseClient.get(subcourse.getCourseId());
        ContentsDTO contents = subcourseClient.getContents(subcourse.getId());

        model.addAttribute("course", course);
        model.addAttribute("subcourse", subcourse);
        model.addAttribute("contents", contents);
        model.addAttribute("node", node);
        model.addAttribute("nodeLists", nodeLists);
        return "node";
    }

    @RequestMapping("/subcourse/node-{id}/nodeList/create")
    public String createNodeList(@PathVariable int id, Model model) {
        NodeDTO node = nodeClient.get(id);
        SubcourseDTO subcourse = subcourseClient.get(node.getSubcourseId());
        CourseDTO course = courseClient.get(subcourse.getCourseId());

        model.addAttribute("course", course);
        model.addAttribute("subcourse", subcourse);
        model.addAttribute("node", node);
        return "create_node_list";
    }


     */
}