package com.prosper.learn.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 并查集实现
 */
public class UnionFind {
    private Map<Integer, Integer> parent;
    private Map<Integer, Integer> rank;
    private int componentCount;

    public UnionFind(Set<Integer> nodes) {
        parent = new HashMap<>();
        rank = new HashMap<>();
        componentCount = nodes.size();

        for (Integer node : nodes) {
            parent.put(node, node);
            rank.put(node, 0);
        }
    }

    public int find(int x) {
        if (parent.get(x) != x) {
            parent.put(x, find(parent.get(x)));
        }
        return parent.get(x);
    }

    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX != rootY) {
            if (rank.get(rootX) < rank.get(rootY)) {
                parent.put(rootX, rootY);
            } else if (rank.get(rootX) > rank.get(rootY)) {
                parent.put(rootY, rootX);
            } else {
                parent.put(rootY, rootX);
                rank.put(rootX, rank.get(rootX) + 1);
            }
            componentCount--;
        }
    }

    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }

    public int getComponentCount() {
        return componentCount;
    }
}
