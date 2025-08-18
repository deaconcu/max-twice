package com.prosper.learn.common;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface Repository<T extends Aggregate<ID>, ID> {

    T find(@NotNull ID id);

    List<T> listByPage(@NotNull int count, @NotNull int offset);

    void remove(@NotNull T aggregate);

    void save(@NotNull T aggregate);
}

