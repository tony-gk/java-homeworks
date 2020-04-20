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
     * @param threadCount number of threads
     */
    public ParallelMapperImpl(int threadCount) {
        this.threads = new ArrayList<>(threadCount);
        this.tasks = new LinkedList<>();

        for (int i = 0; i < threadCount; i++) {
            this.threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        runTask();
                    }
                } catch (InterruptedException ignored) {
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
        ResultList<R> resultList = new ResultList<>(args.size());
        List<RuntimeException> runtimeExceptions = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            final int finalIndex = i;
            synchronized (tasks) {
                tasks.add(() -> {
                    R result = null;
                    try {
                        result = f.apply(args.get(finalIndex));
                    } catch (RuntimeException e) {
                        synchronized (runtimeExceptions) {
                            runtimeExceptions.add(e);
                        }
                    }
                    resultList.set(finalIndex, result);
                });
                tasks.notify();
            }
        }

        if (!runtimeExceptions.isEmpty()) {
            RuntimeException firstException = runtimeExceptions.get(0);
            runtimeExceptions.stream().skip(1).forEach(firstException::addSuppressed);
            throw firstException;
        }

        return resultList.getList();
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        threads.forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {
                    }
                }
        );
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

    private static class ResultList<R> {
        private final List<R> results;
        private int done;

        private ResultList(int size) {
            this.results = new ArrayList<>(Collections.nCopies(size, null));
            this.done = 0;
        }

        private synchronized void set(int index, R value) {
            results.set(index, value);
            done++;
            if (done == results.size()) {
                notify();
            }
        }

        private synchronized List<R> getList() throws InterruptedException {
            while (done != results.size()) {
                wait();
            }
            return results;
        }
    }
}
