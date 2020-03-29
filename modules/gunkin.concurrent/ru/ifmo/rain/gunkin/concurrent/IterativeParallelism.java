package ru.ifmo.rain.gunkin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class MapperParallelism implements AdvancedIP {

    private final ParallelMapper mapper;

    public MapperParallelism() {
        this.mapper = null;
    }

    public MapperParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or advanced.concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @return maximum of given values
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws NoSuchElementException   if not values are given.
     * @throws IllegalArgumentException if number of threads less than 1.
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
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws NoSuchElementException   if not values are given.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelStreamMap(threads, values,
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
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
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
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelStreamMap(threads, values,
                stream -> stream.anyMatch(predicate),
                resultStream -> resultStream.anyMatch(Boolean::booleanValue));
    }

    /**
     * Join values to string.
     *
     * @param threads number of advanced.concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelStreamMap(threads, values,
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
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelStreamMap(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                resultStream -> resultStream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Maps values.
     *
     * @param threads number of advanced.concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelStreamMap(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                resultStream -> resultStream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param lift    mapping function.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if number of threads less than 1.
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return parallelStreamMap(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                resultStream -> resultStream.reduce(monoid.getOperator()).get());
    }

    private <T, R> R parallelStreamMap(int threads, List<T> values,
                                       Function<Stream<T>, R> map,
                                       Function<Stream<R>, R> resultMap) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("Number of threads can't be less than one");
        }
        List<Stream<T>> parts = divideList(values, threads);
        List<R> results;

        if (mapper == null) {
            results = new ArrayList<>(Collections.nCopies(parts.size(), null));
            List<Thread> workers = new ArrayList<>(parts.size());
            for (int i = 0; i < parts.size(); i++) {
                final int finalIndex = i;
                workers.add(new Thread(() -> results.set(finalIndex, map.apply(parts.get(finalIndex)))));
                workers.get(i).start();
            }
            joinThreads(workers);
        } else {
            results =  mapper.map(map, parts);
        }

        return resultMap.apply(results.stream());
    }

    private static void joinThreads(List<Thread> threads) throws InterruptedException {
        InterruptedException ie = null;
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (ie == null) {
                    ie = new InterruptedException("Executing thread was interrupted during joining thread");
                }
                ie.addSuppressed(e);
            }
        }
        if (ie != null) {
            throw ie;
        }
    }

    private static <T> List<Stream<T>> divideList(List<T> list, int partsNumber) {
        int step = list.size() / partsNumber;
        int rest = list.size() % partsNumber;

        List<Stream<T>> parts = new ArrayList<>();
        for (int l = 0; l < list.size(); ) {
            int r = l + step;
            if (rest > 0) {
                r++;
                rest--;
            }
            parts.add(list.subList(l, r).stream());
            l = r;
        }

        return parts;
    }

}
