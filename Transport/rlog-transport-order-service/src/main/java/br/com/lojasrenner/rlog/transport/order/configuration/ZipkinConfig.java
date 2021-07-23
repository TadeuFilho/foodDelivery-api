package br.com.lojasrenner.rlog.transport.order.configuration;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import brave.handler.FinishedSpanHandler;
import brave.handler.MutableSpan;
import brave.propagation.TraceContext;

@Configuration
public class ZipkinConfig implements EnvironmentAware {

	private Environment environment;
	
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	
	@Bean
	public FinishedSpanHandler customSpanAdjuster() {
		return new FinishedSpanHandler() {
			@Override
			public boolean handle(TraceContext traceContext, MutableSpan span) {
				if (environment != null)
					for (String env : environment.getActiveProfiles())
						span.tag("env", env);
				
				return true;
			}
		};
	}
}