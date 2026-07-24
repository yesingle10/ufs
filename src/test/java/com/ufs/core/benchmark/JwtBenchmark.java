package com.ufs.core.benchmark;

import com.ufs.core.auth.JwtService;
import com.ufs.core.config.properties.UfsProperties;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * JMH 基准测试：JWT 签发与验证吞吐量。
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class JwtBenchmark {

    private JwtService jwtService;
    private String preIssuedToken;

    @Setup
    public void setup() {
        UfsProperties props = new UfsProperties();
        props.setJwtSecret("benchmark-jwt-secret-key-32bytes-ok!");
        props.setJwtTtlSeconds(7200);
        jwtService = new JwtService(props);
        preIssuedToken = jwtService.issue("tenant-001", "operator-001", "app-key-001");
    }

    @Benchmark
    public String issueToken() {
        return jwtService.issue("tenant-001", "operator-001", "app-key-001");
    }

    @Benchmark
    public JwtService.JwtClaims validateToken() {
        return jwtService.parse(preIssuedToken);
    }
}
