package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.SplitShoppingCartBusiness;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.UnavailableSkuStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class SplitShoppingCartBusinessTest {

    @Mock
    private EcommBusiness ecommBusiness;

    @Mock
    private LiveConfig config;

    @InjectMocks
    private SplitShoppingCartBusiness splitShoppingCartBusiness;

    private static final String ANY_SKU = "548860081";
    private static final String ANY_OTHER_SKU = "548860082";
    private static final String COMPANY_ID_RENNER = "001";
    private static final String APPLICATION_NAME = "Teste Unitario";
    private static final String BRANCH_OFFICE_ID_899 = "899";
    private static final String BRANCH_OFFICE_ID_900 = "900";

    @Test
    public void splitShoppingCartByOriginWhenJustHaveEcommBranchOfficeItemsTest() {
        when(ecommBusiness.getEcommBranchOffice(eq(COMPANY_ID_RENNER), eq(APPLICATION_NAME))).thenReturn(
                BranchOfficeEntity.builder()
                        .id("000".concat(BRANCH_OFFICE_ID_899))
                        .build());

        DeliveryOptionsRequest deliveryOptionsRequest = buildDeliveryOptionsRequest();
        final List<CartItem> itemList = List.of(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .build(),
                CartItem.builder()
                        .sku(ANY_OTHER_SKU)
                        .quantity(1)
                        .build()
        );

        final List<LocationStockV1Response> stockResponse = buildStockResponse(List.of(
                LocationStockItemV1Response.builder()
                        .sku(ANY_SKU)
                        .amountSaleable(0)
                        .amountPhysical(0)
                        .blocked(true)
                        .build(),
                LocationStockItemV1Response.builder()
                        .sku(ANY_OTHER_SKU)
                        .amountSaleable(9999999)
                        .amountPhysical(9999999)
                        .blocked(false)
                        .build()
        ), BRANCH_OFFICE_ID_899);
        final LocationStockV1Response buildBestLocation = stockResponse.stream().findAny().get();

        final Map<String, List<CartItem>> solution = splitShoppingCartBusiness
                .splitShoppingCartByOrigin(deliveryOptionsRequest, itemList, stockResponse, buildBestLocation);
        Assert.assertNotNull(solution);
    }

    @Test
    public void splitShoppingCartByOriginWhenJustHaveUnavailableItemsTest() {
        when(ecommBusiness.getEcommBranchOffice(eq(COMPANY_ID_RENNER), eq(APPLICATION_NAME))).thenReturn(
                BranchOfficeEntity.builder()
                        .id("000".concat(BRANCH_OFFICE_ID_899))
                        .build());

        DeliveryOptionsRequest deliveryOptionsRequest = buildDeliveryOptionsRequest();
        final List<CartItem> itemList = Collections.singletonList(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .build()
        );

        final List<LocationStockV1Response> stockResponse = buildStockResponse(Collections.singletonList(
                LocationStockItemV1Response.builder()
                        .sku(ANY_SKU)
                        .amountSaleable(0)
                        .amountPhysical(0)
                        .blocked(true)
                        .build()
        ), BRANCH_OFFICE_ID_899);
        final LocationStockV1Response buildBestLocation = stockResponse.stream().findAny().get();

        when(config.getConfigValueString(eq(COMPANY_ID_RENNER), eq(Optional.ofNullable(APPLICATION_NAME)), any(), eq(true)))
                .thenReturn(UnavailableSkuStrategyEnum.UNAVAILABLE_MODE.name());

        final Map<String, List<CartItem>> solution = splitShoppingCartBusiness
                .splitShoppingCartByOrigin(deliveryOptionsRequest, itemList, stockResponse, buildBestLocation);
        Assert.assertNotNull(solution);
    }


    @Test
    public void splitShoppingCartByOriginTest() {

        when(ecommBusiness.getEcommBranchOffice(eq(COMPANY_ID_RENNER), eq(APPLICATION_NAME))).thenReturn(
                BranchOfficeEntity.builder()
                        .id("000".concat(BRANCH_OFFICE_ID_899))
                        .build());

        DeliveryOptionsRequest deliveryOptionsRequest = buildDeliveryOptionsRequest();
        final List<CartItem> itemList = List.of(
                CartItem.builder()
                        .sku(ANY_SKU)
                        .quantity(1)
                        .build(),
                CartItem.builder()
                        .sku(ANY_OTHER_SKU)
                        .quantity(10)
                        .build()
        );

        final List<LocationStockV1Response> stockResponse900 = buildStockResponse(List.of(
                LocationStockItemV1Response.builder()
                        .sku(ANY_SKU)
                        .amountSaleable(9999)
                        .amountPhysical(9999)
                        .blocked(false)
                        .build(),
                LocationStockItemV1Response.builder()
                        .sku(ANY_OTHER_SKU)
                        .amountSaleable(100)
                        .amountPhysical(100)
                        .blocked(false)
                        .build()
        ), BRANCH_OFFICE_ID_900);

        final List<LocationStockV1Response> stockResponse899 = buildStockResponse(List.of(
                LocationStockItemV1Response.builder()
                        .sku(ANY_SKU)
                        .amountSaleable(1)
                        .amountPhysical(11)
                        .blocked(false)
                        .build(),
                LocationStockItemV1Response.builder()
                        .sku(ANY_OTHER_SKU)
                        .amountSaleable(0)
                        .amountPhysical(0)
                        .blocked(false)
                        .build()
        ), BRANCH_OFFICE_ID_899);

        final LocationStockV1Response buildBestLocation = buildStockResponse(List.of(
                LocationStockItemV1Response.builder()
                        .sku(ANY_SKU)
                        .amountSaleable(9999)
                        .amountPhysical(9999)
                        .blocked(false)
                        .build(),
                LocationStockItemV1Response.builder()
                        .sku(ANY_OTHER_SKU)
                        .amountSaleable(100)
                        .amountPhysical(100)
                        .blocked(false)
                        .build()
        ), BRANCH_OFFICE_ID_899.concat("+").concat(BRANCH_OFFICE_ID_900)).stream().findAny().get();

        var stockResponse = Stream.concat(stockResponse899.stream(), stockResponse900.stream())
                .collect(Collectors.toList());

        final Map<String, List<CartItem>> solution = splitShoppingCartBusiness
                .splitShoppingCartByOrigin(deliveryOptionsRequest, itemList, stockResponse, buildBestLocation);

        Assert.assertNotNull(solution);
        Assert.assertTrue(solution.get(BRANCH_OFFICE_ID_900).stream().anyMatch(item -> ANY_SKU.equals(item.getSku())));
        Assert.assertTrue(solution.get(BRANCH_OFFICE_ID_900).stream().anyMatch(item -> ANY_OTHER_SKU.equals(item.getSku())));
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
        deliveryRequest.setStockRequestInput(Arrays.asList(Map.of("companyId", "001",
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
        deliveryRequest.setXApplicationName(APPLICATION_NAME);
        deliveryRequest.setCompanyId(COMPANY_ID_RENNER);
        return deliveryRequest;
    }

    private List<LocationStockV1Response> buildStockResponse(List<LocationStockItemV1Response> items, final String branchOfficeId) {
        return Collections.singletonList(
                LocationStockV1Response.builder()
                        .branchOfficeId(branchOfficeId)
                        .items(items)
                        .build());
    }

}
