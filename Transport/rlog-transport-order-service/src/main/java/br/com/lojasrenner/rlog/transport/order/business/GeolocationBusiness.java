package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.GeoLocationServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseObjectV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingToResponseV1;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Log4j2
public class GeolocationBusiness {

	@Autowired
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Autowired
	private GeoLocationServiceV1 geolocationService;

	@Autowired
	private EcommBusiness ecommBusiness;

	@Autowired
	private LiveConfig config;

	public List<GeoLocationResponseV1> getBranchesForPickup(String companyId, String channel, String destination) {
		ResponseEntity<List<GeoLocationResponseV1>> geoLocationResponse;
		geoLocationResponse = geolocationService.getClosestStoresInState(companyId, channel, destination);
		return getGeoResponse(geoLocationResponse, companyId);
	}

	public List<GeoLocationResponseV1> getBranchesForShipping(String companyId, String channel, String destination) {
		ResponseEntity<List<GeoLocationResponseV1>> geoLocationResponse;
		geoLocationResponse = geolocationService.getClosestStoresInRange(companyId, channel, destination);
		return getGeoResponse(geoLocationResponse, companyId);
	}

	private List<GeoLocationResponseV1> getGeoResponse(ResponseEntity<List<GeoLocationResponseV1>> geoLocationResponse, String companyId) {
		if (geoLocationResponse == null || !geoLocationResponse.getStatusCode().is2xxSuccessful() || geoLocationResponse.getBody() == null) {
			return new ArrayList<>();
		}

		List<GeoLocationResponseV1> geoResponse = geoLocationResponse.getBody();
		enrichWithBranchData(companyId, geoResponse);

		return geoResponse;
	}

	public List<ShippingGroupResponseV1> getShippingGroups(String companyId, String channel, String destination, QuoteSettings settings, DeliveryOptionsRequest deliveryOptionsRequest) {
		List<ShippingGroupResponseV1> response = new ArrayList<>();

		if (settings.getBranchesForShippingStrategyUsed() == BranchesForShippingStrategyEnum.GEOLOCATION) {
			ResponseEntity<List<GeoLocationResponseV1>> geoLocationResponse;
			geoLocationResponse = geolocationService.getClosestStoresInRange(companyId, channel, destination);

			if (geoLocationResponse != null && geoLocationResponse.getStatusCode().is2xxSuccessful() &&
					geoLocationResponse.getBody() != null) {
				List<GeoLocationResponseV1> geoResponse = geoLocationResponse.getBody();
				List<Integer> branchIntegers = geoResponse.stream().map(g -> Integer.parseInt(g.getBranchOfficeId())).collect(Collectors.toList());

				response.add(ShippingGroupResponseV1.builder()
						.companyId(companyId)
						.name("GEOLOCATION")
						.branches(branchIntegers)
						.build());

				response.add(ShippingGroupResponseV1.builder()
						.companyId(companyId)
						.name("GEOLOCATION-ECOMM")
						.branches(Collections.singletonList(Integer.parseInt(ecommBusiness.getEcommBranchOffice(companyId, channel).getBranchOfficeId())))
						.build());

				deliveryOptionsRequest.setShippingGroupResponseObject(
						ShippingGroupResponseObjectV1.builder()
								.shippingGroupResponse(response)
								.build());
			}
		} else if (settings.getBranchesForShippingStrategyUsed() == BranchesForShippingStrategyEnum.ZIPCODE_RANGE) {
			ResponseEntity<List<ShippingGroupResponseV1>> groupsByZipcode = geolocationService.getGroupsByZipcode(companyId, channel, destination);
			if (groupsByZipcode != null && groupsByZipcode.getStatusCode().is2xxSuccessful() && groupsByZipcode.getBody() != null) {
				String stateKey = "state";
				String cityKey = "city";
				String country = "country";
				response.addAll(groupsByZipcode.getBody());
				if (groupsByZipcode.getHeaders().containsKey(cityKey) && groupsByZipcode.getHeaders().containsKey(stateKey) && groupsByZipcode.getHeaders().containsKey(country))
					deliveryOptionsRequest.setShippingGroupResponseObject(
							ShippingGroupResponseObjectV1.builder()
									.city(Objects.requireNonNull(groupsByZipcode.getHeaders().get(cityKey)).get(0))
									.state(Objects.requireNonNull(groupsByZipcode.getHeaders().get(stateKey)).get(0))
									.country(Objects.requireNonNull(groupsByZipcode.getHeaders().get(country)).get(0))
									.shippingGroupResponse(groupsByZipcode.getBody())
									.build());

			}
		}

		enrichWithBranchDataForGroups(companyId, response);

		return response;
	}

	private void enrichWithBranchDataForGroups(String companyId, List<ShippingGroupResponseV1> groupList) {
		try {
			List<BranchOfficeEntity> branchOffices = branchOfficeService.getBranchOffices(companyId);

			if (branchOffices == null || branchOffices.isEmpty())
				return;

			groupList.forEach(g ->

					g.getBranches().forEach(b -> {
						Optional<BranchOfficeEntity> optionalBranch = branchOffices.stream()
								.filter(a -> b.toString().equals(a.getBranchOfficeId()))
								.findFirst();

						if (optionalBranch.isEmpty())
							return;

						g.getSettings().put(b, getBranchSettings(optionalBranch));
					})

			);
		} catch (Exception e) {
			log.error("Error trying to add branch data to geolocationResponse group. ", e);
		}
	}

	private void enrichWithBranchData(String companyId, List<GeoLocationResponseV1> geoList) {
		try {
			List<BranchOfficeEntity> branchOffices = branchOfficeService.getBranchOffices(companyId);

			if (branchOffices == null || branchOffices.isEmpty())
				return;

			geoList.forEach(b -> {
				Optional<BranchOfficeEntity> optionalBranch = branchOffices.stream().filter(a -> b.getBranchOfficeId().equals(a.getBranchOfficeId())).findFirst();

				if (optionalBranch.isEmpty())
					return;

				b.setSettings(getBranchSettings(optionalBranch));
			});
		} catch (Exception e) {
			log.error("Error trying to add branch data to geolocationResponse. ", e);
		}
	}

	private Map<String, Object> getBranchSettings(Optional<BranchOfficeEntity> optionalBranch) {
		BranchOfficeEntity branch = optionalBranch.orElseGet(BranchOfficeEntity::new);

		Map<String, Object> settings = new HashMap<>();

		settings.put("branchOfficeId", branch.getBranchOfficeId());
		settings.put("branchType", branch.getBranchType());
		settings.put("latitude", branch.getLatitude());
		settings.put("longitude", branch.getLongitude());
		settings.put("zipcode", branch.getZipcode());
		settings.put("state", branch.getState());

		if (branch.getConfiguration() != null) {
			Map<String, Object> configuration = new HashMap<>();
			settings.put("configuration", configuration);
			configuration.put("active", branch.getConfiguration().getActive());
			configuration.put("storeWithdrawalTerm", branch.getConfiguration().getStoreWithdrawalTerm());
			configuration.put("shippingCompanyLocker", branch.getConfiguration().getShippingCompanyLocker());
			configuration.put("branchWithdraw", branch.getConfiguration().getPermission().getBranchWithdraw());
			configuration.put("doShipping", branch.getConfiguration().getPermission().getDoShipping());
			if (branch.getConfiguration().getQuotationZipcode() != null)
				configuration.put("quotationZipcode", branch.getConfiguration().getQuotationZipcode());
		}

		if (branch.getStatus() != null) {
			Map<String, Object> status = new HashMap<>();
			settings.put("status", status);
			status.put("order", branch.getStatus().getOrder());
		}
		return settings;
	}

	public List<GeoLocationResponseV1> getGeolocationResponseForState(String companyId, String channel, String state) {
		List<String> branchesList = new ArrayList<>();
		try {
			ResponseEntity<List<String>> storesByState = geolocationService.getStoresByState(companyId, channel, state);

			if (storesByState != null && storesByState.getStatusCode().is2xxSuccessful() && storesByState.getBody() != null) {
				branchesList.addAll(storesByState.getBody());
			}

		} catch (Exception e) {
			log.error("Error searching branches by state", e);
		}

		List<String> activeBranchOfficesIds = branchOfficeService.getActiveBranchOffices(companyId)
				.stream()
				.map(BranchOfficeEntity::getBranchOfficeId)
				.collect(Collectors.toList());

		return branchesList.stream()
				.filter(activeBranchOfficesIds::contains)
				.map(branchId -> GeoLocationResponseV1.builder()
						.branchOfficeId(branchId)
						.build())
				.collect(Collectors.toList());
	}

	public List<ShippingToResponseV1> getShippingToForBranches(
			String companyId,
			String applicationName,
			List<BranchOfficeEntity> branchs
	) {
		List<ShippingToResponseV1> shippingToStores = new ArrayList<>();
		try {
			ResponseEntity<List<ShippingToResponseV1>> shippingToByBranches = geolocationService.getShippingToForBranches(
					companyId,
					applicationName,
					branchs.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList())
			);
			if (shippingToByBranches != null && shippingToByBranches.getStatusCode().is2xxSuccessful() && shippingToByBranches.getBody() != null)
				shippingToStores.addAll(shippingToByBranches.getBody());

		} catch (Exception e) {
			log.error("Error searching shipping to branches by branches", e);
		}

		return shippingToStores;
	}

}
