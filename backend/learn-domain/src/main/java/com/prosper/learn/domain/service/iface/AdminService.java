package com.prosper.learn.domain.service.iface;

public interface AdminService {

    void proveCourseRequest(int courseId);

    void rejectCourseRequest(int courseId);
}
