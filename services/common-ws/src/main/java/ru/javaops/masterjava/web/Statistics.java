package ru.javaops.masterjava.web;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Statistics {
    public enum Result {
        SUCCESS, FAIL
    }
    
    public static void count(String payload, long startTime, Result result) {
        long now = System.currentTimeMillis();
        int ms = (int) (now - startTime);
        log.info(payload + " " + result.name() + " execution time(ms): " + ms);
    }
}
