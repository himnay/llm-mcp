package com.org.ai.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    @DisplayName("propagate() carries the acting user onto an executor thread and cleans up after")
    void propagatesToWorkerThread() throws Exception {
        RequestContext.set("jane.doe", "default", false);
        Callable<String> task = RequestContext.propagate(RequestContext::user);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertThat(executor.submit(task).get()).isEqualTo("jane.doe");
            assertThat(executor.submit(RequestContext::user).get())
                    .as("a plain task still sees nothing — the thread-local is not inherited").isNull();
        }
    }
}
