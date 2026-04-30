package com.twicemax.shared.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalJsonTest {

    @Test
    void objectKeysAreSortedAlphabetically() {
        String a = "{\"b\":2,\"a\":1}";
        String b = "{\"a\":1,\"b\":2}";
        assertEquals(canonical(a), canonical(b));
        assertEquals("{\"a\":1,\"b\":2}", canonical(a));
    }

    @Test
    void arrayOrderIsPreserved() {
        String forward = "[1,2,3]";
        String reversed = "[3,2,1]";
        assertNotEquals(canonical(forward), canonical(reversed));
    }

    @Test
    void nestedObjectsAreSorted() {
        String input = "{\"z\":{\"y\":2,\"x\":1},\"a\":[{\"q\":2,\"p\":1}]}";
        String expected = "{\"a\":[{\"p\":1,\"q\":2}],\"z\":{\"x\":1,\"y\":2}}";
        assertEquals(expected, canonical(input));
    }

    @Test
    void roadmapLikePayloadStableAcrossReorder() {
        // 仿 roadmap v=2 协议的载荷：trunk 是有序数组，节点对象内字段顺序不应影响 hash。
        String s1 = "{\"v\":2,\"trunk\":[{\"t\":\"c\",\"id\":1,\"label\":\"A\"}," +
                "{\"label\":\"B\",\"t\":\"n\",\"id\":2}]}";
        String s2 = "{\"trunk\":[{\"id\":1,\"label\":\"A\",\"t\":\"c\"}," +
                "{\"id\":2,\"label\":\"B\",\"t\":\"n\"}],\"v\":2}";
        assertEquals(CanonicalJson.hash(s1), CanonicalJson.hash(s2));
    }

    @Test
    void differentContentDifferentHash() {
        String a = "{\"v\":2,\"trunk\":[]}";
        String b = "{\"v\":2,\"trunk\":[{\"t\":\"c\",\"id\":1,\"label\":\"A\"}]}";
        assertNotEquals(CanonicalJson.hash(a), CanonicalJson.hash(b));
    }

    @Test
    void invalidJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> canonical("{not json"));
    }

    @Test
    void hashIsHex64Chars() {
        String h = CanonicalJson.hash("{}");
        assertEquals(64, h.length());
        // 全部为 hex 字符
        for (char c : h.toCharArray()) {
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!ok) {
                throw new AssertionError("non-hex char: " + c);
            }
        }
    }

    private String canonical(String json) {
        return CanonicalJson.canonicalize(json);
    }
}
