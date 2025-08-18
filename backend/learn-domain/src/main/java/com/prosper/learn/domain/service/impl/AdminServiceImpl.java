package com.prosper.learn.domain.service.impl;

import com.prosper.learn.domain.entity.*;
import com.prosper.learn.domain.repository.*;
import com.prosper.learn.domain.service.iface.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SubcourseRepository subcourseRepository;
    @Autowired
    private CourseRequestRepository courseRequestRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private NodeTableRepository nodeTableRepository;

    @Override
    @Transactional
    public void proveCourseRequest(int courseId) {
        CourseRequest courseRequest = courseRequestRepository.find(courseId);
        if (courseRequest == null || !courseRequest.prove()) return;

        Course course = new Course(courseRequest.getCourseName(), courseRequest.getCourseDesc(), 1);
        courseRepository.save(course);

        Subcourse subcourse = new Subcourse(
            courseRequest.getSubcourseName(), courseRequest.getSubcourseDesc(), course.getId(), 1);
        subcourseRepository.save(subcourse);

        Node node = new Node(subcourse.getId());
        nodeRepository.save(node);

        NodeTable nodeTable = new NodeTable(node.getId(), node.getSubcourseId());
        nodeTableRepository.save(nodeTable);

        courseRequestRepository.save(courseRequest);
    }

    @Override
    public void rejectCourseRequest(int courseId) {
        CourseRequest courseRequest = courseRequestRepository.find(courseId);
        courseRequest.reject();
        courseRequestRepository.save(courseRequest);
    }
}
