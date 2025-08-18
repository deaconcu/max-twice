package com.prosper.learn.user;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper mapper; // 具体的DAO接口
    //private final UserDataConverter converter; // 转化器

    public UserRepositoryImpl(UserMapper mapper) {
        this.mapper = mapper;
        //this.converter = OrderDataConverter.INSTANCE;
    }

    @Override
    public User find(Integer integer) {
        return null;
    }

    @Override
    public List<User> listByPage(int count, int offset) {
        return null;
    }

    @Override
    public void remove(User user) {

    }

    @Override
    public void save(User user) {

    }
}
