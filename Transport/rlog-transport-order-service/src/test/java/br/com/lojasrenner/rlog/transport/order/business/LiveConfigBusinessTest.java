package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.LiveConfigBusiness;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.UnavailableSkuStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.LiveConfigDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LiveConfigRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LiveConfigResponseV1;
import br.com.lojasrenner.exception.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class LiveConfigBusinessTest {

    @Mock
    private LiveConfigDBInfrastructure dbLiveConfig;

    @InjectMocks
    private LiveConfigBusiness business;

    private static final List<String> COMPANIES = Arrays.asList("001", "011");

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
                            .pickupActive(true)
                            .reQuoteActive(false)
                            .addOperationalTimeOnEstimate(true)
                            .build())
                    .timeout(TimeoutConfig.builder()
                            .threads(3500)
                            .combinations(17500)
                            .build())
                    .fulfillment(FulfillmentConfig.builder()
                            .validBranchOfficeStatus(Collections.singletonList("MAXIMUM_CAPACITY_ACHIEVED"))
                            .autoReQuote(Boolean.TRUE)
                            .build())
                    .freightCostCurrency("R$")
                    .defaultQuotingMode("DYNAMIC_BOX_ALL_ITEMS")
                    .build())
            .build();

    LiveConfigEntity entity = LiveConfigEntity.builder()
            .id(COMPANIES.get(0))
            .business(CompanyConfigEntity.builder()
                    .maxOrigins(4)
                    .maxOriginsStore(3)
                    .branchSorting(BranchSortingEnum.COST)
                    .unavailableSkusStrategy(UnavailableSkuStrategyEnum.RETRY_MODE)
                    .allowFulfillWithMaxCapacityAchieved(true)
                    .branchesForShippingStrategy(BranchesForShippingStrategyEnum.GEOLOCATION)
                    .eagerBranches(Arrays.asList("899", "35"))
                    .combinationApproachCartSizeLimit(6)
                    .dataLakeSendDataRate(1.0)
                    .shippingTo(ShippingToConfig.builder()
                            .pickupActive(true)
                            .reQuoteActive(true)
                            .addOperationalTimeOnEstimate(false)
                            .build())
                    .timeout(TimeoutConfig.builder()
                            .threads(75000)
                            .combinations(56000)
                            .build())
                    .fulfillment(FulfillmentConfig.builder()
                            .validBranchOfficeStatus(Collections.singletonList("MAXIMUM_CAPACITY_ACHIEVED"))
                            .autoReQuote(Boolean.TRUE)
                            .build())
                    .freightCostCurrency("$")
                    .defaultQuotingMode("DYNAMIC_BOX_ALL_ITEMS")
                    .build())
            .build();

    @Test
    public void save_settings_to_mongo() {
        when(dbLiveConfig.findById(eq(COMPANIES.get(1)))).thenReturn(Optional.empty());
        LiveConfigResponseV1 response = business.postLiveConfig(COMPANIES.get(1), request, Optional.empty());

        validateInformationResponse("Configuration data entered successfully", response);
    }

    @Test
    public void updates_the_settings_on_the_mongo() {
        when(dbLiveConfig.findById(eq(COMPANIES.get(0)))).thenReturn(Optional.of(entity));
        LiveConfigResponseV1 response = business.postLiveConfig(COMPANIES.get(0), request, Optional.empty());

        validateInformationResponse("Configuration data changed successfully", response);
    }

    private void validateInformationResponse(String message, LiveConfigResponseV1 response) {
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertEquals(message, response.getMessage());

        assertNotNull(response.getConfig());

        LiveConfigRequestV1 config = response.getConfig();
        assertNotNull(config.getBusiness());
        assertEquals(request.getBusiness().getMaxOrigins(), config.getBusiness().getMaxOrigins());
        assertEquals(request.getBusiness().getMaxOriginsStore(), config.getBusiness().getMaxOriginsStore());
        assertEquals(request.getBusiness().getUnavailableSkusStrategy(), config.getBusiness().getUnavailableSkusStrategy());
        assertEquals(request.getBusiness().getBranchesForShippingStrategy(), config.getBusiness().getBranchesForShippingStrategy());

        ShippingToConfig shippingToConfig = request.getBusiness().getShippingTo();
        assertNotNull(shippingToConfig);

        TimeoutConfig timeoutConfig = request.getBusiness().getTimeout();
        assertNotNull(timeoutConfig);

        assertEquals(request.getBusiness().getFulfillment().getValidBranchOfficeStatus().size(), config.getBusiness().getFulfillment().getValidBranchOfficeStatus().size());
        assertEquals(request.getBusiness().getFreightCostCurrency(), config.getBusiness().getFreightCostCurrency());
        assertEquals(request.getBusiness().getDefaultQuotingMode(), config.getBusiness().getDefaultQuotingMode());
    }

    @Test
    public void fetches_configuration_data_on_the_mongo() {
        when(dbLiveConfig.findById(eq(COMPANIES.get(0)))).thenReturn(Optional.of(entity));
        LiveConfigEntity response = business.getLiveConfig(COMPANIES.get(0), Optional.empty());

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(response.getId(), COMPANIES.get(0));
        assertNotNull(response.getBusiness());

        CompanyConfigEntity config = response.getBusiness();
        assertEquals(entity.getBusiness().getMaxOrigins(), config.getMaxOrigins());
        assertEquals(entity.getBusiness().getMaxOriginsStore(), config.getMaxOriginsStore());
        assertEquals(entity.getBusiness().getUnavailableSkusStrategy(), config.getUnavailableSkusStrategy());
        assertEquals(entity.getBusiness().getBranchesForShippingStrategy(), config.getBranchesForShippingStrategy());

        ShippingToConfig shippingToConfig = request.getBusiness().getShippingTo();
        assertNotNull(shippingToConfig);

        TimeoutConfig timeoutConfig = request.getBusiness().getTimeout();
        assertNotNull(timeoutConfig);

        assertEquals(request.getBusiness().getFulfillment().getValidBranchOfficeStatus().size(), config.getFulfillment().getValidBranchOfficeStatus().size());
        assertEquals(request.getBusiness().getDefaultQuotingMode(), config.getDefaultQuotingMode());
    }

    @Test(expected = NotFoundException.class)
    public void fetches_configuration_data_on_the_mongo_with_not_found_exception() {
        when(dbLiveConfig.findById(eq(COMPANIES.get(1)))).thenReturn(Optional.empty());
        LiveConfigEntity response = business.getLiveConfig(COMPANIES.get(1), Optional.empty());
        assertNull(response);
    }
}
