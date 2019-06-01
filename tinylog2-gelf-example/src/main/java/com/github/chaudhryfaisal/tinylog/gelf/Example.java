package com.github.chaudhryfaisal.tinylog.gelf;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            log.info("loop {}", i);
            Thread.sleep(100);
        }
        Thread.sleep(10000);
    }
}
