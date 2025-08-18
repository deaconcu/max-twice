package com.prosper.learn.persistence.impl;

//@Repository
public class CourseRequestRepositoryImpl { //implements CourseRequestRepository {

    /*
    private final UserMapper courseRequestMapper;

    public CourseRequestRepositoryImpl(UserMapper courseRequestMapper) {
        this.courseRequestMapper = courseRequestMapper;
    }

    @Override
    public CourseRequest find(Integer id) {
        return Converter.INSTANCE.requestToEntity(courseRequestMapper.getById(id));
    }

    @Override
    public List<CourseRequest> listByPage(int limit, int offset) {
        List<CourseRequestDO> result = courseRequestMapper.list(limit, offset);
        return Converter.INSTANCE.requestToEntity(courseRequestMapper.list(limit, offset));
    }

    public List<CourseRequest> listByUser(int userId, int count, int offset) {
        return Converter.INSTANCE.requestToEntity(courseRequestMapper.listByUserId(userId, count, offset));
    }

    @Override
    public void remove(CourseRequest courseRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(CourseRequest courseRequest) {
        if (courseRequest.getId() != null) {
            courseRequestMapper.update(Converter.INSTANCE.requestToDo(courseRequest));
        } else {
            CourseRequestDO courseRequestDO = Converter.INSTANCE.requestToDo(courseRequest);
            courseRequestMapper.insert(courseRequestDO);
            courseRequest.setId(courseRequestDO.getId());
        }
    }

     */
}
