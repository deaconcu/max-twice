package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.repository.CourseRequestRepository;
import lombok.Data;

import java.util.Date;

@Data
public class CourseRequest implements Aggregate<Integer> {

    private Integer id;

    private String courseName;

    private String courseDesc;

    private String subcourseName;

    private String subcourseDesc;

    private Enums.CourseRequestState state;

    private int userId;

    private Date createTime;

    private Date updateTime;

    public CourseRequest(String courseName, String courseDesc, String subcourseName, String subcourseDesc) {
        this.courseName = courseName;
        this.courseDesc = courseDesc;
        this.subcourseName = subcourseName;
        this.subcourseDesc = subcourseDesc;
        this.state = Enums.CourseRequestState.submitted;
    }

    public boolean prove() {
        if (state != Enums.CourseRequestState.submitted) return false;
        state = Enums.CourseRequestState.proved;
        return true;
    }

    public boolean reject() {
        if (state != Enums.CourseRequestState.submitted) return false;
        state = Enums.CourseRequestState.reject;
        return true;
    }

    public static CourseRequest load(int id, CourseRequestRepository courseRequestRepository) {
        return courseRequestRepository.find(id);
    }

    public void save(CourseRequestRepository courseRequestRepository) {
        courseRequestRepository.save(this);
    }
}
