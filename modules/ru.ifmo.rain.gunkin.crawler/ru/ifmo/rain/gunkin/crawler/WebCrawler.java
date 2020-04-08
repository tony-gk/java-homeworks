package ru.ifmo.rain.gunkin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;


public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaderPool;
    private final ExecutorService extractorPool;

    public static void main(String[] args) {
        Objects.requireNonNull(args);
        if (args.length == 0) {
            System.out.println("Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]");
            return;
        }
        try {
            String url = Objects.requireNonNull(args[0]);
            int depth = getArgumentOrDefault(args, 1);
            int downloaders = getArgumentOrDefault(args, 2);
            int extractors = getArgumentOrDefault(args, 3);
            int perHost = getArgumentOrDefault(args, 4);
            try (Crawler wc = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                wc.download(url, depth);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid argument(s)");
        } catch (IOException e) {
            System.out.println("An error occurred while initializing downloader");
        }
    }

    private static int getArgumentOrDefault(String[] args, int i) {
        if (args.length <= i) {
            return 1;
        }
        Objects.requireNonNull(args[i]);
        return Integer.parseInt(args[i]);
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaderPool = Executors.newFixedThreadPool(downloaders);
        this.extractorPool = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        Map<String, IOException> failed = new ConcurrentHashMap<>();
        Set<String> successful = ConcurrentHashMap.newKeySet();
        Set<String> was = ConcurrentHashMap.newKeySet();

        BlockingQueue<String> nextLevel = new ArrayBlockingQueue<>(1337);
        Phaser phaser = new Phaser(1);

        nextLevel.add(url);
        for (int i = 0; i < depth; i++) {
            List<String> currentLevel = new ArrayList<>();
            nextLevel.drainTo(currentLevel);


            System.out.println(phaser.getPhase());
            currentLevel.stream()
                    .filter(was::add)
                    .forEach(thatUrl -> {
                        phaser.register();
                        downloaderPool.submit(() -> {
                            try {
                                Document document = downloader.download(thatUrl);
                                phaser.register();
                                successful.add(thatUrl);
                                extractorPool.submit(() -> {
                                    try {
                                        nextLevel.addAll(document.extractLinks());
                                    } catch (IOException e) {
                                        failed.put(thatUrl, e);
                                    } finally {
                                        phaser.arriveAndDeregister();
                                    }
                                });
                            } catch (IOException e) {
                                failed.put(thatUrl, e);
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        });
                    });

            phaser.arriveAndAwaitAdvance();

        }
        return new Result(List.copyOf(successful), failed);
    }

    @Override
    public void close() {
        extractorPool.shutdownNow();
        downloaderPool.shutdownNow();
    }
}
