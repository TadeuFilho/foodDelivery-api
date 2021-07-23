package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.LiveConfigDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LiveConfigRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LiveConfigResponseV1;
import br.com.lojasrenner.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

@Service
@Log4j2
public class LiveConfigBusiness {

	@Autowired
	LiveConfigDBInfrastructure dbLiveConfig;

	public LiveConfigResponseV1 postLiveConfig(String companyId, LiveConfigRequestV1 request, Optional<String> channel) {
		LiveConfigResponseV1 response = new LiveConfigResponseV1();

		try {
			Optional<LiveConfigEntity> optionalLiveConfigEntity = dbLiveConfig.findById(companyId);
			if (optionalLiveConfigEntity.isPresent()) {
				LiveConfigEntity liveConfigEntity = optionalLiveConfigEntity.get();
				if (channel.isPresent() && StringUtils.isNotBlank(channel.get()) && Objects.nonNull(request.getBusiness()))
					updateChannelLiveConfig(request, channel.get().trim(), liveConfigEntity);
				else
					updateCompanyLiveConfigEntity(request, liveConfigEntity);

				dbLiveConfig.save(liveConfigEntity);
				response.setMessage("Configuration data changed successfully");
			} else {
				dbLiveConfig.save(requestToLiveConfigEntity(companyId, request, channel));
				response.setMessage("Configuration data entered successfully");
			}
		} catch (Exception ex) {
			response.setMessage("Error saving configuration data - " + ex.getMessage());
		} finally {
			response.setConfig(request);
		}

		return response;
	}

	public LiveConfigResponseV1 putLiveConfig(String companyId, Map<String, Object> request) {
		LiveConfigResponseV1 response = new LiveConfigResponseV1();

		try {
			Optional<LiveConfigEntity> liveConfigEntity = dbLiveConfig.findById(companyId);
			if (liveConfigEntity.isPresent()) {
				updateFields(liveConfigEntity.get(), request);
				dbLiveConfig.save(liveConfigEntity.get());
				response.setMessage("Configuration data changed successfully");
			} else {
				response.setMessage("Error updating configuration data - this company does not exist");
			}
		} catch (Exception ex) {
			response.setMessage("Error saving configuration data - " + ex.getMessage());
		} finally {
			ObjectMapper mapper = new ObjectMapper();
			response.setConfig(mapper.convertValue(request, LiveConfigRequestV1.class));
		}

		return response;
	}

	private void updateFields(LiveConfigEntity local, Map<String, Object> remote) {
		Object obj = remote.get("business");
		dealWithObjectFields(local, null, obj, "business", local.getBusiness());
	}

	private <T> void dealWithObjectFields(LiveConfigEntity local, Class<?> clazz, Object obj, String declaredField, T fieldToUpdate) {
		if (clazz == null)
			clazz = local.getClass();

		Class<?> finalClazz = clazz;

		((LinkedHashMap) obj).forEach((key, value) -> {
			try {
				Field field = ((Class) finalClazz.getDeclaredField(declaredField).getGenericType()).getDeclaredField(key.toString());
				field.setAccessible(true);

				if (field.getGenericType() instanceof Class && ((Class) field.getGenericType()).isEnum())
					field.set(fieldToUpdate, Enum.valueOf((Class) field.getGenericType(), value.toString()));
				else if (field.getGenericType() instanceof Class && ((Class) field.getGenericType()).isAssignableFrom(FulfillmentConfig.class)) {
					if (local.getBusiness().getFulfillment() == null)
						local.getBusiness().setFulfillment(FulfillmentConfig.builder().build());
					dealWithObjectFields(local, field.getDeclaringClass(), value, "fulfillment", local.getBusiness().getFulfillment());
				} else if (field.getGenericType() instanceof Class && ((Class) field.getGenericType()).isAssignableFrom(ReOrderConfig.class)) {
					if (local.getBusiness().getReOrder() == null)
						local.getBusiness().setReOrder(ReOrderConfig.builder().build());
					dealWithObjectFields(local, field.getDeclaringClass(), value, "reOrder", local.getBusiness().getReOrder());
				} else if (field.getGenericType() instanceof Class && ((Class) field.getGenericType()).isAssignableFrom(ShippingToConfig.class)) {
					if (local.getBusiness().getShippingTo() == null)
						local.getBusiness().setShippingTo(ShippingToConfig.builder().build());
					dealWithObjectFields(local, field.getDeclaringClass(), value, "shippingTo", local.getBusiness().getShippingTo());
				} else if (field.getGenericType() instanceof Class && ((Class) field.getGenericType()).isAssignableFrom(TimeoutConfig.class)) {
					if (local.getBusiness().getTimeout() == null)
						local.getBusiness().setTimeout(TimeoutConfig.builder().build());
					dealWithObjectFields(local, field.getDeclaringClass(), value, "timeout", local.getBusiness().getTimeout());
				} else if (key.equals("ecomms")) {
					List<BranchOfficeEntity> branchs = new ArrayList<>();
					if (!((ArrayList) value).isEmpty())
						((ArrayList) value).forEach(branch ->
							branchs.add(BranchOfficeEntity.builder()
									.id(((LinkedHashMap) branch).get("id").toString())
									.zipcode(((LinkedHashMap) branch).get("zipcode").toString())
									.build()));
					field.set(fieldToUpdate, branchs);
				} else {
					field.set(fieldToUpdate, value);
				}

			} catch (NoSuchFieldException | IllegalAccessException e) {
				log.error("Error merging fields when updating live config settings: ", e);
			}
		});
	}

	private LiveConfigEntity requestToLiveConfigEntity(String companyId, LiveConfigRequestV1 request, Optional<String> channel) {
		CompanyConfigEntity config;
		if (channel.isPresent())
			config = CompanyConfigEntity.builder().channelConfig(Map.of(channel.get(), request.getBusiness())).build();
		else
			config = request.getBusiness();

		return LiveConfigEntity.builder()
				.id(companyId)
				.business(config)
				.build();
	}

	private void updateChannelLiveConfig(LiveConfigRequestV1 request, String channel, LiveConfigEntity liveConfigEntity) {
		if (Objects.nonNull(liveConfigEntity.getBusiness().getChannelConfig()))
			liveConfigEntity.getBusiness().getChannelConfig().put(channel, request.getBusiness());
		else
			liveConfigEntity.getBusiness().setChannelConfig(Map.of(channel, request.getBusiness()));
	}

	private void updateCompanyLiveConfigEntity(LiveConfigRequestV1 request, LiveConfigEntity liveConfigEntity) {
		if (Objects.nonNull(request.getBusiness()))
			liveConfigEntity.setBusiness(request.getBusiness());
	}

	public LiveConfigEntity getLiveConfig(String companyId, Optional<String> channel) {
		Optional<LiveConfigEntity> liveConfigEntity;

		if (channel.isPresent() && StringUtils.isNotBlank(channel.get())) {
			liveConfigEntity = dbLiveConfig.findByIdAndChannel(companyId, channel.get());
		} else {
			liveConfigEntity = dbLiveConfig.findById(companyId);
		}

		if (liveConfigEntity.isEmpty())
			throw new NotFoundException("Configuration not found. Please register the configuration to consult", "404");

		return liveConfigEntity.get();
	}
}
