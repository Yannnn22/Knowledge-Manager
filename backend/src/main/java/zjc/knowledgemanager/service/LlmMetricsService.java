package zjc.knowledgemanager.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LlmMetricsService {

    private static final Logger log = LoggerFactory.getLogger(LlmMetricsService.class);

    private final MeterRegistry meterRegistry;
    
    private final Map<String, Counter> callCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> durationTimers = new ConcurrentHashMap<>();

    public LlmMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCall(String task, String model, long durationMs, boolean success, String errorType) {
        String counterKey = task + "_" + model.replace("/", "_");
        
        callCounters.computeIfAbsent(counterKey, k -> 
            Counter.builder("llm.call.count")
                    .tag("task", task)
                    .tag("model", model)
                    .register(meterRegistry))
            .increment();

        if (success) {
            successCounters.computeIfAbsent(counterKey, k -> 
                Counter.builder("llm.call.success")
                        .tag("task", task)
                        .tag("model", model)
                        .register(meterRegistry))
                .increment();
        } else {
            errorCounters.computeIfAbsent(counterKey, k -> 
                Counter.builder("llm.call.error")
                        .tag("task", task)
                        .tag("model", model)
                        .tag("error_type", errorType)
                        .register(meterRegistry))
                .increment();
        }

        durationTimers.computeIfAbsent(counterKey, k -> 
            Timer.builder("llm.call.duration")
                    .tag("task", task)
                    .tag("model", model)
                    .register(meterRegistry))
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        log.info("LLM调用统计: task={}, model={}, duration={}ms, success={}", 
                task, model, durationMs, success);
    }

    public void recordMainChat(String model, long durationMs, boolean success, String errorType) {
        recordCall("main_chat", model, durationMs, success, errorType);
    }

    public void recordMemoryExtraction(String model, long durationMs, boolean success, String errorType) {
        recordCall("memory_extraction", model, durationMs, success, errorType);
    }

    public void recordProfileUpdate(String model, long durationMs, boolean success, String errorType) {
        recordCall("profile_update", model, durationMs, success, errorType);
    }

    public void recordSummary(String model, long durationMs, boolean success, String errorType) {
        recordCall("summary_compression", model, durationMs, success, errorType);
    }
}
