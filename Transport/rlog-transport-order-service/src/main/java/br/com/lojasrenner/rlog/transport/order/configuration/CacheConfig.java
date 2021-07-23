package br.com.lojasrenner.rlog.transport.order.configuration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {
	
	@Bean("branchOfficesInMemory")
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("branchOffice", "ecommBranchOffice");
	}

}
