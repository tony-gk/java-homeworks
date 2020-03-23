package ru.ifmo.rain.gunkin.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final Queue<Runnable> works;

    public ParallelMapperImpl(int threads) {
        workers = new ArrayList<>(threads);
        works = new ArrayDeque<>();

        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable work;
                        synchronized (works) {
                            while (works.isEmpty()) {
                                works.wait();
                            }
                            work = works.poll();
                            works.notifyAll();
                        }
                        work.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            workers.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        FutureList<R> futureList = new FutureList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int finalIndex = i;
            synchronized (works) {
                works.add(() -> futureList.set(finalIndex, f.apply(args.get(finalIndex))));
                works.notifyAll();
            }
        }
        return futureList.getList();
    }

    private static class FutureList<R> {
        private final List<R> result;
        private int left;

        private FutureList(int size) {
            this.result = new ArrayList<>(Collections.nCopies(size, null));
            left = size;
        }

        public synchronized void set(int index, R value) {
            result.set(index, value);
            left--;
            if (left == 0) {
                notify();
            }
        }

        public synchronized List<R> getList() throws InterruptedException {
            while (left != 0) {
                wait();
            }
            return result;
        }
    }

    @Override
    public void close() {
        for(Thread w: workers) {
            w.interrupt();
        }
    }
}
