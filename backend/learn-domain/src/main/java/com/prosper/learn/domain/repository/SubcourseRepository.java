package com.prosper.learn.domain.repository;

import com.prosper.learn.common.Repository;
import com.prosper.learn.domain.entity.Subcourse;

import java.util.List;

public interface SubcourseRepository extends Repository<Subcourse, Integer> {

    List<Subcourse> findByCourse(int courseId);
}
