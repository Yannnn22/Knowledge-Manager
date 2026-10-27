package zjc.knowledgemanager.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式ID生成器（简化版雪花算法）
 */
@Component
public class IdGenerator {
    
    private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00 UTC
    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    
    private final long workerId;
    private final AtomicLong sequence = new AtomicLong(0L);
    private long lastTimestamp = -1L;
    
    public IdGenerator() {
        // 简化实现，使用固定workerId
        this.workerId = 1L;
    }
    
    public IdGenerator(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID超出范围");
        }
        this.workerId = workerId;
    }
    
    /**
     * 生成下一个ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }
        
        if (lastTimestamp == timestamp) {
            long sequence = this.sequence.incrementAndGet() & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            this.sequence.set(0L);
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT) |
                (workerId << WORKER_ID_SHIFT) |
                this.sequence.get();
    }
    
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    private long timeGen() {
        return Instant.now().toEpochMilli();
    }
} 