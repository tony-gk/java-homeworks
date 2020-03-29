package ru.ifmo.rain.gunkin.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Queue<Runnable> tasks;

    /**
     * Creates a {@code ParallelMapper} instance with the specified number of {@code threads}.
     *
     * @param threads number of threads
     */
    public ParallelMapperImpl(int threads) {
        this.threads = new ArrayList<>(threads);
        this.tasks = new ArrayDeque<>();

        for (int i = 0; i < threads; i++) {
            this.threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        runTask();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            this.threads.get(i).start();
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @param f    function to apply to each element
     * @param args elements to be mapped
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        FutureList<R> futureList = new FutureList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int finalIndex = i;
            synchronized (tasks) {
                tasks.add(() -> futureList.set(finalIndex, f.apply(args.get(finalIndex))));
                tasks.notify();
            }
        }
        return futureList.getList();
    }

    /** Stops all threads. All unfinished mappings leave in undefined state. */
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);

        boolean interrupted = false;
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (InterruptedException e) {
                i--;
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void runTask() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notify();
        }
        task.run();
    }

    private static class FutureList<R> {
        private final List<R> results;
        private int leftTasks;

        private FutureList(int size) {
            this.results = new ArrayList<>(Collections.nCopies(size, null));
            leftTasks = size;
        }

        public synchronized void set(int index, R value) {
            results.set(index, value);
            leftTasks--;
            if (leftTasks == 0) {
                notify();
            }
        }

        public synchronized List<R> getList() throws InterruptedException {
            while (leftTasks != 0) {
                wait();
            }
            return results;
        }
    }

}
