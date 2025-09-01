package com.prosper.learn.persistence.impl;

//@Repository
public class CourseRepository {

    /*
    private final CourseMapper courseMapper;

    public CourseRepository(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    //@Override
    public Course find(Long id) {
        return Converter.INSTANCE.courseToEntity(courseMapper.getById(id));
    }

    //@Override
    public Course find(String name) {
        return Converter.INSTANCE.courseToEntity(courseMapper.getByName(name));
    }

    //@Override
    public List<Course> listByPage(int count, int offset) {
        return Converter.INSTANCE.courseToEntity(courseMapper.listAll(count, offset));
    }

    //@Override
    public void remove(Course course) {
        throw new UnsupportedOperationException();
    }

    //@Override
    public void save(Course course) {
        if (course.getId() == null) {
            CourseDO courseInDb = courseMapper.getByName(course.getName());
            if (courseInDb != null) throw new IllegalArgumentException("课程名相同");
            course.setUserId(1);
            course.setState(Enums.CourseState.created);
            CourseDO courseDO = Converter.INSTANCE.courseToDo(course);
            courseMapper.insert(courseDO);
            course.setId(courseDO.getId());
        } else {
            courseMapper.update(Converter.INSTANCE.courseToDo(course));
        }
    }

     */


}
