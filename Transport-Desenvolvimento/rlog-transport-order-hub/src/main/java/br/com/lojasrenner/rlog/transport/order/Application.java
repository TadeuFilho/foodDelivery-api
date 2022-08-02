package br.com.lojasrenner.rlog.transport.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableRetry
@EnableScheduling
@EnableFeignClients
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class
})
@ComponentScan("br.com.lojasrenner")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
