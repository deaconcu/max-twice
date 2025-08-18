package com.prosper.learn.front.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class TemplateUtils {

    public int countChild(ObjectNode node) {
        int size = node.size();
        if (node.has("^")) size --;
        if (node.has("+")) size --;
        return size;
    }

    public int countChild(HashMap map) {
        int size = map.size();
        if (map.containsKey("^")) size --;
        if (map.containsKey("+")) size --;
        return size;
    }
}
