package ru.ifmo.rain.gunkin.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        List<Integer> a = IntStream.range(0, 100_000).boxed().collect(Collectors.toList());
        ParallelMapper pm = new ParallelMapperImpl(6);
        List<Integer> res = pm.map((i) -> i * 2, a);
        pm.close();
        for (int i = 0; i < res.size(); i++) {
            if (res.get(i) / 2 != i) {
                System.err.println("wowo");
                return;
            }
        }
    }
}
