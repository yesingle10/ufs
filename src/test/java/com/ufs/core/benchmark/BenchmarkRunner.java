package com.ufs.core.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH 基准测试入口：通过 main 方法运行全部 benchmark。
 * 默认 3 warmup iterations, 5 measurement iterations, 2 forks。
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JwtBenchmark.class.getSimpleName())
                .include(HmacBenchmark.class.getSimpleName())
                .include(IdGenBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(5)
                .forks(2)
                .build();
        new Runner(opt).run();
    }
}
