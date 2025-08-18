package com.prosper.learn.persistence.impl;

import org.springframework.stereotype.Repository;

@Repository
public class SubcourseRepositoryImpl { //implements SubcourseRepository {

    /*
    private final SubcourseMapper subcourseMapper;

    public SubcourseRepositoryImpl(SubcourseMapper subcourseMapper) {
        this.subcourseMapper = subcourseMapper;
    }

    @Override
    public Subcourse find(Integer id) {
        return Converter.INSTANCE.subcourseToEntity(subcourseMapper.getById(id));
    }

    @Override
    public List<Subcourse> findByCourse(int courseId) {
        return Converter.INSTANCE.subcourseToEntity(subcourseMapper.getByCourse(courseId));
    }

    @Override
    public List<Subcourse> listByPage(int count, int offset) {
        return Converter.INSTANCE.subcourseToEntity(subcourseMapper.list(count, offset));
    }

    @Override
    public void remove(Subcourse subcourse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(Subcourse subcourse) {
        if (subcourse.getId() == null) {
            SubcourseDO subcourseInDb = subcourseMapper.getByNameAndCourse(subcourse.getCourseId(), subcourse.getName());
            if (subcourseInDb != null) throw new IllegalArgumentException("子课程名相同");
            subcourse.setUserId(1);
            SubcourseDO subcourseDO = Converter.INSTANCE.subcourseToDo(subcourse);
            subcourseMapper.insert(subcourseDO);
            subcourse.setId(subcourseDO.getId());
        } else {
            subcourseMapper.update(Converter.INSTANCE.subcourseToDo(subcourse));
        }
    }
    */

}
