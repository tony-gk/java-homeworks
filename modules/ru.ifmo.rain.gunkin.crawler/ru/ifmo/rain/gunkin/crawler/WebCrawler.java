package ru.ifmo.rain.gunkin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;


public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final Map<String, HostDownloader> hostDownloaders;
    private final int perHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        this.hostDownloaders = new HashMap<>();
        this.perHost = perHost;
    }

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
            System.out.println("The integer numbers expected in arguments");
        } catch (IOException e) {
            System.out.println("An error occurred while initializing downloader: " + e.getMessage());
        }
    }

    private static int getArgumentOrDefault(String[] args, int i) {
        if (args.length <= i) {
            return 1;
        }
        Objects.requireNonNull(args[i]);
        return Integer.parseInt(args[i]);
    }


    @Override
    public Result download(String url, int depth) {
        Map<String, IOException> failed = new ConcurrentHashMap<>();
        Set<String> successful = ConcurrentHashMap.newKeySet();
        Set<String> was = ConcurrentHashMap.newKeySet();

        BlockingQueue<String> nextLevel = new LinkedBlockingQueue<>();
        Phaser phaser = new Phaser(1);

        nextLevel.add(url);
        for (int i = 0; i < depth; i++) {
            List<String> currentLevel = new ArrayList<>();
            nextLevel.drainTo(currentLevel);

            currentLevel.stream()
                    .filter(was::add)
                    .forEach(thatUrl ->
                            addDownloading(thatUrl, phaser, successful, failed, nextLevel));

            phaser.arriveAndAwaitAdvance();
        }

        return new Result(List.copyOf(successful), failed);
    }

    private void addDownloading(String url, Phaser phaser, Set<String> successful,
                                Map<String, IOException> failed, Queue<String> nextLevel) {
        String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            failed.put(url, e);
            return;
        }
        HostDownloader hostDownloader = hostDownloaders.computeIfAbsent(host, s -> new HostDownloader());

        phaser.register();
        hostDownloader.add(() -> {
            try {
                Document document = downloader.download(url);
                phaser.register();
                successful.add(url);
                addExtracting(document, url, phaser, failed, nextLevel);
            } catch (IOException e) {
                failed.put(url, e);
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    private void addExtracting(Document document, String url, Phaser phaser,
                               Map<String, IOException> failed, Queue<String> nextLevel) {
        extractorsPool.submit(() -> {
            try {
                nextLevel.addAll(document.extractLinks());
            } catch (IOException e) {
                failed.put(url, e);
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    @Override
    public void close() {
        extractorsPool.shutdownNow();
        downloadersPool.shutdownNow();
    }

    private class HostDownloader {
        private final Queue<Runnable> downloadQueue;
        private int downloading;

        public HostDownloader() {
            this.downloading = 0;
            downloadQueue = new ArrayDeque<>();
        }

        public synchronized void add(Runnable task) {
            downloadQueue.add(task);
            downloadNext();
        }

        private synchronized void downloadNext() {
            if (downloading < perHost) {
                Runnable task = downloadQueue.poll();
                if (task != null) {
                    downloading++;
                    downloadersPool.submit(() -> {
                        task.run();
                        finished();
                    });
                }
            }
        }

        private synchronized void finished() {
            downloading--;
            downloadNext();
        }
    }
}
