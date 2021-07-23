package br.com.lojasrenner.rlog.transport.order.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorManager {

	@Bean
	public ExecutorService getExecutor(@Value("${bfl.business.threadPoolSize:100}") Integer threadPoolSize) {
		return Executors.newFixedThreadPool(threadPoolSize);
	}
}
