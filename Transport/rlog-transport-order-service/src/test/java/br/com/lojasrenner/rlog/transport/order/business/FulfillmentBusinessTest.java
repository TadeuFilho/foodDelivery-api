package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.FulfillmentBusiness;
import br.com.lojasrenner.rlog.transport.order.business.PickupBusiness;
import br.com.lojasrenner.rlog.transport.order.business.QueryBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.*;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.PickupOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CheckoutInfo;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.CheckoutServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.metrics.BadRequestMetrics;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class FulfillmentBusinessTest {

	@InjectMocks
	private FulfillmentBusiness business;

	@Mock
	private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

	@Mock
	private PickupOptionsReactiveDBInfrastructure pickupOptionsDB;

	@Mock
	private QueryBusiness queryBusiness;

	@Mock
	private PickupBusiness pickupBusiness;

	@Mock
	private EcommBusiness ecommBusiness;

	@Mock
	private BadRequestMetrics badRequestMetrics;

	@Mock
	private StockBusiness stockBusiness;

	@Mock
	private BranchOfficeCachedServiceV1 branchService;

	@Mock
	private LiveConfig config;

	@Mock
	private CheckoutServiceV1 checkoutService;

	private List<CartItemWithMode> partOrderNOKCartItemWithMode = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-TRES", 22, "STORE-STANDARD-5-490", null)
	);

	private List<CartItemWithMode> shippingMethodNOKCartItemWithMode = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-PICKUP-3-0", 1001)
	);

	private List<CartItemWithMode> completeCartFulfillModalIdNOK = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-EXPRESS-5-490", null)
	);

	private List<CartItemWithMode> completeCartItemsWithMode = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-STANDARD-5-490", null)
	);

	private List<CartItemWithMode> completeCartItemsWithModePickup = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-PICKUP-3-0", 1001),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-PICKUP-3-0", 1001)
	);

	private List<CartItemWithMode> completeCartItemsWithModeNewModalID = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-STANDARD-5-490-1001", 1001),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-STANDARD-5-490-1001", 1001)
	);

	private List<CartItemWithMode> completeCartItemsPickupWithModeNewModalID = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 11, "STORE-PICKUP-5-490-1001", 1001),
			buildCartItemWithMode("SKU-DOIS", 22, "STORE-PICKUP-5-490-1001", 1001)
	);

	private List<CartItemWithMode> completeCartItemsWithModeSkuNOK = Arrays.asList(
			buildCartItemWithMode("SKU-UM-a", 11, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-DOIS-a", 22, "STORE-PICKUP-3-0", 1001)
	);

	private List<CartItemWithMode> completeCartItemsWithModeStockNOK = Arrays.asList(
			buildCartItemWithMode("SKU-UM", 12, "STORE-STANDARD-5-490", null),
			buildCartItemWithMode("SKU-DOIS", 23, "STORE-STANDARD-5-490", null)
	);

	private List<CartItem> completeCartItems = Arrays.asList(
			buildCartItemWithStockStatusAndProductType("SKU-UM", 11, StockStatusEnum.INSTOCK, ProductTypeEnum.DEFAULT),
			buildCartItemWithStockStatusAndProductType("SKU-DOIS", 22, StockStatusEnum.INSTOCK, ProductTypeEnum.DEFAULT),
			buildCartItemWithStockStatusAndProductType("SKU-TRES", 33, StockStatusEnum.INOMNISTOCK, ProductTypeEnum.DEFAULT),
			buildCartItemWithStockStatusAndProductType("SKU-QUATRO", 44, StockStatusEnum.INSTOCK, ProductTypeEnum.DEFAULT)
	);

	public Map<String, DeliveryOptionsReturn> deliveryOptionsReturnMap = Map.of(
			"completeCartResponse",
			DeliveryOptionsReturn.builder()
					.deliveryOptions(Arrays.asList(
							DeliveryOption.builder()
									.deliveryModesVerbose(Arrays.asList(
											DeliveryMode.builder()
													.modalId("STORE-STANDARD-5-490")
													.quotationId(11111111L)
													.deliveryMethodId(1111)
													.originBranchOfficeId("1001")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(4.90)
													.isRecommendation(true)
													.build(),
											DeliveryMode.builder()
													.modalId("CD-STANDARD-5-490")
													.quotationId(11111111L)
													.deliveryMethodId(1111)
													.originBranchOfficeId("5005")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(4.90)
													.isRecommendation(false)
													.build(),
											DeliveryMode.builder()
													.modalId("STORE-PICKUP-3-0")
													.quotationId(11111111L)
													.deliveryMethodId(1111)
													.originBranchOfficeId("1001")
													.shippingMethod(ShippingMethodEnum.PICKUP)
													.estimatedDeliveryTimeValue("1")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-02")
													.freightCost(0.0)
													.isRecommendation(true)
													.build()
									))
									.sku("SKU-UM")
									.build(),
							DeliveryOption.builder()
									.deliveryModesVerbose(Arrays.asList(
											DeliveryMode.builder()
													.modalId("STORE-STANDARD-5-490")
													.quotationId(11111111L)
													.deliveryMethodId(1111)
													.originBranchOfficeId("1001")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(4.90)
													.isRecommendation(true)
													.build(),
											DeliveryMode.builder()
													.modalId("CD-STANDARD-5-490")
													.quotationId(11111111L)
													.deliveryMethodId(1111)
													.originBranchOfficeId("5005")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(4.90)
													.isRecommendation(false)
													.build(),
											DeliveryMode.builder()
													.modalId("STORE-PICKUP-3-0")
													.quotationId(11111111L)
													.deliveryMethodId(1111)
													.originBranchOfficeId("1001")
													.shippingMethod(ShippingMethodEnum.PICKUP)
													.estimatedDeliveryTimeValue("1")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-02")
													.freightCost(0.0)
													.isRecommendation(true)
													.build()
									))
									.sku("SKU-DOIS")
									.build(),
							DeliveryOption.builder()
									.deliveryModesVerbose(Arrays.asList(
											DeliveryMode.builder()
													.modalId("STORE-STANDARD-5-490")
													.quotationId(33333333L)
													.deliveryMethodId(3333)
													.originBranchOfficeId("2002")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(4.90)
													.build(),
											DeliveryMode.builder()
													.modalId("STORE-STANDARD-5-0")
													.quotationId(33333333L)
													.deliveryMethodId(3333)
													.originBranchOfficeId("1001")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(0.0)
													.build()
									))
									.sku("SKU-TRES")
									.build(),
							DeliveryOption.builder()
									.deliveryModesVerbose(Collections.singletonList(
											DeliveryMode.builder()
													.modalId("CD-PICKUP-5-0")
													.quotationId(44444444L)
													.deliveryMethodId(4444)
													.originBranchOfficeId("5005")
													.shippingMethod(ShippingMethodEnum.PICKUP)
													.estimatedDeliveryTimeValue("1")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-02")
													.freightCost(0.0)
													.build()
									))
									.sku("SKU-QUATRO")
									.build(),
							DeliveryOption.builder()
									.deliveryModesVerbose(Collections.singletonList(
											DeliveryMode.builder()
													.modalId("CD-STANDARD-5-899")
													.quotationId(77777777L)
													.deliveryMethodId(7777)
													.originBranchOfficeId("5005")
													.shippingMethod(ShippingMethodEnum.STANDARD)
													.estimatedDeliveryTimeValue("5")
													.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
													.estimatedDeliveryDate("2021-02-07")
													.freightCost(4.90)
													.build()
									))
									.sku("SKU-SETE")
									.build()
					))
					.build()
	);

	private final List<String> companiesId = Collections.singletonList("555");

	private Map<String, List<BranchOfficeEntity>> ecommBranchOfficeMapActive = Map.of(
			companiesId.get(0),
			Collections.singletonList(
					buildBranchOffice(companiesId.get(0), "5005", true, true, true, "OK", 4, "SP", true, "CD")
			)
	);

	private Map<String, Optional<DeliveryOptionsRequest>> deliveryOptionsRequestMap = Map.of(
			"completeCart",
			Optional.of(withResult(buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "11111-111", 11111111L, 11, 4, 3, 5000, 100), deliveryOptionsReturnMap.get("completeCartResponse")))
	);

	private Map<String, FulfillmentRequest> fulfillmentRequestMap = Map.of(
			"completeCart",
			buildFulfillmentRequest(companiesId.get(0), completeCartItemsWithMode, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"completeCartPickup",
			buildFulfillmentRequest(companiesId.get(0), completeCartItemsWithModePickup, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"partOrderNOK",
			buildFulfillmentRequest(companiesId.get(0), partOrderNOKCartItemWithMode, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"shippingMethodNOK",
			buildFulfillmentRequest(companiesId.get(0), shippingMethodNOKCartItemWithMode, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"completeCartFulfillModalIdNOK",
			buildFulfillmentRequest(companiesId.get(0), completeCartFulfillModalIdNOK, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"completeCartFulfillSkuNOK",
			buildFulfillmentRequest(companiesId.get(0), completeCartItemsWithModeSkuNOK, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"completeCartFulfillStockNOK",
			buildFulfillmentRequest(companiesId.get(0), completeCartItemsWithModeStockNOK, "11111-111", "completeCart", deliveryOptionsRequestMap.get("completeCart").get()),
			"completeCartWithoutIdInformed",
			buildFulfillmentRequest(companiesId.get(0), completeCartItemsWithModeNewModalID, "11111-111", "", deliveryOptionsRequestMap.get("completeCart").get()),
			"completeCartPickupWithoutIdInformed",
			buildFulfillmentRequest(companiesId.get(0), completeCartItemsPickupWithModeNewModalID, "11111-111", "", deliveryOptionsRequestMap.get("completeCart").get())
	);

	private Map<String, List<PickupOptionsRequest>> pickupOptionsMap = Map.of(
			"completeCart",
			Collections.singletonList(
					withResult(buildPickupOptionsRequest("completeCart", companiesId.get(0)), PickupOptionsReturn.builder()
							.pickupOptions(Collections.singletonList(
									PickupOption.builder()
											.deliveryModeId("STORE-PICKUP-3-0")
											.deliveryEstimateBusinessDays(3)
											.fulfillmentMethod(FulfillmentMethodEnum.STORE.getValue())
											.branchId("1001")
											.originBranchOfficeId("1001")
											.deliveryTimeUnit(TimeUnityEnum.DAY)
											.deliveryMethodId("8282")
											.build()
							))
							.build(), Arrays.asList("SKU-UM", "SKU-DOIS"))
			)
	);

	@Before
	public void init() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field stockConfig = StockBusiness.class.getDeclaredField("config");
		stockConfig.setAccessible(true);
		LiveConfig liveConfig = Mockito.mock(LiveConfig.class);
		stockConfig.set(stockBusiness, liveConfig);

		when(checkoutService.checkoutQuotation(anyString(), anyString(), anyString())).thenReturn(true);

		companiesId.forEach(b -> {
			when(config.getConfigValueString(eq(b), any(), any(), eq(true))).thenReturn(UnavailableSkuStrategyEnum.UNAVAILABLE_MODE.toString());
			when(config.getConfigValueBoolean(eq(b), any(), any(), eq(false))).thenReturn(true);
			when(config.getConfigValueBoolean(eq(b), any(), eq(c -> c.getFulfillment().getAutoReQuote()), eq(true))).thenReturn(false);
			when(liveConfig.getConfigValueString(eq(b), any(), any(), eq(false))).thenReturn(BranchSortingEnum.COUNT.toString());
		});

		deliveryOptionsRequestMap.entrySet()
				.forEach(entry -> when(deliveryOptionsDB.findById(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		pickupOptionsMap.entrySet()
				.forEach(entry -> when(pickupOptionsDB.findByDeliveryOptionsId(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		ecommBranchOfficeMapActive.entrySet()
				.forEach(entry -> when(ecommBusiness.getEcommBranchOffice(eq(entry.getKey()), any()))
						.thenReturn(entry.getValue().get(0)));


		//when(stockBusiness.findStoreWithStock(eq(companiesId.get(0)), anyList(), anyList())).thenReturn(ResponseEntity.ok(stockResponse));
		//when(stockBusiness.findBestLocation(anyList(), anyList(), anyList(), any(), any())).thenCallRealMethod();
		//when(stockBusiness.filterAndSortLocations(anyList(), anyList(), anyList(), any(), any())).thenCallRealMethod();

		when(stockBusiness.overrideStockQuantities(anyList(), any(), anyList(), any())).thenAnswer(new Answer<List<LocationStockV1Response>>() {
			@Override
			public List<LocationStockV1Response> answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				return (List<LocationStockV1Response>) ((ResponseEntity) args[1]).getBody();
			}
		});
	}

	@Test(expected = BrokerBadRequestException.class)
	public void test_verify_if_partOrder_have_items_that_should_not_be_there() throws NoQuotationAvailableForFulfillment {
		business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("partOrderNOK"));
	}

	@Test(expected = DeliveryOptionsRequestNotFoundException.class)
	public void test_error_if_delivery_id_not_found() throws NoQuotationAvailableForFulfillment {
		FulfillmentRequest completeCart = fulfillmentRequestMap.get("completeCart");
		String id = completeCart.getCartOrder().getId();
		completeCart.setCartOrder(CartOrder.builder()
				.destination(completeCart.getCartOrder().getDestination())
				.items(completeCart.getCartOrder().getItems())
				.id("paçoca")
				.build());
		try {
			business.getSimplifiedDeliveryFulfillmentForShoppingCart(completeCart);
		} catch (Exception e) {
			completeCart.setCartOrder(CartOrder.builder()
					.destination(completeCart.getCartOrder().getDestination())
					.items(completeCart.getCartOrder().getItems())
					.id(id)
					.build());
			throw e;
		}
	}

	@Test(expected = BrokerBadRequestException.class)
	public void test_verify_if_shippingMethod_from_partOrder_items_are_all_equals() throws NoQuotationAvailableForFulfillment {
		business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("shippingMethodNOK"));
	}

	@Test(expected = ModalIdNotFoundException.class)
	public void test_items_with_modalId_problem() throws NoQuotationAvailableForFulfillment {
		business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartFulfillModalIdNOK"));
	}

	@Test(expected = SkuNotFoundException.class)
	public void test_that_validates_the_sku_of_the_items_to_check_without_problem() throws NoQuotationAvailableForFulfillment {
		business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartFulfillSkuNOK"));
	}

	@Test
	public void test_a_complete_fulfill_without_errors() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {

		//esse é um cenário de sucesso. Nesse cenário, o fulfill vai repetir a cotação para garantir que a loja
		//ainda é capaz de atender. Por isso esse mock tem que retornar uma cotação que signifique que a loja
		//ainda é capaz de atender
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("1001")
						.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("1111", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("11111111", deliveryGroupFulfillment.getExtQuotationId());
		Assert.assertEquals("1001", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test
	public void test_a_complete_fulfill_without_errors_presale() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {

		List<CartItemWithMode> preSaleCartItemsWithMode = Arrays.asList(buildCartItemWithMode("SKU-PRESALE", 1, "CD-STANDARD-5-490", null));
		List<CartItem> preSaleCartItems = Arrays.asList(buildCartItemWithPreSale("SKU-PRESALE", 1, StockStatusEnum.BACKORDERABLE));

		DeliveryOptionsRequest completeCartResponse = withResult(buildDeliveryOptionsRequest(companiesId.get(0),
				preSaleCartItems,
				"11111-111",
				11111111L,
				11,
				4,
				3,
				5000,
				100),

				DeliveryOptionsReturn.builder()
						.deliveryOptions(Arrays.asList(
								DeliveryOption.builder()
										.sku("SKU-PRESALE")
										.deliveryModesVerbose(Arrays.asList(
												DeliveryMode.builder()
														.modalId("CD-STANDARD-5-490")
														.quotationId(51515151L)
														.deliveryMethodId(5151)
														.originBranchOfficeId("5005")
														.shippingMethod(ShippingMethodEnum.STANDARD)
														.estimatedDeliveryTimeValue("5")
														.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
														.estimatedDeliveryDate("2021-02-07")
														.freightCost(4.90)
														.isRecommendation(true)
														.build()
										))
										.build()
						))
						.build());

		completeCartResponse.setPreSaleItemBranchMap(Map.of("5005-1", preSaleCartItems));

		FulfillmentRequest fulfillmentRequest = buildFulfillmentRequest(companiesId.get(0),
				preSaleCartItemsWithMode,
				"11111-111",
				"completeCartPresale",
				completeCartResponse);

		when(deliveryOptionsDB.findById(eq("completeCartPresale"))).thenReturn(Optional.of(completeCartResponse));

		//esse é um cenário de sucesso. Nesse cenário, o fulfill vai repetir a cotação para garantir que a loja
		//ainda é capaz de atender. Por isso esse mock tem que retornar uma cotação que signifique que a loja
		//ainda é capaz de atender
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("5005-1")
						.skus(Arrays.asList("SKU-UM"))
						.build()))
				.deliveryOptions(Arrays.asList(DeliveryOption.builder()
						.sku("SKU-PRESALE")
						.deliveryModes(Arrays.asList(DeliveryMode.builder()
								.shippingMethod(ShippingMethodEnum.STANDARD)
								.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
								.quotationId(1414L)
								.deliveryMethodId(14)
								.isRecommendation(true)
								.originBranchOfficeId("5005")
								.build()))
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequest);
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("5151", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("51515151", deliveryGroupFulfillment.getExtQuotationId());
		Assert.assertEquals("5005", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(1, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-PRESALE", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(1), itemFulfillment.getQuantity());
	}

	@Test
	public void test_a_complete_fulfill_without_errors_pickup() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException, NoActiveBranchForPickupException, BranchOptionsNotFoundOnGeolocationException {

		//esse é um cenário de sucesso. Nesse cenário, o fulfill vai repetir a chamada de pickup para garantir que a loja
		//ainda é capaz de atender com estoque proprio. Por isso esse mock tem que retornar uma cotação que signifique que a loja
		//ainda é capaz de atender
		when(pickupBusiness.getPickupOptions(any())).thenReturn(PickupOptionsReturn.builder()
				.pickupOptions(Arrays.asList(PickupOption.builder()
						.branchId("1001")
						.originBranchOfficeId("1001")
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartPickup"));
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("8282", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("1001", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test
	public void test_a_complete_fulfill_with_different_origin() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("2002")
						.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
						.build()))
				.deliveryOptions(Arrays.asList(DeliveryOption.builder()
								.sku("SKU-UM")
								.deliveryModes(Arrays.asList(DeliveryMode.builder()
										.shippingMethod(ShippingMethodEnum.STANDARD)
										.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
										.quotationId(9393L)
										.deliveryMethodId(93)
										.isRecommendation(true)
										.originBranchOfficeId("999")
										.build()))
								.build(),
						DeliveryOption.builder()
								.sku("SKU-DOIS")
								.deliveryModes(Arrays.asList(DeliveryMode.builder()
										.shippingMethod(ShippingMethodEnum.STANDARD)
										.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
										.quotationId(9393L)
										.deliveryMethodId(93)
										.isRecommendation(true)
										.originBranchOfficeId("999")
										.build()))
								.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.OK_NEW_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("93", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("9393", deliveryGroupFulfillment.getExtQuotationId());
		Assert.assertEquals("999", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test
	public void test_a_complete_fulfill_with_different_origin_reOrder_off() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		companiesId.forEach(b -> {
			when(config.getConfigValueBoolean(eq(b), any(), any(), eq(false))).thenReturn(false);
		});

		//cenário com o pedido originalmente na 2002. Ela nao pode atender, dai direcionamos para a 999
		//como a flag de reOrder está desligada, nao devemos usar a 999
		//devemos dar indisponível

		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("999")
						.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
						.build()))
				.deliveryOptions(Arrays.asList(DeliveryOption.builder()
								.sku("SKU-UM")
								.deliveryModes(Arrays.asList(DeliveryMode.builder()
										.shippingMethod(ShippingMethodEnum.STANDARD)
										.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
										.quotationId(9393L)
										.deliveryMethodId(93)
										.isRecommendation(true)
										.originBranchOfficeId("999")
										.build()))
								.build(),
						DeliveryOption.builder()
								.sku("SKU-DOIS")
								.deliveryModes(Arrays.asList(DeliveryMode.builder()
										.shippingMethod(ShippingMethodEnum.STANDARD)
										.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
										.quotationId(9393L)
										.deliveryMethodId(93)
										.isRecommendation(true)
										.originBranchOfficeId("999")
										.build()))
								.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.NO_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals(true, deliveryGroupFulfillment.getUnavailable());
	}

	@Test
	public void test_a_complete_fulfill_with_different_origin_reOrder_off_new_origin_cd() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		companiesId.forEach(b -> {
			when(config.getConfigValueBoolean(eq(b), any(), any(), eq(false))).thenReturn(false);
		});

		//cenário com o pedido originalmente na 2002. Ela nao pode atender, dai direcionamos para a 5005
		//apesar de a flag de reOrder estar desligada, devemos usar a branch 5005
		//devemos deixar o produto disponivel

		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("5005")
						.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
						.build()))
				.deliveryOptions(Arrays.asList(DeliveryOption.builder()
								.sku("SKU-UM")
								.deliveryModes(Arrays.asList(DeliveryMode.builder()
										.shippingMethod(ShippingMethodEnum.STANDARD)
										.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
										.quotationId(9393L)
										.deliveryMethodId(93)
										.isRecommendation(true)
										.originBranchOfficeId("5005")
										.build()))
								.build(),
						DeliveryOption.builder()
								.sku("SKU-DOIS")
								.deliveryModes(Arrays.asList(DeliveryMode.builder()
										.shippingMethod(ShippingMethodEnum.STANDARD)
										.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
										.quotationId(9393L)
										.deliveryMethodId(93)
										.isRecommendation(true)
										.originBranchOfficeId("5005")
										.build()))
								.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.OK_NEW_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("93", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("9393", deliveryGroupFulfillment.getExtQuotationId());
		Assert.assertEquals("5005", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test
	public void test_a_complete_fulfill_with_partial() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
								.branchId("2002")
								.skus(Arrays.asList("SKU-UM"))
								.build(),
						OriginPreview.builder()
								.branchId("0")
								.skus(Arrays.asList("SKU-DOIS"))
								.build()))
				.deliveryOptions(Arrays.asList(DeliveryOption.builder()
						.sku("SKU-UM")
						.deliveryModes(Arrays.asList(DeliveryMode.builder()
								.shippingMethod(ShippingMethodEnum.STANDARD)
								.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
								.quotationId(9393L)
								.deliveryMethodId(93)
								.isRecommendation(true)
								.originBranchOfficeId("999")
								.build()))
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.PARTIAL_NEW_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(2, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(1);
		Assert.assertTrue(deliveryGroupFulfillment.isUnavailable());
		Assert.assertEquals(1, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());

		deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("93", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("9393", deliveryGroupFulfillment.getExtQuotationId());
		Assert.assertEquals("999", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(1, deliveryGroupFulfillment.getItems().size());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());


	}

	@Test
	public void test_a_complete_fulfill_with_partial_keeping_original_store() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
								.branchId("1001")
								.skus(Arrays.asList("SKU-UM"))
								.build(),
						OriginPreview.builder()
								.branchId("0")
								.skus(Arrays.asList("SKU-DOIS"))
								.build()))
				.deliveryOptions(Arrays.asList(DeliveryOption.builder()
						.sku("SKU-UM")
						.deliveryModes(Arrays.asList(DeliveryMode.builder()
								.shippingMethod(ShippingMethodEnum.STANDARD)
								.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
								.quotationId(1313L)
								.deliveryMethodId(13)
								.isRecommendation(true)
								.originBranchOfficeId("1001")
								.build()))
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.PARTIAL_SAME_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(2, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(1);
		Assert.assertTrue(deliveryGroupFulfillment.isUnavailable());
		Assert.assertEquals(1, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());

		deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("13", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("1313", deliveryGroupFulfillment.getExtQuotationId());
		Assert.assertEquals("1001", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(1, deliveryGroupFulfillment.getItems().size());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());


	}

	@Test
	public void test_a_complete_fulfill_unavailable() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("0")
						.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.NO_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertTrue(deliveryGroupFulfillment.isUnavailable());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());

	}

	@Test(expected = NoQuotationAvailableForFulfillment.class)
	public void test_a_complete_fulfill_where_requote_doesnt_contain_sku() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException {
		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(DeliveryOptionsReturn.builder()
				.originPreview(Arrays.asList(OriginPreview.builder()
						.branchId("2002")
						.skus(Arrays.asList())
						.build()))
				.deliveryOptions(Arrays.asList())
				.build());

		business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCart"));
	}

	//TODO: cobrir cenário de pickup com 2 itens onde 1 seja atendido pela loja e o outro esteja unavailable

	@Test
	public void test_a_complete_fulfill_with_unavailable_item_pickup() throws NoQuotationAvailableForFulfillment, NoActiveBranchForPickupException, BranchOptionsNotFoundOnGeolocationException {
		when(pickupBusiness.getPickupOptions(any())).thenReturn(PickupOptionsReturn.builder()
				.pickupOptions(Arrays.asList(PickupOption.builder()
						.branchId("1001")
						.originBranchOfficeId("999")
						.fulfillmentMethod(FulfillmentMethodEnum.STORE.getValue())
						.deliveryEstimateBusinessDays(9)
						.deliveryTimeUnit(TimeUnityEnum.DAY)
						.deliveryMethodId("1717171")
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartPickup"));
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());
		Assert.assertEquals(DeliveryOptionsOriginTypeEnum.OK_NEW_ORIGIN, result.getOriginType());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("1717171", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("999", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test
	public void test_a_complete_fulfill_without_errors_pickup_with_stockType() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException, NoActiveBranchForPickupException, BranchOptionsNotFoundOnGeolocationException {

		//esse é um cenário de sucesso. Nesse cenário, o fulfill vai repetir a chamada de pickup para garantir que a loja
		//ainda é capaz de atender com estoque proprio. Por isso esse mock tem que retornar uma cotação que signifique que a loja
		//ainda é capaz de atender
		when(pickupBusiness.getPickupOptions(any())).thenReturn(PickupOptionsReturn.builder()
				.pickupOptions(Arrays.asList(PickupOption.builder()
						.branchId("1001")
						.originBranchOfficeId("1001")
						.stockType(DeliveryOptionsStockTypeEnum.OWN_STOCK)
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartPickup"));
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("8282", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("1001", deliveryGroupFulfillment.getOriginBranchId());
		Assert.assertEquals(DeliveryOptionsStockTypeEnum.OWN_STOCK, deliveryGroupFulfillment.getStockType());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());
		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());
		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test
	public void test_not_found_quotation_in_fulfill() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException, NoActiveBranchForPickupException, BranchOptionsNotFoundOnGeolocationException {
		/*
			Neste cenário não é localizado a cotação dentro do fulfillment. Gerando uma nova cotação com o part order informado.
			Esta cotação não é salva no banco somente dentro do documento do fulfillmentRequest
		*/

		companiesId.forEach(b -> {
			when(config.getConfigValueBoolean(any(), any(), any(), eq(true))).thenReturn(true);
		});

		when(deliveryOptionsDB.findById(any())).thenReturn(Optional.empty());

		List<DeliveryMode> deliveryModes = Arrays.asList(
				DeliveryMode.builder()
						.modalId("STORE-EXPRESS-5-550-1001")
						.shippingMethod(ShippingMethodEnum.EXPRESS)
						.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
						.quotationId(151515L)
						.deliveryMethodId(1515)
						.isRecommendation(true)
						.originBranchOfficeId("5005")
						.build(),
				DeliveryMode.builder()
						.modalId("STORE-STANDARD-5-570-1001")
						.shippingMethod(ShippingMethodEnum.STANDARD)
						.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
						.quotationId(141414L)
						.deliveryMethodId(1414)
						.isRecommendation(true)
						.originBranchOfficeId("2002")
						.build()
		);

		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(
				DeliveryOptionsReturn.builder()
						.originPreview(Collections.singletonList(OriginPreview.builder()
								.branchId("2002")
								.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
								.build()))
						.deliveryOptions(Collections.singletonList(
								DeliveryOption.builder()
										.sku("SKU-UM")
										.deliveryModes(deliveryModes)
										.deliveryModesVerbose(deliveryModes)
										.build()))
						.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartWithoutIdInformed"));
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("1414", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("2002", deliveryGroupFulfillment.getOriginBranchId());

		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());

		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());

		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}

	@Test(expected = DeliveryOptionsRequestNotFoundException.class)
	public void test_not_found_quotation_in_fulfill_with_flag_auto_reQuote_false() throws NoQuotationAvailableForFulfillment {
		/*
			Neste cenário não é localizado a cotação dentro do fulfillment.
			Não gerando uma nova cotação pois a flag está como false.
		*/

		when(deliveryOptionsDB.findById(any())).thenReturn(Optional.empty());

		business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartWithoutIdInformed"));
	}

	@Test()
	public void test_not_found_pickup_in_fulfill() throws NoQuotationAvailableForFulfillment, ExecutionException, InterruptedException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException {
		/*
			Neste cenário não é localizado a cotação e o pickup dentro do fulfillment.
			Será gerado uma nova cotação e um novo pickup para o fulfillment somente se a flag estiver ativa.
		*/

		companiesId.forEach(b -> {
			when(config.getConfigValueBoolean(any(), any(), any(), eq(true))).thenReturn(true);
		});

		when(deliveryOptionsDB.findById(any())).thenReturn(Optional.empty());

		List<DeliveryMode> deliveryModes = Arrays.asList(
				DeliveryMode.builder()
						.modalId("STORE-PICKUP-5-550-1001")
						.shippingMethod(ShippingMethodEnum.EXPRESS)
						.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
						.quotationId(151515L)
						.deliveryMethodId(1515)
						.isRecommendation(true)
						.originBranchOfficeId("5005")
						.build(),
				DeliveryMode.builder()
						.modalId("STORE-PICKUP-5-570-1001")
						.shippingMethod(ShippingMethodEnum.PICKUP)
						.fulfillmentMethod(FulfillmentMethodEnum.STORE.toString())
						.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
						.quotationId(141414L)
						.deliveryMethodId(1414)
						.isRecommendation(true)
						.originBranchOfficeId("2002")
						.build()
		);

		when(queryBusiness.getDeliveryModesForShoppingCart(any())).thenReturn(
				DeliveryOptionsReturn.builder()
						.originPreview(Collections.singletonList(OriginPreview.builder()
								.branchId("2002")
								.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
								.build()))
						.deliveryOptions(Collections.singletonList(
								DeliveryOption.builder()
										.sku("SKU-UM")
										.deliveryModes(deliveryModes)
										.deliveryModesVerbose(deliveryModes)
										.build()))
						.build());

		when(pickupBusiness.getPickupOptions(any(), any(), any(), any())).thenReturn(PickupOptionsReturn.builder()
				.pickupOptions(Collections.singletonList(PickupOption.builder()
						.branchId("1001")
						.originBranchOfficeId("2002")
						.deliveryEstimateBusinessDays(2)
						.deliveryTimeUnit(TimeUnityEnum.DAY)
						.deliveryMethodId("15151515")
						.stockType(DeliveryOptionsStockTypeEnum.OWN_STOCK)
						.build()))
				.build());

		CartOrderResult result = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequestMap.get("completeCartPickupWithoutIdInformed"));
		Assert.assertNotNull(result);
		Assert.assertFalse(result.isFulfillmentConditionsHasChanged());
		Assert.assertNotNull(result.getFulfillmentInfo());

		DeliveryGroup deliveryGroup = result.getFulfillmentInfo();

		Assert.assertNotNull(deliveryGroup.getGroups());
		Assert.assertEquals(1, deliveryGroup.getGroups().size());

		DeliveryGroupFulfillment deliveryGroupFulfillment = deliveryGroup.getGroups().get(0);
		Assert.assertEquals("1414", deliveryGroupFulfillment.getExtDeliveryMethodId());
		Assert.assertEquals("2002", deliveryGroupFulfillment.getOriginBranchId());

		Assert.assertEquals(DeliveryOptionsStockTypeEnum.OWN_STOCK, deliveryGroupFulfillment.getStockType());
		Assert.assertEquals(2, deliveryGroupFulfillment.getItems().size());

		ItemFulfillment itemFulfillment = deliveryGroupFulfillment.getItems().get(0);
		Assert.assertEquals("SKU-UM", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(11), itemFulfillment.getQuantity());

		itemFulfillment = deliveryGroupFulfillment.getItems().get(1);
		Assert.assertEquals("SKU-DOIS", itemFulfillment.getSku());
		Assert.assertEquals(Integer.valueOf(22), itemFulfillment.getQuantity());
	}
}
