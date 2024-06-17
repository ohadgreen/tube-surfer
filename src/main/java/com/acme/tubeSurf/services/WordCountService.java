package com.acme.tubeSurf.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class WordCountService {
    Logger logger = LoggerFactory.getLogger(WordCountService.class);

    private static Set<String> STOP_WORDS_SET = new HashSet<>();

    @PostConstruct
    protected void loadStopWordsForLanguage() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "stop_words_en.txt";
        File file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());
        InputStream inputStream = new FileInputStream(file);
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        for (String line; (line = reader.readLine()) != null;) {
            STOP_WORDS_SET.add(line);
        }
        logger.info("Stop Words Loaded " + STOP_WORDS_SET.size());
    }

    public void wordsCount(Map<String, Integer> currentMap, List<String> commentsToAnalyze) {

        for (String comment : commentsToAnalyze) {
            String[] wordsSplit = comment.split("\\W+");
            for (String word : wordsSplit) {
                String cleanWord = word.trim().toLowerCase();
                if (!STOP_WORDS_SET.contains(cleanWord)) {
                    if (currentMap.containsKey(cleanWord)) {
                        currentMap.put(cleanWord, currentMap.get(cleanWord) + 1);
                    } else {
                        currentMap.put(cleanWord, 1);
                    }
                }
            }
        }
    }
}
