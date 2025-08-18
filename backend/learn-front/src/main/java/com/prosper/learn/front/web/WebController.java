package com.prosper.learn.front.web;

import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.api.client.CourseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

//@Controller
@Slf4j
public class WebController {

    private CourseClient courseApi;

    @RequestMapping("/article/history")
    public ModelAndView articleHistory() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("article_history");
        return modelAndView;
    }

    @RequestMapping("/course")
    public ModelAndView course() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("course");
        CourseDTOV4 courseDTOV4 = courseApi.get(1).getData();
        System.out.println(courseDTOV4);
        return modelAndView;
    }

    @RequestMapping("/course/list")
    public String courseList(Model model) {
        model.addAttribute("course",new CourseDTOV4());
        return "course_list";
    }

    @RequestMapping("/node")
    public ModelAndView node() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("node");
        return modelAndView;
    }

    @RequestMapping("/subcourse")
    public ModelAndView subcourse() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("content");
        return modelAndView;
    }

    @RequestMapping("/user")
    public ModelAndView user() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("user");
        return modelAndView;
    }
}

