package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.business.LiveConfigBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.AuthorizationTokenException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.UnavailableSkuStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LiveConfigRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LiveConfigResponseV1;
import br.com.lojasrenner.exception.BadRequestException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.LiveConfigController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class LiveConfigControllerTest {

	@Mock
	private LiveConfigBusiness business;

	@InjectMocks
	private LiveConfigController controller;

	private static final String COMPANY_ID = "001";
	private static final String AUTHORIZATION = "58d6b06b748ad6b61a238a9b6320dfa37ffae7e5";
	private static final String MESSAGE = "Objeto Mockado";

	LiveConfigRequestV1 request = LiveConfigRequestV1.builder()
			.business(CompanyConfigEntity.builder()
					.maxOrigins(4)
					.maxOriginsStore(3)
					.branchSorting(BranchSortingEnum.COST)
					.unavailableSkusStrategy(UnavailableSkuStrategyEnum.UNAVAILABLE_MODE)
					.allowFulfillWithMaxCapacityAchieved(true)
					.branchesForShippingStrategy(BranchesForShippingStrategyEnum.ZIPCODE_RANGE)
					.eagerBranches(Collections.singletonList("899"))
					.combinationApproachCartSizeLimit(6)
					.dataLakeSendDataRate(1.0)
					.shippingTo(ShippingToConfig.builder()
							.pickupActive(false)
							.addOperationalTimeOnEstimate(true)
							.build())
					.timeout(TimeoutConfig.builder()
							.threads(3500)
							.combinations(17500)
							.build())
					.fulfillment(FulfillmentConfig.builder()
							.validBranchOfficeStatus(Collections.singletonList("MAXIMUM_CAPACITY_ACHIEVED"))
							.build())
					.freightCostCurrency("R$")
					.defaultQuotingMode("DYNAMIC_BOX_ALL_ITEMS")
					.build())
			.build();

	LiveConfigEntity response = LiveConfigEntity.builder()
			.id(COMPANY_ID)
			.business(request.getBusiness())
			.build();

	@Test
	public void sending_settings_to_the_mongo() {
		ReflectionTestUtils.setField(controller, "token", AUTHORIZATION);
		when(business.postLiveConfig(eq(COMPANY_ID), eq(request), any())).thenReturn(LiveConfigResponseV1.builder()
				.message(MESSAGE)
				.build());

		ResponseEntity<LiveConfigResponseV1> responseEntity = controller.postLiveConfig(AUTHORIZATION, COMPANY_ID, null, request);

		assertNotNull(responseEntity);
		assertNotNull(responseEntity.getBody());
		assertEquals(MESSAGE, responseEntity.getBody().getMessage());
	}

	@Test(expected = AuthorizationTokenException.class)
	public void sending_settings_to_the_mongo_with_error_token() {
		ReflectionTestUtils.setField(controller, "token", "46d6b06b748ad6b61a238a9b6320dfa37ffae5e4");
		when(business.postLiveConfig(eq(COMPANY_ID), eq(request), any())).thenThrow(AuthorizationTokenException.class);
		controller.postLiveConfig(AUTHORIZATION, COMPANY_ID, "", request);
	}

	@Test(expected = BadRequestException.class)
	public void sending_settings_to_the_mongo_with_error_params() {
		ReflectionTestUtils.setField(controller, "token", AUTHORIZATION);
		request.setBusiness(null);
		when(business.postLiveConfig(eq(COMPANY_ID), eq(request), any())).thenThrow(BadRequestException.class);
		controller.postLiveConfig(AUTHORIZATION, COMPANY_ID, "", request);
	}

	@Test
	public void retrieves_configuration_data_from_the_mongo() {
		ReflectionTestUtils.setField(controller, "token", AUTHORIZATION);
		when(business.getLiveConfig(eq(COMPANY_ID), any())).thenReturn(response);

		ResponseEntity<LiveConfigEntity> responseEntity = controller.getLiveConfig(AUTHORIZATION, COMPANY_ID, null);

		assertNotNull(responseEntity);
		assertNotNull(responseEntity.getBody());
		assertNotNull(responseEntity.getBody().getBusiness());
	}
}
