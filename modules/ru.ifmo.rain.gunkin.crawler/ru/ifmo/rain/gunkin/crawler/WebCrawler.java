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
        this.hostDownloaders = new ConcurrentHashMap<>();
        this.perHost = perHost;
    }

    public static void main(String[] args) {
        Objects.requireNonNull(args, "Arguments array is null");
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
        Objects.requireNonNull(args[i], (i + 1) + " argument is null");
        return Integer.parseInt(args[i]);
    }

    @Override
    public Result download(String url, int depth) {
        return new BreadthCrawler(url).go(depth).getResult();
    }

    @Override
    public void close() {
        extractorsPool.shutdownNow();
        downloadersPool.shutdownNow();
    }

    private class BreadthCrawler {
        private final Phaser phaser = new Phaser(1);
        private final Map<String, IOException> failed = new ConcurrentHashMap<>();
        private final Set<String> successful = ConcurrentHashMap.newKeySet();
        private final Set<String> was = ConcurrentHashMap.newKeySet();
        private final Queue<String> nextLevel = new ConcurrentLinkedQueue<>();

        private BreadthCrawler(String url) {
            nextLevel.add(url);
        }

        private BreadthCrawler go(int depth) {
            for (int i = 0; i < depth; i++) {
                int size = nextLevel.size();
                for (int j = 0; j < size; j++) {
                    String next = nextLevel.poll();
                    if (was.add(next)) {
                        addDownloading(next);
                    }
                }

                phaser.arriveAndAwaitAdvance();
            }

            return this;
        }

        private Result getResult() {
            return new Result(new ArrayList<>(successful), failed);
        }

        private void addDownloading(String url) {
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
                    addExtracting(document, url);
                } catch (IOException e) {
                    failed.put(url, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }

        private void addExtracting(Document document, String url) {
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
                        done();
                    });
                }
            }
        }

        private synchronized void done() {
            downloading--;
            downloadNext();
        }
    }
}
