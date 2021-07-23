package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.PrepareDeliveryOptionsResponseBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.model.Quotes;
import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.OriginPreview;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteBusinessDaysResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteDeliveryOptionsResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.buildCartItem;
import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.buildCartItemWithPreSale;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
public class PrepareDeliveryOptionsResponseBusinessTest {

	@InjectMocks
	private PrepareDeliveryOptionsResponseBusiness prepareDeliveryOptionsResponse;

	@Mock
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Mock
	private EcommBusiness ecommBusiness;

	@Mock
	private LiveConfig config;

	@Mock
	private FreightServiceV1 freightService;

	@Mock
	private Map<String, QuoteBusinessDaysResponseV1> businessDaysMap;

	private static final String CHANNEL = "[unit-test]";
	private static final List<String> companiesId = Arrays.asList("001");

	private final Map<String, String> originStateMap = Map.of(
			"4004", "SP",
			"888", "RJ"
	);

	private final  List<CartItem> completeCartItems = Arrays.asList(
			buildCartItem("SKU-UM", 11),
			buildCartItem("SKU-DOIS", 22),
			buildCartItem("SKU-TRES", 33)
	);

	private final  List<CartItem> ecommCartItems = Arrays.asList(
			buildCartItem("SKU-QUATRO", 44),
			buildCartItem("SKU-CINCO", 55)
	);

	private final List<CartItem> ecommCartItemWithFourItems = Stream.concat(ecommCartItems.stream(), Stream.of(
			buildCartItem("SKU-SETE", 77),
			buildCartItem("SKU-OITO", 88)
	)).collect(Collectors.toList());

	private final  List<CartItem> preSale = Arrays.asList(
			buildCartItemWithPreSale("SKU-SEIS", 66, StockStatusEnum.PREORDERABLE)
	);

	private final  List<CartItem> noStockCartItems = Arrays.asList(
			buildCartItem("SKU-VINTE", 0),
			buildCartItem("SKU-TRINTA", 0)
	);

	private final List<BranchOfficeEntity> branchOfficeEntities = Arrays.asList(
			BuildHelper.buildBranchOffice(companiesId.get(0), "4004", true, true, true, "OK", 3, "SP", false, "STORE"),
			BuildHelper.buildBranchOffice(companiesId.get(0), "888", true, true, true, "OK", 3, "RJ", true, "CD")
	);

	private final  Map<String, Map<String, List<CartItem>>> itemListMapOptions = Map.of(
			"completeCartItems", Map.of("4004", completeCartItems,"0", noStockCartItems),
			"ecommCartItems", Map.of("888", ecommCartItems),
			"ecommFourCartItems", Map.of("888", ecommCartItemWithFourItems),
			"preSaleCartItems", Map.of("888", preSale),
			"storeWithCD", Map.of("4004", completeCartItems, "888", ecommCartItems)
	);

	private final Map<String, PickupOptionsReturn> pickupOptionsMap = Map.of(
			"4004",
			PickupOptionsReturn.builder()
					.pickupOptions(Arrays.asList(
							PickupOption.builder()
									.branchId("4004")
									.branchType(BranchTypeEnum.STORE)
									.deliveryEstimateBusinessDays(5)
									.deliveryMethodId("1234")
									.deliveryModeId("STORE-PICKUP-5-0")
									.fulfillmentMethod(FulfillmentMethodEnum.STORE.getValue())
									.originBranchOfficeId("4004")
									.state("SP")
									.quotationId(11111111111L)
									.build()))
					.build(),
			"888",
			PickupOptionsReturn.builder()
					.pickupOptions(Arrays.asList(
							PickupOption.builder()
									.branchId("888")
									.branchType(BranchTypeEnum.STORE)
									.deliveryEstimateBusinessDays(3)
									.deliveryMethodId("1234")
									.deliveryModeId("STORE-PICKUP-3-0")
									.fulfillmentMethod(FulfillmentMethodEnum.CD.getValue())
									.state("RJ")
									.quoteDeliveryOption(QuoteDeliveryOptionsResponseV1.builder()
											.logisticProviderName("JadLog")
											.deliveryMethodType("PICKUP")
											.deliveryMethodId(1)
											.description("JadLog Delivery")
											.isPickupEnabled(true)
											.schedulingEnabled(true)
											.deliveryEstimateBusinessDays(3)
											.build())
									.originBranchOfficeId("888")
									.quotationId(11111111111L)
									.build()))
					.build()
	);


	@Before
	public void init() {
		branchOfficeEntities.forEach(branch -> when(branchOfficeService.getBranchOffice(eq(companiesId.get(0)), eq(branch.getBranchOfficeId()))).thenReturn(branch));
		when(branchOfficeService.getBranchOffices(eq(companiesId.get(0)))).thenReturn(branchOfficeEntities);

		List<BranchOfficeEntity> ecomms = branchOfficeEntities.stream().filter(branch -> branch.getBranchType().equals("WEB_STORE")).collect(Collectors.toList());
		BranchOfficeEntity mainEcomm = ecomms.stream().filter(ecomm -> ecomm.getBranchOfficeId().equals("888")).iterator().next();

		when(ecommBusiness.getEcommBranchOffice(eq(companiesId.get(0)), eq(CHANNEL))).thenReturn(mainEcomm);
		when(branchOfficeService.getEcommBranchOffices(eq(companiesId.get(0)))).thenReturn(ecomms);

		when(config.getConfigValueString(eq(companiesId.get(0)), eq(Optional.of(CHANNEL)), any(), eq(true))).thenReturn("R$");
		when(freightService.getMultipleDeliveryDates(anySet())).thenReturn(businessDaysMap);
		when(businessDaysMap.get(any())).thenReturn(QuoteBusinessDaysResponseV1.builder().dateDelivery("2020-01-01").build());
	}

	@Test
	public void QuoteFromStore_with_pickup() {
		Map<String, List<CartItem>> itemListMap = itemListMapOptions.get("completeCartItems");
		QuotationDTO quoteFromStore = BuildHelper.buildQuote(itemListMap, pickupOptionsMap, 11111111111L, 5, false);

		QuotationDTO quoteFromEcomm = QuotationDTO.builder().build();
		QuotationDTO quoteFromPreSale = QuotationDTO.builder().build();

		DeliveryOptionsRequest request = BuildHelper.buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "01311-000", 11111111111L,
				5, 4, 3, 100000, 30);

		Quotes quotes = Quotes.builder()
				.quoteFromStore(quoteFromStore)
				.quoteFromEcomm(quoteFromEcomm)
				.quoteFromPreSale(quoteFromPreSale)
				.build();

		DeliveryOptionsReturn response = prepareDeliveryOptionsResponse.buildResponse(request, quotes);

		assertNotNull(response);
		assertEquals(2, response.getOriginPreview().size());

		validateStateOriginPreview(response);

		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-UM")).findAny().get(), "SKU-UM", 11, 5, 3);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-DOIS")).findAny().get(), "SKU-DOIS", 22, 5, 3);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-TRES")).findAny().get(), "SKU-TRES", 33, 5, 3);
	}

	@Test
	public void QuoteFromStore_with_CD() {
		Map<String, List<CartItem>> itemListMap = itemListMapOptions.get("storeWithCD");
		QuotationDTO quoteFromStore = BuildHelper.buildQuote(itemListMap, pickupOptionsMap, 11111111111L, 5, false);

		QuotationDTO quoteFromEcomm = QuotationDTO.builder().build();
		QuotationDTO quoteFromPreSale = QuotationDTO.builder().build();

		DeliveryOptionsRequest request = BuildHelper.buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "01311-000", 11111111111L,
				5, 4, 3, 100000, 30);

		request.setLogisticInfo(true);

		Quotes quotes = Quotes.builder()
				.quoteFromStore(quoteFromStore)
				.quoteFromEcomm(quoteFromEcomm)
				.quoteFromPreSale(quoteFromPreSale)
				.build();

		DeliveryOptionsReturn response = prepareDeliveryOptionsResponse.buildResponse(request, quotes);

		assertNotNull(response);
		assertEquals(2, response.getOriginPreview().size());

		validateStateOriginPreview(response);

		//SKU's LOJA
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-UM")).findAny().get(), "SKU-UM", 11, 5, 3);
		assertTrue(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-UM")).findAny().get().getDeliveryModes().stream().noneMatch(d -> d.getLogisticCarrierInfo() != null));
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-DOIS")).findAny().get(), "SKU-DOIS", 22, 5, 3);
		assertTrue(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-DOIS")).findAny().get().getDeliveryModes().stream().noneMatch(d -> d.getLogisticCarrierInfo() != null));
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-TRES")).findAny().get(), "SKU-TRES", 33, 5, 3);
		assertTrue(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-TRES")).findAny().get().getDeliveryModes().stream().noneMatch(d -> d.getLogisticCarrierInfo() != null));

		//SKU's CD
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-QUATRO")).findAny().get(), "SKU-QUATRO", 44, 5, 3);
		assertTrue(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-QUATRO")).findAny().get().getDeliveryModes().stream().allMatch(d -> d.getLogisticCarrierInfo() != null));
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-CINCO")).findAny().get(), "SKU-CINCO", 55, 5, 3);
		assertTrue(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-CINCO")).findAny().get().getDeliveryModes().stream().allMatch(d -> d.getLogisticCarrierInfo() != null));
	}

	@Test
	public void QuoteFromEcomm_with_pickup() {
		Map<String, List<CartItem>> itemListMap = itemListMapOptions.get("ecommCartItems");
		QuotationDTO quoteFromEcomm = BuildHelper.buildQuote(itemListMap, pickupOptionsMap, 11111111111L, 5, false);

		QuotationDTO quoteFromStore = QuotationDTO.builder().build();
		QuotationDTO quoteFromPreSale = QuotationDTO.builder().build();

		DeliveryOptionsRequest request = BuildHelper.buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "01311-000", 11111111111L,
				5, 4, 3, 100000, 30);

		Quotes quotes = Quotes.builder()
				.quoteFromStore(quoteFromStore)
				.quoteFromEcomm(quoteFromEcomm)
				.quoteFromPreSale(quoteFromPreSale)
				.build();

		DeliveryOptionsReturn response = prepareDeliveryOptionsResponse.buildResponse(request, quotes);

		assertNotNull(response);
		assertEquals(1, response.getOriginPreview().size());

		validateStateOriginPreview(response);

		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-QUATRO")).findAny().get(), "SKU-QUATRO", 44, 6, 4);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-CINCO")).findAny().get(), "SKU-CINCO", 55, 6, 4);


	}

	@Test
	public void QuoteFromEcomm_vs_QuoteFromStore() {
		Map<String, List<CartItem>> storeItemListMap = itemListMapOptions.get("completeCartItems");
		Map<String, List<CartItem>> ecommItemListMap = itemListMapOptions.get("ecommFourCartItems");
		QuotationDTO quoteFromStore = BuildHelper.buildQuote(storeItemListMap, pickupOptionsMap, 11111111111L, 6, false);
		QuotationDTO quoteFromEcomm = BuildHelper.buildQuote(ecommItemListMap, pickupOptionsMap, 11111111111L, 5, false);

		QuotationDTO quoteFromPreSale = QuotationDTO.builder().build();

		DeliveryOptionsRequest request = BuildHelper.buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "01311-000", 11111111111L,
				5, 4, 3, 100000, 30);

		Quotes quotes = Quotes.builder()
				.quoteFromStore(quoteFromStore)
				.quoteFromEcomm(quoteFromEcomm)
				.quoteFromPreSale(quoteFromPreSale)
				.build();

		DeliveryOptionsReturn response = prepareDeliveryOptionsResponse.buildResponse(request, quotes);

		assertNotNull(response);
		assertEquals(1, response.getOriginPreview().size());

		validateStateOriginPreview(response);

		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-QUATRO")).findAny().get(), "SKU-QUATRO", 44, 6, 4);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-CINCO")).findAny().get(), "SKU-CINCO", 55, 6, 4);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-SETE")).findAny().get(), "SKU-SETE", 77, 6, 4);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-OITO")).findAny().get(), "SKU-OITO", 88, 6, 4);
	}

	@Test
	public void PreSale_QuoteFromEcomm() {
		QuotationDTO quoteFromEcomm = BuildHelper.buildQuote(itemListMapOptions.get("ecommCartItems"), pickupOptionsMap, 11111111111L, 5, false);
		QuotationDTO quoteFromPreSale = BuildHelper.buildQuote(itemListMapOptions.get("preSaleCartItems"), pickupOptionsMap, 11111111111L, 5, true);

		QuotationDTO quoteFromStore = QuotationDTO.builder().build();

		DeliveryOptionsRequest request = BuildHelper.buildDeliveryOptionsRequest(companiesId.get(0), completeCartItems, "01311-000", 11111111111L,
				5, 4, 3, 100000, 30);

		request.setPreSaleItemBranchMap(itemListMapOptions.get("preSaleCartItems"));

		Quotes quotes = Quotes.builder()
				.quoteFromStore(quoteFromStore)
				.quoteFromEcomm(quoteFromEcomm)
				.quoteFromPreSale(quoteFromPreSale)
				.build();

		DeliveryOptionsReturn response = prepareDeliveryOptionsResponse.buildResponse(request, quotes);

		assertNotNull(response);
		assertEquals(2, response.getOriginPreview().size());

		validateStateOriginPreview(response);

		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-QUATRO")).findAny().get(), "SKU-QUATRO", 44, 6, 4);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-CINCO")).findAny().get(), "SKU-CINCO", 55, 6, 4);
		validateDeliveryOption(response.getDeliveryOptions().stream().filter(d -> d.getSku().equals("SKU-SEIS")).findAny().get(), "SKU-SEIS", 66, 5, 3);
	}

	private void validateStateOriginPreview(DeliveryOptionsReturn response) {
		Map<String, HashSet<String>> statesMap = new HashMap<>();
		response.getDeliveryOptions().forEach(deliveryOption ->
				deliveryOption.getDeliveryModes().forEach(deliveryMode -> {
					HashSet<String> states = statesMap.computeIfAbsent(deliveryMode.getOriginBranchOfficeId(), k -> new HashSet<>());
					states.add(deliveryMode.getState());
				}));

		statesMap.forEach((key, value) -> {
			String expectedState = originStateMap.get(key);
			value.forEach(actualState -> assertEquals(expectedState, actualState));
		});
	}

	private void validateDeliveryOption(DeliveryOption deliveryOption, String sku, Integer quantity, int verboseSize, int size) {
		assertEquals(sku, deliveryOption.getSku());
		assertEquals(quantity, deliveryOption.getQuantity());
		assertEquals(verboseSize, deliveryOption.getDeliveryModesVerbose().size());
		assertEquals(size, deliveryOption.getDeliveryModes().size());
	}
}
