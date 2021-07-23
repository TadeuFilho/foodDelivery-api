package br.com.lojasrenner.rlog.transport.order.configuration;

import br.com.lojasrenner.rlog.transport.order.business.exception.CompanyConfigNotFoundException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.LiveConfigDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.LiveConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class LiveConfig {

	@Autowired
	private LiveConfigDBInfrastructure db;

	private Map<String, LiveConfigEntity> map;

	@PostConstruct
	@Scheduled(fixedRateString = "${bfl.loader.ttl.live-config:10000}",
			initialDelayString = "${bfl.loader.ttl.live-config:10000}")
	public void updateConfigMap() {
		Map<String, LiveConfigEntity> newMap = new ConcurrentHashMap<>();
		Optional<Map<String, LiveConfigEntity>> allCompanyOptionalMap = Optional.ofNullable(db.findAll());
		allCompanyOptionalMap.ifPresent(liveConfigEntity -> newMap.putAll(allCompanyOptionalMap.get()));
		map = newMap;
	}

	public CompanyConfigEntity getCompanyConfiguration(String companyId) {
		return Objects.nonNull(map.get(companyId)) ? map.get(companyId).getBusiness() : null;
	}

	public Object getCompanyConfigValue(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func) {
		CompanyConfigEntity specificCompany = getCompanyConfiguration(companyId);
		CompanyConfigEntity defaultCompany = getCompanyConfiguration("default");

		CompanyConfigEntity channelCompanyOverride = null;
		if(Objects.nonNull(specificCompany) && specificCompany.getChannelConfig() != null && channel.isPresent()) {
			channelCompanyOverride = specificCompany.getChannelConfig().get(channel.get());
		}

		Object specificValue = null;
		Object defaultValue = null;
		Object overrideValue = null;

		try {
			defaultValue = func.apply(defaultCompany);
		} catch (Exception ignored) {
			// Ignore
		}

		try {
			specificValue = func.apply(specificCompany);
		} catch (Exception ignored) {
			// Ignore
		}

		try {
			overrideValue = func.apply(channelCompanyOverride);
		} catch (Exception ignored) {
			// Ignore
		}

		if (overrideValue != null) {
			return overrideValue;
		}
		else if (specificValue != null)
			return specificValue;

		return defaultValue;
	}

	public String getConfigValueString(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func, Boolean isRequired) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, isRequired);
		return value == null ? null : value.toString();
	}

	public Integer getConfigValueInteger(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func, Boolean isRequired) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, isRequired);
		return value == null ? null : Integer.parseInt(value.toString());
	}

	public Double getConfigValueDouble(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func, Boolean isRequired) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, isRequired);
		return value == null ? null : Double.parseDouble(value.toString());
	}

	public List<String> getConfigValueAsListOfString(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func, Boolean isRequired) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, isRequired);
		return value == null ? null : ((List<Object>) value).stream().map(Object::toString).collect(Collectors.toList());
	}

	public Map<String, List<String>> getConfigMetricsMap(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func, Boolean isRequired) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, isRequired);
		return value == null ? null : ((Map<String, List<String>>) value);
	}

	public Boolean getConfigValueBoolean(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func, Boolean isRequired) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, isRequired);
		String finalValue = value == null ? null : value.toString();
		return Boolean.valueOf(finalValue);
	}

	public List<BranchOfficeEntity> getConfigEcomms(String companyId, Optional<String> channel, Function<CompanyConfigEntity, Object> func) {
		Object value = getCompanyConfigValue(companyId, channel, func);
		checkIfObjectIsRequired(value, true);
		return (List<BranchOfficeEntity>) value;
	}

	public void checkIfObjectIsRequired(Object value, Boolean isRequired) {
		if(isRequired.booleanValue() && Objects.isNull(value))
			throw new CompanyConfigNotFoundException("A required field as not found on application");
	}
}
