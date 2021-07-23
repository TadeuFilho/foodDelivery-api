package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestLocationBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestSolutionBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.SplitShoppingCartBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.model.FindBestSolution;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ExtraIdentification;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class BestSolutionBusinessTest {

    @InjectMocks
    private BestSolutionBusiness bestSolutionBusiness;

    @Mock
    private MetricsService metricsService;

    @Mock
    private EcommBusiness ecommBusiness;

    @Mock
    private LiveConfig config;

    @Mock
    private StockBusiness stockBusiness;

    @Mock
    private BestLocationBusiness bestLocationBusiness;

    @Mock
    private SplitShoppingCartBusiness splitShoppingCartBusiness;

    private static final String ANY_SKU = "548860081";
    private static final String ANY_SKU_PLUS_1 = "5488600811";
    private static final String COMPANY_ID_RENNER = "001";

    @Test
    public void findBestSolutionTest() {
        when(bestLocationBusiness.findBestLocationGrouping(any(), any(), any(), any(), any(), eq(null), eq(false), eq(false))).thenReturn(
                LocationStockV1Response.builder()
                        .companyId(COMPANY_ID_RENNER)
                        .branchOfficeId("889")
                        .branchOfficeStatus("OK")
                        .items(Collections.singletonList(
                                LocationStockItemV1Response.builder()
                                        .sku(ANY_SKU)
                                        .amountSaleable(9999)
                                        .amountPhysical(9999)
                                        .build()
                        )).build()
        );
        Map<String, List<CartItem>> itemListMap = new ConcurrentHashMap<>();
        itemListMap.put("899", Collections.singletonList(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .productType(ProductTypeEnum.DEFAULT)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .build()
        ));
        when(splitShoppingCartBusiness.splitShoppingCartByOrigin(any(), any(), any(), any())).thenReturn(itemListMap);

        final var toFindBestSolution = FindBestSolution.builder()
                .geolocationResponse(buildGeolocationResponse())
                .itemsList(buildItemList())
                .stockResponseFiltered(buildStockResponseFiltered())
                .eagerBranches(Collections.emptyList())
                .skuQuantityMap(buildSkuQuantityMap())
                .activeBranchOffices(buildActiveBranchOffices())
                .build();
        DeliveryOptionsRequest deliveryRequest = buildDeliveryOptionsRequest();


        var bestSolution = bestSolutionBusiness.findBestSolution(deliveryRequest, toFindBestSolution);

        Assert.assertTrue(bestSolution.containsKey("899"));
        Assert.assertTrue(bestSolution.get("899")
                .stream()
                .map(CartItem::getSku)
                .allMatch(ANY_SKU::equals));
    }

    @Test
    public void testBestSolutionGroupingWithPriorityGroup() {
        when(bestLocationBusiness.findBestLocationGrouping(any(), any(), any(), any(), any(), eq(null), eq(true), eq(true))).thenReturn(
                LocationStockV1Response.builder()
                        .companyId(COMPANY_ID_RENNER)
                        .branchOfficeId("889")
                        .branchOfficeStatus("OK")
                        .items(Collections.singletonList(
                                LocationStockItemV1Response.builder()
                                        .sku(ANY_SKU)
                                        .amountSaleable(9999)
                                        .amountPhysical(9999)
                                        .build()
                        )).build()
        );

        Map<String, List<CartItem>> itemListMap = new ConcurrentHashMap<>();
        itemListMap.put("899", Collections.singletonList(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .productType(ProductTypeEnum.DEFAULT)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .build()
        ));
        when(splitShoppingCartBusiness.splitShoppingCartByOrigin(any(), any(), any(), any())).thenReturn(itemListMap);


        when(config.getConfigValueBoolean(any(), any(), any(), any())).thenReturn(true);
        final var toFindBestSolution = FindBestSolution.builder()
                .geolocationResponse(buildGeolocationResponse())
                .itemsList(buildItemList())
                .stockResponseFiltered(buildStockResponseFiltered())
                .eagerBranches(Collections.emptyList())
                .skuQuantityMap(buildSkuQuantityMap())
                .activeBranchOffices(buildActiveBranchOffices())
                .build();
        DeliveryOptionsRequest deliveryRequest = buildDeliveryOptionsRequest();

        var bestSolution = bestSolutionBusiness.findBestSolution(deliveryRequest, toFindBestSolution);

        Assert.assertTrue(bestSolution.containsKey("899"));
        Assert.assertTrue(bestSolution.get("899")
                .stream()
                .map(CartItem::getSku)
                .allMatch(ANY_SKU::equals));
    }

    @Test
    public void findBestSolutionSkuQuantityApproachTest() {

        when(ecommBusiness.getEcommBranchOffice(eq(COMPANY_ID_RENNER), any()))
                .thenReturn(BranchOfficeEntity.builder()
                        .id("000111")
                        .build());

        final var toFindBestSolution = FindBestSolution.builder()
                .geolocationResponse(buildGeolocationResponse())
                .itemsList(buildSkuQuantityApproachItemList())
                .stockResponseFiltered(buildStockResponseFilteredWhenSkuQuantityApproach())
                .eagerBranches(Collections.emptyList())
                .skuQuantityMap(buildSkuQuantityMap())
                .activeBranchOffices(buildActiveBranchOffices())
                .build();
        DeliveryOptionsRequest deliveryRequest = buildDeliveryOptionsRequest();

        //configurando quoteSettings
        QuoteSettings quoteSettings = deliveryRequest.getQuoteSettings();
        quoteSettings.setCombinationApproachCartSizeLimitConfig(2);
        deliveryRequest.setQuoteSettings(quoteSettings);

        //configurando
        ShoppingCart shoppingCart = deliveryRequest.getShoppingCart();
        shoppingCart.setItems(buildSkuQuantityApproachItemList());
        deliveryRequest.setShoppingCart(shoppingCart);

        var bestSolution = bestSolutionBusiness.findBestSolution(deliveryRequest, toFindBestSolution);

        Assert.assertTrue(bestSolution.containsKey("899"));
        Assert.assertTrue(bestSolution.get("899")
                .stream()
                .map(CartItem::getSku)
                .allMatch(ANY_SKU::equals));
    }

    private List<BranchOfficeEntity> buildActiveBranchOffices() {
        return Collections.singletonList(BranchOfficeEntity.builder()
                .id("001899")
                .companyId(COMPANY_ID_RENNER)
                .name("LOJAS RENNER WEB")
                .zipcode("23575-450")
                .city("RIO DE JANEIRO")
                .state("RJ")
                .country("BR")
                .latitude("-22.8817943").longitude("-43.6668487")
                .branchType("WEB_STORE")
                .build());
    }

    private Map<String, Integer> buildSkuQuantityMap() {
        return Map.of(ANY_SKU, 1);
    }

    private List<LocationStockV1Response> buildStockResponseFiltered() {
        return Collections.singletonList(
                LocationStockV1Response.builder()
                        .companyId(COMPANY_ID_RENNER)
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
                        .build());
    }

    private List<LocationStockV1Response> buildStockResponseFilteredWhenSkuQuantityApproach() {
        return List.of(
                LocationStockV1Response.builder()
                        .companyId(COMPANY_ID_RENNER)
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
                        .companyId(COMPANY_ID_RENNER)
                        .branchOfficeId("111")
                        .branchOfficeStatus("OK")
                        .items(Collections.singletonList(
                                LocationStockItemV1Response.builder()
                                        .sku(ANY_SKU_PLUS_1)
                                        .amountSaleable(9999)
                                        .amountPhysical(9999)
                                        .blocked(false)
                                        .build()
                        ))
                        .positionBasedOnPriority(19)
                        .okCount(1)
                        .okItems(Collections.singletonList(ANY_SKU))
                        .groupIndex(0)
                        .build()
                );
    }

    private List<ShippingGroupResponseV1> buildGeolocationResponse() {
        return Arrays.asList(ShippingGroupResponseV1.builder()
                        .name("GEOLOCATION")
                        .companyId(COMPANY_ID_RENNER)
                        .branches(Arrays.asList(899, 900, 249, 111))
                        .statePriority(true)
                        .build(),
                ShippingGroupResponseV1.builder()
                        .name("GEOLOCATION 2")
                        .companyId(COMPANY_ID_RENNER)
                        .branches(Arrays.asList(35, 120, 49))
                        .statePriority(false)
                        .build());
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

    private List<CartItem> buildSkuQuantityApproachItemList() {
        return List.of(
                CartItem.builder()
                        .branchOfficeId(111)
                        .sku(ANY_SKU)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build(),
                CartItem.builder()
                        .sku(ANY_SKU_PLUS_1)
                        .branchOfficeId(889)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build(),
                CartItem.builder()
                        .sku(ANY_SKU + "2")
                        .branchOfficeId(111)
                        .quantity(1)
                        .stockStatus(StockStatusEnum.INSTOCK)
                        .productType(ProductTypeEnum.DEFAULT)
                        .build()
        );
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
        deliveryRequest.setLogisticInfo(false);
        deliveryRequest.setStockRequestInput(Arrays.asList(Map.of("companyId", COMPANY_ID_RENNER,
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
        deliveryRequest.setCompanyId(COMPANY_ID_RENNER);
        return deliveryRequest;
    }

}
