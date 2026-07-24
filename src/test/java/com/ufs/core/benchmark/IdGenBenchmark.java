package com.ufs.core.benchmark;

import com.ufs.core.common.util.IdGenerator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * JMH 基准测试：IdGenerator 生成 ID 吞吐量（单线程 + 多线程竞争）。
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class IdGenBenchmark {

    @Benchmark
    @Threads(1)
    public String singleThread() {
        return IdGenerator.newId();
    }

    @Benchmark
    @Threads(4)
    public String multiThread4() {
        return IdGenerator.newId();
    }

    @Benchmark
    @Threads(8)
    public String multiThread8() {
        return IdGenerator.newId();
    }

    @Benchmark
    @Threads(1)
    public String withPrefix() {
        return IdGenerator.newId("f_");
    }
}
