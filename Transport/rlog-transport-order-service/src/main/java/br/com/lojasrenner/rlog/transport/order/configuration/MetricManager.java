package br.com.lojasrenner.rlog.transport.order.configuration;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class MetricManager {
	@Autowired
	private MeterRegistry registry;

	private static Map<Meter.Id, LocalDateTime> metricsChangesTimestampMap = new ConcurrentHashMap<>();

	@Value("${bfl.metric.inertTimeLimit:3}")
	private Long inertTimeLimitInMinutes;

	@Scheduled(fixedRateString = "${bfl.metric.ttl.validate:10000}",
			initialDelayString = "${bfl.metric.ttl.validate:10000}")
	public void validateMeters() {
		List<Meter> meters = registry.getMeters().stream().filter(i -> i.getId().getName().contains("bfl_")).collect(Collectors.toList());
		List<Meter> metersToRemove = new ArrayList<>();
		meters.forEach((i) -> {
			Double value = getMeasurementForEvaluation(i);
			if(value != 0)
				metricsChangesTimestampMap.put(i.getId(), LocalDateTime.now());
			else {
				if(metricsChangesTimestampMap.containsKey(i.getId())) {
					Long timeDiffInMinutes = ChronoUnit.MINUTES.between(metricsChangesTimestampMap.get(i.getId()), LocalDateTime.now());
					if(timeDiffInMinutes > inertTimeLimitInMinutes) {
						metersToRemove.add(i);
					}
				}
			}

		});

		metersToRemove.forEach(m -> registry.remove(m));
	}

	private Double getMeasurementForEvaluation(Meter meter) {
		Meter.Type type = meter.getId().getType();
		if(type.equals(Meter.Type.DISTRIBUTION_SUMMARY))
			for(Measurement measure : meter.measure()) {
				if(measure.getStatistic().equals(Statistic.COUNT))
					return measure.getValue();
			}

		return meter.measure().iterator().next().getValue();
	}

}
