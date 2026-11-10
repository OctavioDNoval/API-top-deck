package org.example.topdeckapi.src.configuracion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /*
    * Small pool with a bounded queue: if mail delivery falls behind (SMTP down,
    * order spam), extra messages are dropped and logged instead of piling up
    * in memory.
    * */
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler((task, pool) ->
                log.error("Cola de mails llena: se descarta un envio"));
        executor.initialize();
        return executor;
    }
}
