package com.acme.tubeSurf.services;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class CommentsChunkHandlerTest {

    private Map<Integer, String> treeMap = new TreeMap<>();
    private int mapMaxSize = 3;
    @Test
    void treeMapTest() {

        insertToTreeMap(7, "seven");
        insertToTreeMap(3, "three");
        insertToTreeMap(4, "four");
        insertToTreeMap(4, "four");
        insertToTreeMap(5, "five");
        insertToTreeMap(3, "three");
        insertToTreeMap(5, "five");

        System.out.println("treeMap = " + treeMap);
        assertEquals(3, treeMap.size());
    }

    private void insertToTreeMap(Integer key, String value) {
        if (treeMap.size() < mapMaxSize) {
            treeMap.put(key, value);
        } else {
            Integer lowestKey = treeMap.keySet().iterator().next();
            if (key > lowestKey) {
                treeMap.remove(lowestKey);
                treeMap.put(key, value);
            }
        }
    }
}