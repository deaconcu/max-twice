package com.prosper.learn.common.types;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
public class Page {

    private int value;

    public Page(int value) {
        if (value < 1) value = 1;
        this.value = value;
    }
}
