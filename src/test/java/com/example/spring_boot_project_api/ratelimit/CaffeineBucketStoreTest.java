package com.example.spring_boot_project_api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaffeineBucketStoreTest {

    private CaffeineBucketStore store;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getStore().setMaxBuckets(1000);
        store = new CaffeineBucketStore(properties);
    }

    @Test
    void allowsExactlyCapacityThenDenies() {
        assertThat(store.tryConsume("k1", 3, Duration.ofMinutes(5)).allowed()).isTrue();
        assertThat(store.tryConsume("k1", 3, Duration.ofMinutes(5)).allowed()).isTrue();
        assertThat(store.tryConsume("k1", 3, Duration.ofMinutes(5)).allowed()).isTrue();
        assertThat(store.tryConsume("k1", 3, Duration.ofMinutes(5)).allowed()).isFalse();
    }

    @Test
    void deniedResultCarriesPositiveRetryAfter() {
        store.tryConsume("k2", 1, Duration.ofMinutes(5));
        BucketResult denied = store.tryConsume("k2", 1, Duration.ofMinutes(5));
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfter()).isNotNull();
        assertThat(denied.retryAfter()).isPositive();
    }

    @Test
    void keysAreIndependent() {
        store.tryConsume("a", 1, Duration.ofMinutes(5));
        assertThat(store.tryConsume("a", 1, Duration.ofMinutes(5)).allowed()).isFalse();
        assertThat(store.tryConsume("b", 1, Duration.ofMinutes(5)).allowed()).isTrue();
    }

    @Test
    void refillRestoresAllowancesOverTime() throws Exception {
        java.time.Duration window = java.time.Duration.ofMillis(300);
        store.tryConsume("k4", 1, window);
        assertThat(store.tryConsume("k4", 1, window).allowed()).isFalse();
        Thread.sleep(400);
        assertThat(store.tryConsume("k4", 1, window).allowed())
                .as("bucket should refill once its window elapses")
                .isTrue();
    }

    @Test
    void concurrentConsumersNeverExceedCapacity() throws Exception {
        int capacity = 50;
        int workers = 100;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                tasks.add(() -> store.tryConsume("concurrent", capacity, Duration.ofMinutes(5)).allowed());
            }
            List<Future<Boolean>> futures = pool.invokeAll(tasks);
            long allowed = futures.stream().filter(this::uncheckedGet).count();
            assertThat(allowed)
                    .as("never more than capacity, but every token is usable under concurrency")
                    .isEqualTo(capacity);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> store.tryConsume("x", 0, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.tryConsume("x", -1, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidRefillPeriod() {
        assertThatThrownBy(() -> store.tryConsume("x", 1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.tryConsume("x", 1, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.tryConsume("x", 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private boolean uncheckedGet(Future<Boolean> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}