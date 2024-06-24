package com.acme.tubeSurf.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    @Test
    void sortWordsCountMapTest() {
        Map<String, Integer> wordCount = new TreeMap<>();
        wordCount.put("one", 1);
        wordCount.put("two", 2);
        wordCount.put("2", 2);
        wordCount.put("A", 7);
        wordCount.put("four", 4);
        wordCount.put("another-two", 2);
        wordCount.put("another-4", 4);
        wordCount.put("another-3", 3);
        wordCount.put("4", 4);

        // Convert the HashMap entries to a list
        List<Map.Entry<String, Integer>> wordList = new ArrayList<>(wordCount.entrySet());

        // Sort the list based on the frequency (value) in descending order
        wordList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        List<Map.Entry<String, Integer>> subList = wordList.subList(0, Math.min(5, wordList.size()));
        // Convert the sorted list back to a Map
        HashMap<String, Integer> topFrequentWordsMap = subList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));

        System.out.println("topFrequentWordsMap = " + topFrequentWordsMap);

        Assertions.assertEquals(5, topFrequentWordsMap.size());

    }
}