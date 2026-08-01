package com.raul.ecommercehub.worker.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SyncMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSuccess(long durationMs) {
        meterRegistry.counter("sync_batchitem_total", "status", "success").increment();
        meterRegistry.timer("sync_batchitem_duration_seconds", "status", "success")
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordFailure(long durationMs) {
        meterRegistry.counter("sync_batchitem_total", "status", "failure").increment();
        meterRegistry.timer("sync_batchitem_duration_seconds", "status", "failure")
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDeadLetter() {
        meterRegistry.counter("sync_batchitem_total", "status", "dead_letter").increment();
    }
}