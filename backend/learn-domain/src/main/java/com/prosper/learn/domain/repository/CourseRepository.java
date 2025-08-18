package com.prosper.learn.domain.repository;

import com.prosper.learn.common.Repository;
import com.prosper.learn.domain.entity.Course;
import jakarta.validation.constraints.NotNull;


public interface CourseRepository extends Repository<Course, Integer> {

    Course find(@NotNull String name);
}
