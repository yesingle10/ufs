package com.ufs.core.benchmark;

import com.ufs.core.auth.HmacSigner;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * JMH 基准测试：HMAC-SHA256 签名吞吐量（不同 payload 大小）。
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class HmacBenchmark {

    @Param({"100", "1024", "10240"})
    private int payloadSize;

    private String appSecret;
    private String canonical;

    @Setup
    public void setup() {
        appSecret = "benchmark-hmac-secret-32bytes-pad!";
        canonical = buildPayload(payloadSize);
    }

    private static String buildPayload(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('x');
        }
        return sb.toString();
    }

    @Benchmark
    public String sign() {
        return HmacSigner.sign(appSecret, canonical);
    }
}
