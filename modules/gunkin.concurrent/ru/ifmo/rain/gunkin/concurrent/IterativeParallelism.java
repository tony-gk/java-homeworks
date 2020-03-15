package ru.ifmo.rain.gunkin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class IterativeParallelism implements ScalarIP {
    /**
     * Returns maximum value.
     *
     * @param threads    number or advanced.concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @return maximum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        List<List<? extends T>> parts = divideList(values, threads);
        List<T> results = new ArrayList<>(parts.size());
        List<Thread> workers = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            int finalI = i;
            workers.add(new Thread(() -> {
                List<? extends T> part = parts.get(finalI);
                results.add(part.stream().max(comparator).get());
            }));

            workers.get(i).start();
        }
        for (int i =0;i< parts.size(); i++) {
            workers.get(i).join();
        }
        return results.stream().max(comparator).get();
    }

    private <T> List<List<? extends T>> divideList(List<? extends T> list, int partsNumber) {
        int step = list.size() / partsNumber;
        int rest = list.size() % partsNumber;

        List<List<? extends T>> parts = new ArrayList<>();
        for (int l = 0; l < list.size(); ) {
            int r = l + step;
            if (rest > 0) {
                r++;
                rest--;
            }
            parts.add(list.subList(l, r));
            l = r;
        }

        return parts;
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or advanced.concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @return minimum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        List<List<? extends T>> parts = divideList(values, threads);
        List<T> results = new ArrayList<>(parts.size());
        List<Thread> workers = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            int finalI = i;
            workers.add(new Thread(() -> {
                List<? extends T> part = parts.get(finalI);
                results.add(part.stream().min(comparator).get());
            }));

            workers.get(i).start();
        }
        for (int i =0;i< parts.size(); i++) {
            workers.get(i).join();
        }
        return results.stream().min(comparator).get();
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or advanced.concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<List<? extends T>> parts = divideList(values, threads);
        List<Boolean> results = new ArrayList<>(parts.size());
        List<Thread> workers = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            int finalI = i;
            workers.add(new Thread(() -> {
                List<? extends T> part = parts.get(finalI);
                results.add(part.stream().allMatch(predicate));
            }));

            workers.get(i).start();
        }
        for (int i =0;i< parts.size(); i++) {
            workers.get(i).join();
        }
        return results.stream().allMatch(b -> b);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or advanced.concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<List<? extends T>> parts = divideList(values, threads);
        List<Boolean> results = new ArrayList<>(parts.size());
        List<Thread> workers = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            int finalI = i;
            workers.add(new Thread(() -> {
                List<? extends T> part = parts.get(finalI);
                results.add(part.stream().anyMatch(predicate));
            }));

            workers.get(i).start();
        }
        for (int i =0;i< parts.size(); i++) {
            workers.get(i).join();
        }
        return results.stream().anyMatch(b -> b);
    }
}
