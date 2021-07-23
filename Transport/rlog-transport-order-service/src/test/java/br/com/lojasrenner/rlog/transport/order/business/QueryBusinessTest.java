package br.com.lojasrenner.rlog.transport.order.business;

import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.*;
import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.buildQuoteRequest;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.GeolocationBusiness;
import br.com.lojasrenner.rlog.transport.order.business.PickupBusiness;
import br.com.lojasrenner.rlog.transport.order.business.QueryBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestLocationBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestSolutionBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.PrepareDeliveryOptionsResponseBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.SplitShoppingCartBusiness;
import br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.*;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import br.com.lojasrenner.rlog.transport.order.business.exception.BranchOptionsNotFoundOnGeolocationException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoActiveBranchForPickupException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteRequestV1;
import brave.Tracing;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class QueryBusinessTest {

	@InjectMocks
	private QueryBusiness business;

	@Mock
	private FreightServiceV1 freightService;

	@Mock
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Mock
	private GeolocationBusiness geolocationBusiness;

	@Mock
	private StockBusiness stockBusiness;

	@Mock
	private PickupBusiness pickupBusiness;

	@Mock
	private EcommBusiness ecommBusiness;

	@Mock
	private LiveConfig config;

	@Mock
	private BestSolutionBusiness bestSolutionBusiness;

	@Mock
	private SplitShoppingCartBusiness splitShoppingCartBusiness;

	@Mock
	private BestLocationBusiness bestLocationBusiness;

	@Mock
	private PrepareDeliveryOptionsResponseBusiness prepareDeliveryOptionsResponse;

	private InOrder getInOrderObject() {
		return inOrder(stockBusiness);
	}

	private static final List<LocationStockV1Response> stockResponse = Arrays.asList(
		LocationStockV1Response.builder()
			.branchOfficeId("1001")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(100)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-DOIS")
						.amountSaleable(200)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-TRES")
						.amountSaleable(300)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("2002")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(100)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(200)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(300)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("3003")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(100)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(200)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(300)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("4004")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(10)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(10)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(10)
					.build(),
					LocationStockItemV1Response.builder()
					.sku("SKU-CINCO")
					.amountSaleable(20)
					.build(),
					LocationStockItemV1Response.builder()
					.sku("SKU-SEIS")
					.amountSaleable(20)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("5005")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(20)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(20)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(20)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("6006")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(30)
					.blocked(true)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(30)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(30)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("7007")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(100)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(200)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(300)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("8008")
			.branchOfficeStatus("NOK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(100)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(200)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(300)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("9009")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(100)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(200)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(300)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("599")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(21)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-TRES")
					.amountSaleable(31)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-SP")
					.amountSaleable(59)
					.build()
			))
			.build()
	);

	private List<CartItem> completeCartItems = Arrays.asList(
			buildCartItem("SKU-UM", 11),
			buildCartItem("SKU-DOIS", 22),
			buildCartItem("SKU-TRES", 33)
	);

	private List<CartItem> spCartItems = Arrays.asList(
			buildCartItem("SKU-UM", 11),
			buildCartItem("SKU-DOIS", 21),
			buildCartItem("SKU-TRES", 31),
			buildCartItem("SKU-SP", 59)
	);

	private List<CartItem> skuUmItems = Arrays.asList(
			buildCartItem("SKU-UM", 11)
	);

	private List<CartItem> skuDoisItems = Arrays.asList(
			buildCartItem("SKU-DOIS", 22)
	);

	private List<CartItem> skuTresItems = Arrays.asList(
			buildCartItem("SKU-TRES", 33)
	);

	private List<CartItem> skuQuatroItems = Arrays.asList(
			buildCartItem("SKU-QUATRO", 4)
	);

	private List<CartItem> skuCDOK = Arrays.asList(
			buildCartItem("SKU-UM", 11),
			buildCartItem("SKU-QUATRO", 4)
	);

	private List<CartItem> skuSPLoja = Arrays.asList(
			buildCartItem("SKU-DOIS", 21),
			buildCartItem("SKU-TRES", 31),
			buildCartItem("SKU-SP", 59)
	);

	private List<CartItem> skuSeisECinco = Arrays.asList(
			buildCartItem("SKU-CINCO", 10),
			buildCartItem("SKU-SEIS", 10)
	);

	private List<CartItem> skuQuantityApproach = Arrays.asList(
			buildCartItem("SKU-DOIS", 21),
			buildCartItem("SKU-TRES", 31),
			buildCartItem("SKU-CINCO", 10),
			buildCartItem("SKU-SEIS", 10),
			buildCartItem("SKU-SP", 59)
	);

	private List<CartItem> skuPreSale = Arrays.asList(
			buildCartItemWithStockStatusAndProductType("SKU-UM", 11, StockStatusEnum.PREORDERABLE, null),
			buildCartItemWithStockStatusAndProductType("SKU-DOIS", 22, StockStatusEnum.INSTOCK, null),
			buildCartItemWithStockStatusAndProductType("SKU-TRES", 33, null, null)
	);

	private List<CartItem> skuGift = Arrays.asList(
			buildCartItemWithStockStatusAndProductType("SKU-UM", 11, StockStatusEnum.INSTOCK, null),
			buildCartItemWithStockStatusAndProductType("SKU-DOIS", 22, StockStatusEnum.INSTOCK, ProductTypeEnum.GIFT),
			buildCartItemWithStockStatusAndProductType("SKU-TRES", 33, StockStatusEnum.INSTOCK, null)
	);

	private Map<BranchOfficeEntity, List<CartItem>> skuParallelStock = Map.of(
			buildBranchOffice("111", "888", true, true, true, "OK", null, "RJ", true, "CD"),
			Arrays.asList(buildCartItemWithStockStatusAndProductType("SKU-UM", 11, StockStatusEnum.INSTOCK, null)),
			buildBranchOffice("111", "5005", true, true, true, "OK", 5, "SP", false, "STORE"),
			Arrays.asList(buildCartItemWithStockStatusAndProductType("SKU-DOIS", 22, StockStatusEnum.INOMNISTOCK, null),
			buildCartItemWithStockStatusAndProductType("SKU-TRES", 33, StockStatusEnum.INOMNISTOCK, null)),
			buildBranchOffice("111", "4004", true, false, true, "OK", 4, "SP", false, "STORE"),
			Arrays.asList(buildCartItemWithStockStatusAndProductType("SKU-CINCO", 10, StockStatusEnum.INOMNISTOCK, null),
			buildCartItemWithStockStatusAndProductType("SKU-SEIS", 10, StockStatusEnum.INOMNISTOCK, null))
	);

	private final List<String> companiesId = Arrays.asList("111", "222");

	private final Map<String, DeliveryOptionsRequest> deliveryOptionsRequestMap = Map.ofEntries(
			Map.entry("completeCart",
			buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "11111-111", null, 0, 3, 2, 5000, 100)),
			Map.entry("geoProblem",
			buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "22222-222", null, 0, 3, 2, 5000, 100)),
			Map.entry("branchProblem",
			buildDeliveryOptionsRequest(companiesId.get(1), completeCartItems, "33333-333", null, 0, 3, 2, 5000, 100)),
			Map.entry("stockProblem",
			buildDeliveryOptionsRequest(companiesId.get(0), skuQuatroItems, "11111-111", null, 0, 3, 2, 5000, 100)),
			Map.entry("stockProblemCDOK",
			buildDeliveryOptionsRequest(companiesId.get(0), skuCDOK, "11111-111", null, 0, 3, 2, 5000, 100)),
			Map.entry("restricted",
			buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "11111-111", null, 0, true, 3, 2, 5000, 100)),
			Map.entry("cdSP599",
			buildDeliveryOptionsRequest(companiesId.get(0), spCartItems, "01111-599", null, 0, 3, 2, 5000, 100)),
			Map.entry("skuQuantityApproach",
			buildDeliveryOptionsRequest(companiesId.get(0), skuQuantityApproach, "01111-599", null, 0, 3, 2, 5000, 2)),
			Map.entry("skuPreSale",
			buildDeliveryOptionsRequest(companiesId.get(0), skuPreSale, "11111-123", null, 0, 3, 2, 5000, 2)),
			Map.entry("skuGift",
			buildDeliveryOptionsRequest(companiesId.get(0), skuGift, "11111-123", null, 0, 3, 2, 5000, 2)),
			Map.entry("parallelStock",
			buildDeliveryOptionsRequest(companiesId.get(0), skuParallelStock.values().stream().flatMap(Collection::stream).collect(Collectors.toList()), "11111-999", null, 0, 3, 2, 5000, 100))
	);

	private Map<String, List<BranchOfficeEntity>> ecommBranchOfficeMap = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "888", true, true, true, "OK", null, "RJ", true, "CD")
			),
			companiesId.get(1),
			Arrays.asList(
					buildBranchOffice(companiesId.get(1), "777", true, true, true, "OK", null, "RJ", true, "CD")
			)
	);

	private Map<String, List<BranchOfficeEntity>> branchOfficeMap = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "1001", false, true, true, "OK", 1, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "2002", true, false, false, "OK", 2, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3003", true, true, false, "OK", 3, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4004", true, false, true, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, true, "OK", 5, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "6006", true, true, true, "OK", 6, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "599", true, false, true, "OK", 6, "SP", false, "CD2")
			),
			companiesId.get(1),
			Arrays.asList(
					buildBranchOffice("111", "1001", false, true, true, "OK", 1, "SP", false, "STORE")
			)
	);

	private Map<String, List<BranchOfficeEntity>> branchOfficeMapActive = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "2002", true, false, false, "OK", 2, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3003", true, true, false, "OK", 3, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4004", true, false, true, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, true, "OK", 5, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "6006", true, true, true, "OK", 6, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "599", true, false, true, "OK", 6, "SP", false, "CD2")
			),
			companiesId.get(1),
			Arrays.asList()
	);

	private Map<String, List<BranchOfficeEntity>> branchOfficeMapActiveForShipping = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "4004", true, false, true, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, true, "OK", 5, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "6006", true, true, true, "OK", 6, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "599", true, false, true, "OK", 6, "SP", false, "CD2")
			)
	);

	private Map<QuoteRequestV1, QuoteResponseV1> quoteMap = Map.ofEntries(
			Map.entry(buildQuoteRequest("00888-555", "11111-111", completeCartItems),
				buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "11111-111", skuQuatroItems),
				buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "22222-222", completeCartItems),
				buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "01111-599", spCartItems),
				buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("00777-555", "33333-333", completeCartItems),
				buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "11111-111", skuTresItems),
				buildQuoteResponse(11888555L, 8)),
			Map.entry(buildQuoteRequest("06006-555", "11111-111", skuDoisItems),
				buildQuoteResponse(16006555L, 6)),
			Map.entry(buildQuoteRequest("05005-555", "11111-111", skuUmItems),
				buildQuoteResponse(15005555L, 5)),
			Map.entry(buildQuoteRequest("00599-555", "01111-599", skuSPLoja),
				buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("05005-555", "01111-599", skuUmItems),
				buildQuoteResponse(15005555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "11111-111", skuCDOK),
					buildQuoteResponse(11888555L, 5)),
			Map.entry(buildQuoteRequest("04004-555", "01111-599", skuSeisECinco),
					buildQuoteResponse(14004555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "11111-123", skuPreSale),
					buildQuoteResponse(14004555L, 5)),
			Map.entry(buildQuoteRequest("04004-555", "11111-999", skuSeisECinco),
					buildQuoteResponse(14004555L, 5)),
			Map.entry(buildQuoteRequest("05005-555", "11111-999", Stream.concat(skuDoisItems.stream(), skuTresItems.stream()).collect(Collectors.toList())),
					buildQuoteResponse(15005555L, 5)),
			Map.entry(buildQuoteRequest("00888-555", "11111-999", skuUmItems),
					buildQuoteResponse(15005555L, 5))
	);

	private Map<DeliveryOptionsRequest, PickupOptionsReturn> pickupOptionsMap = Map.of(
			deliveryOptionsRequestMap.get("completeCart"),
			PickupOptionsReturn.builder()
				.pickupOptions(Arrays.asList(
					PickupOption.builder()
						.branchId("1001")
						.branchType(BranchTypeEnum.STORE)
						.deliveryEstimateBusinessDays(5)
						.deliveryMethodId("1234")
						.deliveryModeId("STORE-PICKUP-5-0")
						.fulfillmentMethod(FulfillmentMethodEnum.STORE.getValue())
						.originBranchOfficeId("899")
						.quotationId(12345678L)
						.build()))
				.build(),
			deliveryOptionsRequestMap.get("skuPreSale"),
			PickupOptionsReturn.builder()
					.pickupOptions(Arrays.asList(
							PickupOption.builder()
									.branchId("888")
									.branchType(BranchTypeEnum.STORE)
									.deliveryEstimateBusinessDays(5)
									.deliveryMethodId("1234")
									.deliveryModeId("CD-PICKUP-5-0")
									.fulfillmentMethod(FulfillmentMethodEnum.CD.getValue())
									.originBranchOfficeId("888")
									.quotationId(12345678L)
									.build()))
					.build()

	);

	private Map<DeliveryOptionsRequest, List<GeoLocationResponseV1>> gelocationResponseMap = Map.of(
			deliveryOptionsRequestMap.get("completeCart"),
			Arrays.asList(
					buildGeolocationItem("5005", 5005, true),
					buildGeolocationItem("1001", 1001, true),
					buildGeolocationItem("2002", 2002, true),
					buildGeolocationItem("3003", 3003, true),
					buildGeolocationItem("4004", 4004, true),
					buildGeolocationItem("6006", 6006, false)
			),
			deliveryOptionsRequestMap.get("parallelStock"),
			Arrays.asList(
					buildGeolocationItem("5005", 5005, true),
					buildGeolocationItem("1001", 1001, true),
					buildGeolocationItem("2002", 2002, true),
					buildGeolocationItem("3003", 3003, true),
					buildGeolocationItem("4004", 4004, true),
					buildGeolocationItem("6006", 6006, false)
			),
			deliveryOptionsRequestMap.get("geoProblem"),
			Arrays.asList(),
			deliveryOptionsRequestMap.get("branchProblem"),
			Arrays.asList(
					buildGeolocationItem("1001", 1001, true)
			),
			deliveryOptionsRequestMap.get("cdSP599"),
			Arrays.asList(
					buildGeolocationItem("1001", 1001, true),
					buildGeolocationItem("2002", 2002, true),
					buildGeolocationItem("3003", 3003, true),
					buildGeolocationItem("4004", 4004, true),
					buildGeolocationItem("5005", 5005, true),
					buildGeolocationItem("6006", 6006, false),
					buildGeolocationItem("599", 59900, true)
			)
	);

	@Before
	public void init() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field expirationMinutes = business.getClass().getDeclaredField("expirationMinutes");
		expirationMinutes.setAccessible(true);
		expirationMinutes.set(business, 1440);

		Integer timeout = 10000000;
		Field stockConfig = StockBusiness.class.getDeclaredField("config");
		stockConfig.setAccessible(true);
		LiveConfig businessConfig = Mockito.mock(LiveConfig.class);
		stockConfig.set(stockBusiness, businessConfig);


		companiesId.forEach(b -> {
			when(config.getConfigValueString(eq(b), any(), any(), eq(true))).thenReturn(UnavailableSkuStrategyEnum.RETRY_MODE.toString());
			when(config.getConfigValueInteger(eq(b), any(), any(), eq(true))).thenReturn(timeout);
			when(businessConfig.getConfigValueString(eq(b), any(), any(), eq(false))).thenReturn(BranchSortingEnum.COUNT.toString());
			when(config.getConfigValueString(eq(b), any(), any(), eq(false))).thenReturn(CountryEnum.BR.toString());
		});

		ReflectionTestUtils.setField(business, "executor", Executors.newFixedThreadPool(8));

		when(prepareDeliveryOptionsResponse.buildResponse(any(), any())).thenReturn(new DeliveryOptionsReturn());

		branchOfficeMap.forEach((key, value) -> {
			when(branchOfficeService.getBranchOffices(eq(key))).thenReturn(value);
			value.forEach(entity -> when(branchOfficeService.getBranchOffice(eq(key), eq(entity.getBranchOfficeId()))).thenReturn(entity));
		});

		ecommBranchOfficeMap.entrySet().forEach(entry -> {
			entry.getValue().forEach((b) -> when(ecommBusiness.getEcommBranchOffice(eq(entry.getKey()), any())).thenReturn(b));
		});

        ecommBranchOfficeMap.entrySet()
		        .forEach(entry -> {
		          entry.getValue().forEach((b) -> when(branchOfficeService.getBranchOffice(eq(entry.getKey()), eq(b.getBranchOfficeId())))
		                  .thenReturn(b));
		        });

		branchOfficeMapActive.entrySet()
				.forEach(entry -> when(branchOfficeService.getActiveBranchOffices(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		branchOfficeMapActiveForShipping.entrySet()
				.forEach(entry -> when(branchOfficeService.getActiveBranchOfficesForShipping(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		ecommBranchOfficeMap.entrySet()
				.forEach(entry -> when(ecommBusiness.getEcommBranchOffice(eq(entry.getKey()), any()))
						.thenReturn(entry.getValue().get(0)));

		ecommBranchOfficeMap.entrySet()
				.forEach(entry -> when(branchOfficeService.getEcommBranchOffices(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		quoteMap.entrySet()
				.forEach(entry -> when(freightService.getQuote(any(), anyString(), eq(entry.getKey())))
						.thenReturn(ResponseEntity.ok(entry.getValue())));

		pickupOptionsMap.entrySet()
				.forEach(entry -> {
					try {
						when(pickupBusiness.getPickupOptions(any(), eq(entry.getKey()), any(), any()))
								.thenReturn(entry.getValue());
					} catch (BranchOptionsNotFoundOnGeolocationException e) {
						fail(e.toString());
					} catch (NoActiveBranchForPickupException e) {
						fail(e.toString());
					}
				});

		gelocationResponseMap.entrySet()
				.forEach(entry -> {
					List<ShippingGroupResponseV1> response = new ArrayList<>();

					response.add(ShippingGroupResponseV1.builder()
							.companyId("888")
							.name("GEOLOCATION")
							.branches(entry.getValue()
									.stream().map(g -> Integer.parseInt(g.getBranchOfficeId()))
									.collect(Collectors.toList()))
							.build());

					response.add(ShippingGroupResponseV1.builder()
							.companyId("888")
							.name("GEOLOCATION-ECOMM")
							.branches(Arrays.asList(Integer.parseInt("888")))
							.build());
					DeliveryOptionsRequest delivery = new DeliveryOptionsRequest();
					delivery.setCompanyId(entry.getKey().getCompanyId());


					when(geolocationBusiness.getShippingGroups(eq(entry.getKey().getCompanyId()), any(), eq(entry.getKey().getDestinationZipcode()), any(), any()))
							.thenAnswer(new Answer<List<ShippingGroupResponseV1>>() {
								@Override
								public List<ShippingGroupResponseV1> answer(InvocationOnMock invocation) throws Throwable {
									Object[] args = invocation.getArguments();
									DeliveryOptionsRequest req = (DeliveryOptionsRequest) args[4];
									req.setShippingGroupResponseObject(ShippingGroupResponseObjectV1.builder().shippingGroupResponse(response).build());
									return response;
								}
							});
				});


		when(stockBusiness.findStoreWithStock(eq("111"), anyString(), anyList(), anyList())).thenReturn(ResponseEntity.ok(stockResponse));
		when(stockBusiness.findBestLocation(anyList(), anyList(), anyList(), any(), any())).thenCallRealMethod();
		when(stockBusiness.filterAndSortLocations(anyList(), anyList(), anyList(), any(), any())).thenCallRealMethod();
		when(stockBusiness.getAmountSaleableForItem(any(), any())).thenCallRealMethod();

		when(stockBusiness.overrideStockQuantities(anyList(), any(), anyList(), any())).thenAnswer(new Answer<List<LocationStockV1Response>>() {
					@Override
					public List<LocationStockV1Response> answer(InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						List<BranchOfficeEntity> branches = (List<BranchOfficeEntity>) args[2];
						List<String> branchIds = branches.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList());
						return ((List<LocationStockV1Response>) ((ResponseEntity)args[1]).getBody()).stream()
								.filter(s -> branchIds.contains(s.getBranchOfficeId()))
								.collect(Collectors.toList());
					}
				});

		when(stockBusiness.prepareStockResponse(anyList(), any(), anyList(), anyMap(), anyList())).thenCallRealMethod();
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreOK() throws InterruptedException, ExecutionException {
		when(bestSolutionBusiness.findBestSolution(any(), any())).thenReturn(
				Map.of("5005", List.of(buildCartItem("SKU-UM", 11)),
						"888", List.of(buildCartItem("SKU-TRES", 33)),
						"6006", List.of(buildCartItem("SKU-DOIS", 22))
				)
		);

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("completeCart");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap(), true);
		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), true);

		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreNOK_geoProblem() throws InterruptedException, ExecutionException {
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("geoProblem");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreNOK_branchProblem() throws InterruptedException, ExecutionException {
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("branchProblem");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());

		assertEquals(DeliveryOptionsErrorEnum.BRANCH_STATUS, deliveryOptionsRequest.getStatistics().getReason());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreNOK_stockProblem() throws InterruptedException, ExecutionException {
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("stockProblem");

		when(stockBusiness.findStoreWithStock(any(), anyString(), anyList(), anyList())).thenReturn(ResponseEntity.ok(Arrays.asList()));

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());

		assertEquals(DeliveryOptionsErrorEnum.STOCK_UNAVAILABLE, deliveryOptionsRequest.getStatistics().getReason());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreNOK_noLocation() throws InterruptedException, ExecutionException {
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("stockProblem");

		when(stockBusiness.findStoreWithStock(any(), anyString(), anyList(), anyList())).thenReturn(ResponseEntity.ok(Arrays.asList(
				LocationStockV1Response.builder()
				.branchOfficeId("9009")
				.branchOfficeStatus("NOK")
				.items(Arrays.asList())
				.build())));

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);
		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());

		assertEquals(0, deliveryOptionsRequest.getStockResponseList().get(deliveryOptionsRequest.getStockResponseList().size() - 1).size());
		assertEquals(DeliveryOptionsErrorEnum.STOCK_UNAVAILABLE, deliveryOptionsRequest.getStatistics().getReason());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreOK_restricted() throws InterruptedException, ExecutionException {
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("restricted");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreNOK_sp() throws InterruptedException, ExecutionException {

		when(bestSolutionBusiness.findBestSolution(any(), any())).thenReturn(
				Map.of("599", List.of(
						buildCartItem("SKU-DOIS", 21),
						buildCartItem("SKU-TRES", 31),
						buildCartItem("SKU-SP", 59)),
						"5005", List.of(
								buildCartItem("SKU-UM", 11))
				)
		);

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("cdSP599");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);
		validateQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void ecomStockOK_storeStockNOK_pickupStockOK() throws InterruptedException, ExecutionException, NoSuchFieldException, IllegalAccessException {
		when(splitShoppingCartBusiness.splitShoppingCartByOrigin(any(), anyList(), anyList(), any())).thenReturn(
				Map.of("888", List.of(
						buildCartItem("SKU-UM", 11),
						buildCartItem("SKU-QUATRO", 4)
				))
		);

		when(bestLocationBusiness.findBestLocationGrouping(any(), anyList(), anyList(),anyList(), anyList(), anyList(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenReturn(LocationStockV1Response.builder()
						.companyId("001")
						.branchOfficeId("888")
						.branchOfficeStatus("OK")
						.items(List.of(LocationStockItemV1Response.builder().sku("SKU-UM").amountSaleable(1000).build(),
								LocationStockItemV1Response.builder().sku("SKU-QUATRO").amountSaleable(5).build()
								))
						.build());

		when(config.getConfigValueString(eq(companiesId.get(0)), any(), any(), eq(true))).thenReturn(UnavailableSkuStrategyEnum.UNAVAILABLE_MODE.toString());

		List<BranchOfficeEntity> ecomBranchs = Collections.singletonList(
				buildBranchOffice("111", "888", true, true, true, "OK", null, "RJ", true, "CD")
		);

		List<LocationStockV1Response> stockResponse = Collections.singletonList(
				LocationStockV1Response.builder()
						.branchOfficeId("888")
						.branchOfficeStatus("OK")
						.items(Arrays.asList(
								LocationStockItemV1Response.builder()
										.sku("SKU-UM")
										.amountSaleable(1000)
										.build(),
								LocationStockItemV1Response.builder()
										.sku("SKU-QUATRO")
										.amountSaleable(5)
										.build()
						))
						.build());

		when(stockBusiness.overrideStockQuantities(anyList(), any(), eq(ecomBranchs), eq(ecomBranchs.get(0)))).thenReturn(stockResponse);

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("stockProblemCDOK");
		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap(), false);
		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void ecomStockNOK_storeStockNOK_pickupStockOK() throws InterruptedException, ExecutionException, NoSuchFieldException, IllegalAccessException {
		when(bestSolutionBusiness.findBestSolution(any(), any())).thenReturn(
				Map.of("0", List.of(buildCartItem("SKU-QUATRO", 4)),
						"5005", List.of(buildCartItem("SKU-UM", 11))
				)
		);

		when(config.getConfigValueString(eq(companiesId.get(0)), any(), any(), eq(true))).thenReturn(UnavailableSkuStrategyEnum.UNAVAILABLE_MODE.toString());

		List<BranchOfficeEntity> ecomBranchs = Collections.singletonList(
				buildBranchOffice("111", "888", true, true, true, "OK", null, "RJ", true, "CD")
		);

		when(stockBusiness.findStoreWithStock(eq("111"), anyString(), anyList(), eq(ecomBranchs))).thenReturn(ResponseEntity.ok(stockResponse));

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("stockProblemCDOK");
		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap(), false);

		validateEmptyQuote(new HashMap<>(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void ecomStockNOK_quoteFromStoreOK_skuQuantityApproach() throws InterruptedException, ExecutionException {

		companiesId.forEach(b -> {
			when(config.getConfigValueString(eq(b), any(), any(), eq(true))).thenReturn("DYNAMIC_BOX_ALL_ITEMS");
			when(config.getConfigValueString(eq(b), any(), any(), eq(false))).thenReturn(CountryEnum.BR.toString());
		});

		when(bestSolutionBusiness.findBestSolution(any(), any())).thenReturn(
				Map.of("599", List.of(
						BuildHelper.buildCartItem("SKU-DOIS", 21),
						BuildHelper.buildCartItem("SKU-TRES", 31),
						BuildHelper.buildCartItem("SKU-SP", 59)),
						"4004", List.of(
								BuildHelper.buildCartItem("SKU-CINCO", 10),
								BuildHelper.buildCartItem("SKU-SEIS", 10))
				)
		);

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("skuQuantityApproach");
		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap(), false);
		validateEmptyQuote(new HashMap<>(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void quoteFromEcommOK_quoteFromStoreOK_quoteFromPreSaleOK() throws ExecutionException, InterruptedException {
		when(freightService.getQuote(any(), anyString(), any())).thenReturn(ResponseEntity.ok(buildQuoteResponse(888555L, 5)));
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("skuPreSale");
		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap(), true);
		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
	}

	@Test
	public void quoteFromEcommOK_pickupOK_quoteFromStoreOK_giftItem() throws InterruptedException, ExecutionException {
		when(stockBusiness.overrideStockQuantities(any(), any(), any(), any())).thenCallRealMethod();
		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("skuGift");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	@Test
	public void quoteFromEcommOk_quoteFromStoreOK_parallelStockService() throws ExecutionException, InterruptedException {
		List<String> branchesInRange = gelocationResponseMap.get(deliveryOptionsRequestMap.get("parallelStock"))
				.stream()
				.map(GeoLocationResponseV1::getBranchOfficeId)
				.collect(Collectors.toList());

		List<BranchOfficeEntity> activeBranchesInRange = branchOfficeMapActiveForShipping.get(companiesId.get(0))
				.stream()
				.filter(b -> branchesInRange.contains(b.getBranchOfficeId()))
				.collect(Collectors.toList());

		when(config.getConfigValueBoolean(eq("111"), any(), any(), eq(false))).thenReturn(true);

		when(bestSolutionBusiness.findBestSolution(any(), any())).thenReturn(
				Map.of("888", List.of(buildCartItem("SKU-UM", 11)),
						"5005", List.of(buildCartItem("SKU-DOIS", 22), buildCartItem("SKU-TRES", 33)),
						"4004", List.of(buildCartItem("SKU-CINCO", 10), buildCartItem("SKU-SEIS", 10))
				)
		);

		skuParallelStock.entrySet().forEach(entry -> {
			for(CartItem item : entry.getValue()) {
				Map<String, int[]> quantityMap = new HashMap<>();
				activeBranchesInRange.stream().forEach(b -> {
					if(b.getBranchOfficeId().equals(entry.getKey().getBranchOfficeId()))
						quantityMap.put(entry.getKey().getBranchOfficeId(), new int[] {999});
					else
						quantityMap.put(b.getBranchOfficeId(), new int[] {-10});
				});

				when(stockBusiness.findStoreWithStock(any(), anyString(), eq(Arrays.asList(item)), eq(activeBranchesInRange)))
						.thenReturn(ResponseEntity.ok(buildStockResponse(Arrays.asList(item), activeBranchesInRange, quantityMap)));
			}
		});

		InOrder _inOrder = getInOrderObject();

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("parallelStock");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		_inOrder.verify(stockBusiness, Mockito.times(5)).findStoreWithStock(any(), any(), any(), any());
		validateQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap(), false);
	}

	@Test
	public void quoteWithMaxCartItemsEnabled() throws ExecutionException, InterruptedException {
		when(config.getConfigValueInteger(any(), any(), any(), eq(true))).thenReturn(1).thenReturn(10000000);
		Map<String, List<CartItem>> map = new HashMap<>();
		map.put("5005", Arrays.asList(CartItem.builder().sku("SKU-UM").quantity(11).build()));
		map.put("888", Arrays.asList(CartItem.builder().sku("SKU-TRES").quantity(33).build()));
		map.put("6006", Arrays.asList(CartItem.builder().sku("SKU-DOIS").quantity(22).build()));

		when(bestSolutionBusiness.findBestSolution(any(), any())).thenReturn(map);

		DeliveryOptionsRequest deliveryOptionsRequest = deliveryOptionsRequestMap.get("completeCart");

		business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);

		validateQuote(deliveryOptionsRequest.getItemBranchMapForEcomm(), deliveryOptionsRequest.getQuoteMapForEcomm(), deliveryOptionsRequest.getEcommPickupOptionsReturnMap(), false);

		validateEmptyQuote(deliveryOptionsRequest.getItemBranchMap(), deliveryOptionsRequest.getQuoteMap(), deliveryOptionsRequest.getPickupOptionsReturnMap());
		validateEmptyQuote(deliveryOptionsRequest.getPreSaleItemBranchMap(), deliveryOptionsRequest.getPreSaleQuoteMap(), deliveryOptionsRequest.getPreSalePickupOptionsReturnMap());
	}

	private void validateQuote(Map<String, List<CartItem>> itemBranchMap,
							   Map<String, QuoteResponseV1> quoteMap,
							   Map<String, PickupOptionsReturn> pickupOptionsReturnMap,
							   boolean shouldValidatePickup) {
		quoteMap.entrySet().forEach(entry -> {
			assertNotNull(entry.getValue());
			assertNotNull(entry.getValue().getContent());
		});
		int expectedSize = itemBranchMap.entrySet()
				.stream()
				.filter(entry -> !entry.getKey().equals("0"))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
				.size();

		assertEquals(expectedSize, quoteMap.size());

		if(shouldValidatePickup) {
			pickupOptionsReturnMap.entrySet().forEach(entry -> {
				assertNotNull(entry.getValue());
				assertNotNull(entry.getValue().getPickupOptions());
				assertFalse(entry.getValue().getPickupOptions().isEmpty());
			});
			assertEquals(expectedSize, pickupOptionsReturnMap.size());
		}
	}

	private void validateEmptyQuote(Map<String, List<CartItem>> itemBranchMap,
									Map<String, QuoteResponseV1> quoteMap,
									Map<String, PickupOptionsReturn> pickupOptionsReturnMap) {
		assertTrue(itemBranchMap == null || itemBranchMap.isEmpty());
		assertTrue(quoteMap == null || quoteMap.isEmpty());
		assertTrue(pickupOptionsReturnMap == null || pickupOptionsReturnMap.isEmpty());
	}

}
