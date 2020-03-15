package ru.ifmo.rain.gunkin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class IterativeParallelism implements ListIP {
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
        return minimum(threads, values, comparator.reversed());
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
        return func(threads, values,
                stream -> stream.min(comparator).get(),
                resultStream -> resultStream.min(comparator).get());
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
        return !any(threads, values, predicate.negate());
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
        return func(threads, values,
                stream -> stream.anyMatch(predicate),
                resultStream -> resultStream.anyMatch(Boolean::booleanValue));
    }

    /**
     * Join values to string.
     *
     * @param threads number of advanced.concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return func(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                resultStream -> resultStream.collect(Collectors.joining()));
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number of advanced.concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return func(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                resutlStream -> resutlStream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Maps values.
     *
     * @param threads number of advanced.concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return func(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                resultStream -> resultStream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    private static <T, U> U func(int threads, List<? extends T> values,
                          Function<Stream<? extends T>, U> mapper,
                          Function<Stream<U>, U> resultMapper) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("Number of threads can't be less than one");
        }
        List<List<? extends T>> parts = divideList(values, threads);
        List<U> results = new ArrayList<>(Collections.nCopies(parts.size(), null));
        List<Thread> workers = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            final int finalIndex = i;
            workers.add(new Thread(() -> results.set(finalIndex, mapper.apply(parts.get(finalIndex).stream()))));
            workers.get(i).start();
        }
        for (int i = 0; i < parts.size(); i++) {
            workers.get(i).join();
        }
        return resultMapper.apply(results.stream());
    }

    private static <T> List<List<? extends T>> divideList(List<? extends T> list, int partsNumber) {
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
}
