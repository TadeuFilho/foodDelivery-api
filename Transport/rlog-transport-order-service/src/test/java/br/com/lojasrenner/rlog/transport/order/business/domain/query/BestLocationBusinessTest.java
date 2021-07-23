package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestLocationBusiness;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ExtraIdentification;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class BestLocationBusinessTest {

    private static final String ANY_SKU = "548860081";

    @InjectMocks
    private BestLocationBusiness bestLocationBusiness;

    @Mock
    private EcommBusiness ecommBusiness;

    @Mock
    private StockBusiness stockBusiness;

    @Mock
    private MetricsService metricsService;

    @Mock
    private LiveConfig config;

    @Before
    public void init(){
        when(ecommBusiness.getEcommBranchOffice("001", "111"))
                .thenReturn(null);
        when(metricsService.combinationTimeoutExceeded(any())).thenReturn(Boolean.FALSE);

        when(stockBusiness.findBestLocation(anyList(), anyList(), anyList(), anyList(), any()))
                .thenReturn(LocationStockV1Response.builder()
                        .companyId("001")
                        .branchOfficeId("35")
                        .branchOfficeStatus("OK")
                        .positionBasedOnPriority(0)
                        .items(Collections.singletonList(LocationStockItemV1Response.builder()
                                .sku("548987123")
                                .amountSaleable(9999)
                                .build()))
                        .okCount(0)
                        .groupIndex(0)
                        .build());

        when(config.getConfigValueInteger(any(), any(), any(), any())).thenReturn(3);
        when(config.getConfigValueString(any(), any(), any(), any())).thenReturn(BranchSortingEnum.COST.toString());

    }

    @Test
    public void findBestLocationGroupingTest() {

        when(ecommBusiness.getEcommBranchOffice("001", "111"))
                .thenReturn(null);

        when(metricsService.combinationTimeoutExceeded(any())).thenReturn(Boolean.FALSE);

        when(stockBusiness.findBestLocation(anyList(), anyList(), anyList(), anyList(), any()))
                .thenReturn(LocationStockV1Response.builder()
                        .companyId("001")
                        .branchOfficeId("889")
                        .branchOfficeStatus("OK")
                        .positionBasedOnPriority(0)
                        .items(Collections.singletonList(LocationStockItemV1Response.builder()
                                .sku(ANY_SKU)
                                .amountSaleable(9999)
                                .build()))
                        .okCount(0)
                        .groupIndex(0)
                        .build());

        DeliveryOptionsRequest deliveryRequest = buildDeliveryOptionsRequest();
        List<GeoLocationResponseV1> geolocationResponse = buildGeolocation();
        List<String> eagerOrigins = null;
        boolean isQuoteFromEcomm = false;
        final List<BranchOfficeEntity> activeBranchOffices = buildActiveBranchOffices();
        final List<LocationStockV1Response> stockResponse = buildStockResponse();
        final List<CartItem> itemList = buildItemList();
        var bestLocationStock = bestLocationBusiness
                .findBestLocationGrouping(deliveryRequest, itemList, activeBranchOffices, stockResponse, geolocationResponse, eagerOrigins, isQuoteFromEcomm, false, false);

        Assert.assertNotNull(bestLocationStock);
        Assert.assertEquals("001", bestLocationStock.getCompanyId());
        Assert.assertEquals("889", bestLocationStock.getBranchOfficeId());
        Assert.assertEquals("OK", bestLocationStock.getBranchOfficeStatus());
        bestLocationStock.getItems().stream()
                .map(LocationStockItemV1Response::getSku)
                .forEach(Assert::assertNotNull);
    }

    @Test
    public void findBestLocationGroupingTestWithPriorityGroup(){

        DeliveryOptionsRequest deliveryRequest = buildDeliveryOptionsRequest();
        List<GeoLocationResponseV1> geolocationResponse = buildGeolocation();
        List<String> eagerOrigins = null;
        final List<BranchOfficeEntity> activeBranchOffices = buildActiveBranchOffices();
        final List<LocationStockV1Response> stockResponse = buildStockResponse();
        final List<CartItem> itemList = Arrays.asList(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build(),
                CartItem.builder()
                        .sku("548987123")
                        .quantity(2)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build());

        LocationStockV1Response bestLocationStock = bestLocationBusiness.findBestLocationGrouping(deliveryRequest, itemList, activeBranchOffices, stockResponse, geolocationResponse, eagerOrigins,true, false);

        Assert.assertNotNull(bestLocationStock);
        Assert.assertEquals("001", bestLocationStock.getCompanyId());
        Assert.assertEquals("35", bestLocationStock.getBranchOfficeId());
        Assert.assertEquals("OK", bestLocationStock.getBranchOfficeStatus());
        bestLocationStock.getItems().stream()
                .map(LocationStockItemV1Response::getSku)
                .forEach(Assert::assertNotNull);
    }

    @Test
    public void findBestLocationGroupingTestWithPriorityGroupAndExtraParameter(){

        DeliveryOptionsRequest deliveryRequest = buildDeliveryOptionsRequest();
        List<GeoLocationResponseV1> geolocationResponse = buildGeolocation();
        List<String> eagerOrigins = null;
        final List<BranchOfficeEntity> activeBranchOffices = buildActiveBranchOffices();
        final List<LocationStockV1Response> stockResponse = buildStockResponse();
        final List<CartItem> itemList = Arrays.asList(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build(),
                CartItem.builder()
                        .sku("548987123")
                        .quantity(2)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build());

        LocationStockV1Response bestLocationStock = bestLocationBusiness.findBestLocationGrouping(deliveryRequest, itemList, activeBranchOffices, stockResponse, geolocationResponse, eagerOrigins,true, true);

        Assert.assertNotNull(bestLocationStock);
        Assert.assertEquals("001", bestLocationStock.getCompanyId());
        Assert.assertEquals("35", bestLocationStock.getBranchOfficeId());
        Assert.assertEquals("OK", bestLocationStock.getBranchOfficeStatus());
        bestLocationStock.getItems().stream()
                .map(LocationStockItemV1Response::getSku)
                .forEach(Assert::assertNotNull);
    }

    private List<BranchOfficeEntity> buildActiveBranchOffices() {
        return Arrays.asList(
                BranchOfficeEntity.builder()
                        .id("001899")
                        .companyId("001")
                        .name("LOJAS RENNER WEB")
                        .zipcode("23575-450")
                        .city("RIO DE JANEIRO")
                        .state("RJ")
                        .country("BR")
                        .latitude("-22.8817943").longitude("-43.6668487")
                        .branchType("WEB_STORE")
                        .build(),
                BranchOfficeEntity.builder()
                        .id("00135")
                        .companyId("001")
                        .name("LOJAS RENNER WEB")
                        .zipcode("23575-450")
                        .city("SÃO PAULO")
                        .state("SP")
                        .country("BR")
                        .latitude("-22.8817943").longitude("-43.6668487")
                        .branchType("WEB_STORE")
                        .build()
                );
    }

    private List<CartItem> buildItemList() {
        return Collections.singletonList(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build()
        );
    }

    private List<LocationStockV1Response> buildStockResponse() {
        return Arrays.asList(
                LocationStockV1Response.builder()
                        .companyId("001")
                        .branchOfficeId("899")
                        .branchOfficeStatus("OK")
                        .items(Collections.singletonList(
                                LocationStockItemV1Response.builder()
                                        .sku(ANY_SKU)
                                        .amountSaleable(9999)
                                        .amountPhysical(9999)
                                        .blocked(false)
                                        .build()
                        ))
                        .positionBasedOnPriority(19)
                        .okCount(1)
                        .okItems(Collections.singletonList(ANY_SKU))
                        .groupIndex(0)
                        .build(),
                LocationStockV1Response.builder()
                        .companyId("001")
                        .branchOfficeId("35")
                        .branchOfficeStatus("OK")
                        .items(List.of(
                                LocationStockItemV1Response.builder()
                                        .sku("548987123")
                                        .amountSaleable(9999)
                                        .amountPhysical(9999)
                                        .blocked(false)
                                        .build()
                        ))
                        .positionBasedOnPriority(19)
                        .okCount(1)
                        .okItems(Collections.singletonList("548987123"))
                        .groupIndex(0)
                        .build());
    }

    private DeliveryOptionsRequest buildDeliveryOptionsRequest() {
        DeliveryOptionsRequest deliveryRequest = new DeliveryOptionsRequest();
        deliveryRequest.setShoppingCart(ShoppingCart.builder()
                .items(Collections.singletonList(CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build()))
                .destination(CartDestination.builder()
                        .zipcode("01311-000")
                        .build())
                .extraIdentification(ExtraIdentification.builder()
                        .extOrderCode("123456")
                        .build())
                .containsRestrictedOriginItems(false)
                .build());
        deliveryRequest.setVerbose(true);
        deliveryRequest.setCompanyId("001");
        deliveryRequest.setXApplicationName("teste-teste");
        deliveryRequest.setLogisticInfo(false);
        deliveryRequest.setStockRequestInput(List.of(Map.of("companyId", "001",
                "activeStoresInRange", Arrays.asList(21, 49, 48, 13, 899, 35, 43, 249, 900),
                "items", Collections.singletonList(ANY_SKU))));
        deliveryRequest.setQuoteSettings(QuoteSettings.builder()
                .maxOriginsConfig(4)
                .maxOriginsStoreConfig(3)
                .maxCombinationsTimeOutConfig(500000)
                .combinationApproachCartSizeLimitConfig(6)
                .branchesForShippingStrategyHeader(BranchesForShippingStrategyEnum.GEOLOCATION)
                .branchesForShippingStrategyConfig(BranchesForShippingStrategyEnum.ZIPCODE_RANGE)
                .maxOriginsUsed(4)
                .maxOriginsStoreUsed(3)
                .maxCombinationsTimeOutUsed(500000)
                .combinationApproachCartSizeLimitUsed(6)
                .branchesForShippingStrategyUsed(BranchesForShippingStrategyEnum.GEOLOCATION)
                .build());
        return deliveryRequest;
    }

    private List<GeoLocationResponseV1> buildGeolocation() {
        return Stream.of(Pair.of(899, 13), Pair.of(35, 10))
                .map(item -> GeoLocationResponseV1.builder()
                        .branchOfficeId(item.getFirst().toString())
                        .distance(item.getSecond())
                        .build())
                .collect(Collectors.toList());
    }

}
