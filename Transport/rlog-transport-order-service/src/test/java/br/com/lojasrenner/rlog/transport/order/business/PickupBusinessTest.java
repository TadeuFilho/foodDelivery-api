package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.GeolocationBusiness;
import br.com.lojasrenner.rlog.transport.order.business.PickupBusiness;
import br.com.lojasrenner.rlog.transport.order.business.QueryBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.*;
import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsStockTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingToResponseV1;
import br.com.lojasrenner.rlog.transport.order.metrics.BadRequestMetrics;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

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
import java.util.stream.Collectors;

import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class PickupBusinessTest {

    @InjectMocks
    private PickupBusiness business;

	@Mock
	private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

	@Mock
	private GeolocationBusiness geolocationBusiness;

	@Mock
	private StockBusiness stockBusiness;

	@Mock
	private QueryBusiness queryBusiness;

	@Mock
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Mock
	private EcommBusiness ecommBusiness;

	@Mock
	private BadRequestMetrics badRequestMetrics;

	@Mock
	private LiveConfig config;

	private List<String> companiesId = Arrays.asList("001", "002", "003");

	private Map<String, List<BranchOfficeEntity>> ecommBranchOfficeMap = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "899", true, true, true, "OK", null, "RJ", true, "CD")
			),
			companiesId.get(2),
			Arrays.asList(
					buildBranchOffice(companiesId.get(2), "899", true, true, true, "OK", null, "RJ", true, "CD")
			)
	);

	private List<CartItem> generalItems = Arrays.asList(buildCartItem("SKU-UM", 11), buildCartItem("SKU-DOIS", 22));

	private Map<String, Optional<DeliveryOptionsRequest>> deliveryOptionsRequestMap = Map.of(
			"generalDeliveryOptionsRequest",
			Optional.of(buildDeliveryOptionsRequest(companiesId.get(0), generalItems, "11111-111", 11111111L, 11, 1, 0, 5000, 100)),
			"noGeolocationResponse_throws_error",
			Optional.of(buildDeliveryOptionsRequest(companiesId.get(0), null, "33333-333", null, 0, 1, 0, 5000, 100)),
			"company003",
			Optional.of(buildDeliveryOptionsRequest(companiesId.get(2), generalItems, "11111-111", 11111111L, 11, 1, 0, 5000, 100)),
			"generalDeliveryOptionsRequestWithNewZipCode",
			Optional.of(buildDeliveryOptionsRequest(companiesId.get(0), generalItems, "12345-678", 11111111L, 11, 1, 0, 5000, 100))
		);

	private Map<String, List<GeoLocationResponseV1>> geolocationMap = Map.of(
			"generalDeliveryOptionsRequest",
			Arrays.asList(
					buildGeolocationItem("1001", 1001, false),
					buildGeolocationItem("2002", 2002, false),
					buildGeolocationItem("3003", 3003, false),
					buildGeolocationItem("4004", 4004, false),
					buildGeolocationItem("5005", 5005, false),
					buildGeolocationItem("35", 35, false),
					buildGeolocationItem("249", 249, false),
					buildGeolocationItem("899", 899, false)
			),
			"noGeolocationResponse_throws_error",
			new ArrayList<>(),
			"generalDeliveryOptionsRequestWithNewZipCode",
			Arrays.asList(
					buildGeolocationItem("1101", 1001, false),
					buildGeolocationItem("2202", 2002, false),
					buildGeolocationItem("3303", 3003, false),
					buildGeolocationItem("4404", 4004, false),
					buildGeolocationItem("5505", 5005, false)
			)
		);

	private Map<String, List<ShippingToResponseV1>> geolocationShippingToMap = Map.of(
			"generalDeliveryOptionsRequest",
			Arrays.asList()
	);

	private Map<String, List<GeoLocationResponseV1>> geolocationStateMap = Map.of(
			companiesId.get(0) + "-RJ",
			Arrays.asList(
					buildGeolocationItem("6006", 6006, false)
			),
			companiesId.get(0) + "-MG",
			Arrays.asList(
					buildGeolocationItem("8008", 8008, false)
			),
			companiesId.get(2) + "-BA",
			Arrays.asList(
					buildGeolocationItem("9999", 9999, false)
			)
		);

	private Map<String, QuotationDTO> quotationMap = Map.of(
			"generalDeliveryOptionsRequest",
			new QuotationDTO(Map.of("899", Arrays.asList(buildCartItem("SKU-DOIS", 22))), Map.of("899", buildQuoteResponse(22222222L, 22)), null),
			"company003",
			new QuotationDTO(Map.of("899", generalItems), Map.of("899", buildQuoteResponse(22222222L, 22)), null)
		);

	private Map<String, List<BranchOfficeEntity>> branchOfficesMap = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", 1, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "2002", true, false, true, "OK", 2, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3003", true, true, true, "OK", 3, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4004", true, true, false, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, false, "OK", null, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "6006", true, true, false, "OK", 6, "RJ", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "7007", false, true, false, "OK", 7, "SP", false, "STORE")
			),
			companiesId.get(1),
			new ArrayList<>(),
			companiesId.get(2),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "9009", true, false, false, "OK", null, "BA", false, "STORE")
			)
		);

	private Map<String, List<BranchOfficeEntity>> activeBranchOfficesMap = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", 1, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "2002", true, false, true, "OK", 2, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3003", true, true, true, "OK", 3, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4004", true, true, false, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, false, "OK", null, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "6006", true, true, false, "OK", 6, "RJ", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "1101", true, true, true, "OK", 1, "AM", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "2202", true, false, true, "OK", 2, "AM", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3303", true, true, true, "OK", 3, "AM", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4404", true, true, false, "OK", 4, "AM", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5505", true, true, false, "OK", null, "AM", false, "STORE")
			),
			companiesId.get(1),
			new ArrayList<>(),
			companiesId.get(2),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "9009", true, false, false, "OK", null, "BA", false, "STORE")
			)
		);

	private Map<String, List<BranchOfficeEntity>> activeBranchOfficesForPickupMap = Map.of(
			companiesId.get(0),
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", 1, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3003", true, true, true, "OK", 3, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4004", true, true, false, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, false, "OK", null, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "6006", true, true, false, "OK", 6, "RJ", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "899", true, true, true, "OK", null, "RJ", true, "CD")
			),
			companiesId.get(0) + "-SP",
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", 1, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "3003", true, true, true, "OK", 3, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "4004", true, true, false, "OK", 4, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "5005", true, true, false, "OK", null, "SP", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "899", true, true, true, "OK", null, "RJ", true, "CD")
			),
			companiesId.get(0) + "-RJ",
			Arrays.asList(
					buildBranchOffice(companiesId.get(0), "6006", true, true, false, "OK", 6, "RJ", false, "STORE"),
					buildBranchOffice(companiesId.get(0), "899", true, true, true, "OK", null, "RJ", true, "CD")
			)
		);

	private List<LocationStockV1Response> stockResponse = buildStockResponse(generalItems, activeBranchOfficesForPickupMap.get(companiesId.get(0)), Map.of(
			"1001", new int[] { 11, 22 },
			"3003", new int[] { 1, 2 },
			"4004", new int[] { -1100, 2200 },
			"5005", new int[] { 1100, 2200 },
			"899", new int[] { 9999, 9999 },
			"6006", new int[] { 100, 200 }
			));

	@Before
	public void init() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field f1 = business.getClass().getDeclaredField("fallbackValueForPickupDeliveryEstimate");
		f1.setAccessible(true);
		f1.set(business, 97);

		when(config.getConfigValueInteger(any(), any(), any(), eq(true))).thenReturn(1);
		when(config.getConfigValueBoolean(any(), any(), any(), eq(false))).thenReturn(true);

		Field stockConfig = StockBusiness.class.getDeclaredField("config");
		stockConfig.setAccessible(true);
		LiveConfig liveConfig = Mockito.mock(LiveConfig.class);
		stockConfig.set(stockBusiness, liveConfig);

		deliveryOptionsRequestMap.entrySet()
				.forEach(entry -> when(deliveryOptionsDB.findById(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		geolocationMap.entrySet()
				.forEach(entry -> when(geolocationBusiness.getBranchesForPickup(eq(deliveryOptionsRequestMap.get(entry.getKey()).get().getCompanyId()), any(), eq(deliveryOptionsRequestMap.get(entry.getKey()).get().getDestinationZipcode())))
						.thenReturn(entry.getValue()));

		geolocationShippingToMap.entrySet().forEach(entry -> when(geolocationBusiness.getShippingToForBranches(eq(deliveryOptionsRequestMap.get(entry.getKey()).get().getCompanyId()), any(), any())).thenReturn(entry.getValue()));

		geolocationStateMap.entrySet()
				.forEach(entry -> when(geolocationBusiness.getGeolocationResponseForState(eq(entry.getKey().split("-")[0]), any(), eq(entry.getKey().split("-")[1])))
						.thenReturn(entry.getValue()));

		branchOfficesMap.entrySet()
				.forEach(entry -> when(branchOfficeService.getBranchOffices(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		activeBranchOfficesMap.entrySet()
				.forEach(entry -> when(branchOfficeService.getActiveBranchOffices(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		activeBranchOfficesForPickupMap.entrySet()
				.forEach(entry -> when(branchOfficeService.getActiveBranchOfficesForPickup(eq(entry.getKey())))
						.thenReturn(entry.getValue()));

		ecommBranchOfficeMap.entrySet()
				.forEach(entry -> when(ecommBusiness.getEcommBranchOffice(eq(entry.getKey()), any()))
						.thenReturn(entry.getValue().get(0)));

		when(stockBusiness.filterAndSortLocations(anyList(), anyList(), anyList(), anyList(), any())).thenCallRealMethod();

		companiesId.forEach(i -> {
			when(liveConfig.getConfigValueString(eq(i), any(), any(), eq(false))).thenReturn(BranchSortingEnum.COUNT.toString());
		});

		when(stockBusiness.overrideStockQuantities(anyList(), any(), anyList(), any())).thenAnswer(new Answer<List<LocationStockV1Response>>() {
			@Override
			public List<LocationStockV1Response> answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();

				if (args[1] == null)
					return new ArrayList<>();

				return (List<LocationStockV1Response>) ((ResponseEntity)args[1]).getBody();
			}
		});

		when(stockBusiness.findStoreWithStockWithString(any(), any(), anyList(), anyList())).thenAnswer(new Answer<ResponseEntity<List<LocationStockV1Response>>>() {
			@Override
			public ResponseEntity<List<LocationStockV1Response>> answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				List<String> requested = ((List<String>)args[3]);
				List<LocationStockV1Response> list = stockResponse.stream().filter(s -> requested.contains(s.getBranchOfficeId())).collect(Collectors.toList());

				return ResponseEntity.ok(list);
			}
		});

		quotationMap.entrySet()
				.forEach(entry -> when(queryBusiness.quoteFromEcomm(eq(deliveryOptionsRequestMap.get(entry.getKey()).get()), anyList()))
						.thenReturn(entry.getValue()));
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(5, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "STORE-PICKUP-1-0-1001", 1);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "1001", "1001");
		validatePickupOptionGeo(option, 1001);
		validatePickupOptionIntelipost(option, null, null);

		option = pickupOptions.getPickupOptions().get(1);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "2002", "899");
		validatePickupOptionGeo(option, 2002);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(2);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "3003", "899");
		validatePickupOptionGeo(option, 3003);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(3);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "4004", "899");
		validatePickupOptionGeo(option, 4004);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(4);

		validatePickupOptionBroker(option, "STORE-PICKUP-97-0-5005", 97);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "5005", "5005");
		validatePickupOptionGeo(option, 5005);
		validatePickupOptionIntelipost(option, null, null);
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus_novoZipcode() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		pickupOptionsRequest.setZipcode("12345-678");

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(5, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "1101", "899");
		validatePickupOptionGeo(option, 1001);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(1);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "2202", "899");
		validatePickupOptionGeo(option, 2002);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(2);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "3303", "899");
		validatePickupOptionGeo(option, 3003);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(3);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "4404", "899");
		validatePickupOptionGeo(option, 4004);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(4);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "5505", "899");
		validatePickupOptionGeo(option, 5005);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus_novoState() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		when(queryBusiness.quoteFromEcomm(any(), anyList()))
				.thenReturn(quotationMap.get("generalDeliveryOptionsRequest"));

		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		pickupOptionsRequest.setState("RJ");

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(1, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "STORE-PICKUP-6-0-6006", 6);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "6006", "6006");
		validatePickupOptionGeo(option, 6006);
		validatePickupOptionIntelipost(option, null, null);
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus_novoState_faltandoBranchNoGeo() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		when(queryBusiness.quoteFromEcomm(any(), anyList()))
				.thenReturn(quotationMap.get("company003"));

		when(queryBusiness.quoteFromEcomm(eq(buildDeliveryOptionsRequest(companiesId.get(2), generalItems, "09009-555", 11111111L, 11, 3, 2, 5000, 100)), anyList()))
				.thenReturn(quotationMap.get("company003"));

		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("company003", companiesId.get(2));
		pickupOptionsRequest.setState("BA");

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(0, pickupOptions.getPickupOptions().size());
	}

	@Test(expected = NoBranchAvailableForState.class)
	public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus_stateSemBranch() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		pickupOptionsRequest.setState("MG");

		business.getPickupOptions(pickupOptionsRequest);

		fail();
	}

	@Test
	public void deliveryOptionsParam_geoOK_branchesOK_todosSkus() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest(null, companiesId.get(0));

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest, deliveryOptionsRequestMap.get("generalDeliveryOptionsRequest").get(), null, null);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(5, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "STORE-PICKUP-1-0-1001", 1);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "1001", "1001");
		validatePickupOptionGeo(option, 1001);
		validatePickupOptionIntelipost(option, null, null);

		option = pickupOptions.getPickupOptions().get(1);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "2002", "899");
		validatePickupOptionGeo(option, 2002);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(2);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "3003", "899");
		validatePickupOptionGeo(option, 3003);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(3);

		validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "4004", "899");
		validatePickupOptionGeo(option, 4004);
		validatePickupOptionIntelipost(option, "11111111", 11111111L);

		option = pickupOptions.getPickupOptions().get(4);

		validatePickupOptionBroker(option, "STORE-PICKUP-97-0-5005", 97);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "5005", "5005");
		validatePickupOptionGeo(option, 5005);
		validatePickupOptionIntelipost(option, null, null);
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_partialSkus() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		when(queryBusiness.quoteFromEcomm(any(), anyList()))
				.thenReturn(quotationMap.get("generalDeliveryOptionsRequest"));

		when(queryBusiness.quoteFromEcomm(eq(buildDeliveryOptionsRequest(companiesId.get(0), Arrays.asList(buildCartItem("SKU-DOIS", 22)), "11111-111", 11111111L, 11, 3, 2, 5000, 100)), anyList()))
				.thenReturn(quotationMap.get("generalDeliveryOptionsRequest"));

		when(stockBusiness.findStoreWithStockWithString(any(), any(), anyList(), anyList())).thenReturn(ResponseEntity.ok(
				buildStockResponse(
						Arrays.asList(buildCartItem("SKU-DOIS", 22)),
						Arrays.asList(buildBranchOffice(companiesId.get(0), "899", true, true, true, "OK", null, "RJ", true, "CD"),
								buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", 1001, "SP", false, "STORE"),
								buildBranchOffice(companiesId.get(0), "4004", true, true, true, "OK", 4004, "SP", false, "STORE"),
								buildBranchOffice(companiesId.get(0), "5005", true, true, true, "OK", 5005, "SP", false, "STORE")),
						Map.of("899", new int[] { 9999 },
								"1001", new int[] { 9999 },
								"4004", new int[] { 9999 },
								"5005", new int[] { 9999 }))));

		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		pickupOptionsRequest.setSkus(Arrays.asList("SKU-DOIS"));

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(5, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "STORE-PICKUP-1-0-1001", 1);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "1001", "1001");
		validatePickupOptionGeo(option, 1001);
		validatePickupOptionIntelipost(option, null, null);

		option = pickupOptions.getPickupOptions().get(1);

		validatePickupOptionBroker(option, "STORE-PICKUP-4-0-4004", 4);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "4004", "4004");
		validatePickupOptionGeo(option, 4004);
		validatePickupOptionIntelipost(option, null, null);

		option = pickupOptions.getPickupOptions().get(2);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "2002", "899");
		validatePickupOptionGeo(option, 2002);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);

		option = pickupOptions.getPickupOptions().get(3);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "3003", "899");
		validatePickupOptionGeo(option, 3003);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);

		option = pickupOptions.getPickupOptions().get(4);

		validatePickupOptionBroker(option, "STORE-PICKUP-97-0-5005", 97);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "5005", "5005");
		validatePickupOptionGeo(option, 5005);
		validatePickupOptionIntelipost(option, null, null);
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_stockError() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		when(queryBusiness.quoteFromEcomm(any(), anyList()))
				.thenReturn(quotationMap.get("generalDeliveryOptionsRequest"));

		when(stockBusiness.overrideStockQuantities(anyList(), any(), anyList(), any())).thenReturn(BuildHelper.buildStockResponse(Arrays.asList(buildCartItem("SKU-UM", 11)), ecommBranchOfficeMap.get(companiesId.get(0)), Map.of(
				"899", new int[] { 9999, 9999 }
		)));

		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		pickupOptionsRequest.setSkus(Arrays.asList("SKU-UM"));

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(5, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "1001", "899");
		validatePickupOptionGeo(option, 1001);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);

		option = pickupOptions.getPickupOptions().get(1);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "2002", "899");
		validatePickupOptionGeo(option, 2002);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);

		option = pickupOptions.getPickupOptions().get(2);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "3003", "899");
		validatePickupOptionGeo(option, 3003);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);

		option = pickupOptions.getPickupOptions().get(3);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "4004", "899");
		validatePickupOptionGeo(option, 4004);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);

		option = pickupOptions.getPickupOptions().get(4);

		validatePickupOptionBroker(option, "CD-PICKUP-24-0-899", 24);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "5005", "899");
		validatePickupOptionGeo(option, 5005);
		validatePickupOptionIntelipost(option, "22222222", 22222222L);
	}

	@Test
	public void test_a_complete_pickup_with_re_quote_shipping_to() throws NoActiveBranchForPickupException, BranchOptionsNotFoundOnGeolocationException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequestWithReQuote("generalDeliveryOptionsRequest", companiesId.get(0));
		assertTrue(pickupOptionsRequest.getQuoteSettings().getReQuotePickup());

		when(geolocationBusiness.getShippingToForBranches(eq(pickupOptionsRequest.getCompanyId()), eq(pickupOptionsRequest.getXApplicationName()), anyList())).thenReturn(
				Collections.singletonList(buildShippingToItem("1001", Collections.singletonList("249")))
		);

		BranchOfficeEntity branchOfficeEntityShipping = buildBranchOffice(companiesId.get(0), "249", true, true, true, "OK", null, "SP", false, "STORE");

		when(stockBusiness.findStoreWithStockWithString(any(), any(), anyList(), anyList())).thenReturn(ResponseEntity.ok(
				Arrays.asList(
						buildStockResponseItem(Arrays.asList(buildCartItem("SKU-UM", 11), buildCartItem("SKU-DOIS", 22)),
								buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", null, "RJ", false, "STORE"),
								Map.of("1001", new int[] { 21, 21})
						),
						buildStockResponseItem(Arrays.asList(buildCartItem("SKU-UM", 11), buildCartItem("SKU-DOIS", 22)),
								branchOfficeEntityShipping,
								Map.of("249", new int[] { 9999, 9999 })
						)
				)));

		when(branchOfficeService.getActiveBranchOffices(any())).thenReturn(Arrays.asList(
				buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", null, "SP", false, "STORE"),
				branchOfficeEntityShipping
		));

		when(branchOfficeService.getActiveBranchOfficesForShipping(any())).thenReturn(Collections.singletonList(branchOfficeEntityShipping));

		when(queryBusiness.quoteFromBranch(any(), any(), anyList()))
				.thenReturn(new QuotationDTO(Map.of("249", Arrays.asList(buildCartItem("SKU-DOIS", 22))),
						Map.of("249", buildQuoteResponse(22222222L, 11)), null));

		when(branchOfficeService.getBranchOffice(eq(companiesId.get(0)), eq("249"))).thenReturn(branchOfficeEntityShipping);

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(2, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "STORE-PICKUP-13-0-249", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "1001", "249");

		option = pickupOptions.getPickupOptions().get(1);

		validatePickupOptionBroker(option, "STORE-PICKUP-97-0-249", 97);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "249", "249");
	}

	@Test
	public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus_shippingTo_emptyList() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		pickupOptionsRequest.setZipcode("12345-678");

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);

		assertNotNull(pickupOptions);
        assertNotNull(pickupOptions.getPickupOptions());
        assertEquals(5, pickupOptions.getPickupOptions().size());

        PickupOption option = pickupOptions.getPickupOptions().get(0);

        validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
        validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "1101", "899");
        validatePickupOptionGeo(option, 1001);
        validatePickupOptionIntelipost(option, "11111111", 11111111L);

        option = pickupOptions.getPickupOptions().get(1);

        validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
        validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "2202", "899");
        validatePickupOptionGeo(option, 2002);
        validatePickupOptionIntelipost(option, "11111111", 11111111L);

        option = pickupOptions.getPickupOptions().get(2);

        validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
        validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "3303", "899");
        validatePickupOptionGeo(option, 3003);
        validatePickupOptionIntelipost(option, "11111111", 11111111L);

        option = pickupOptions.getPickupOptions().get(3);

        validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
        validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "4404", "899");
        validatePickupOptionGeo(option, 4004);
        validatePickupOptionIntelipost(option, "11111111", 11111111L);

        option = pickupOptions.getPickupOptions().get(4);

        validatePickupOptionBroker(option, "CD-PICKUP-13-0-899", 13);
        validatePickupOptionOrigin(option, FulfillmentMethodEnum.CD.getValue(), "5505", "899");
        validatePickupOptionGeo(option, 5005);
        validatePickupOptionIntelipost(option, "11111111", 11111111L);
	}

    @Test
    public void deliveryOptionsPorID_geoOK_branchesOK_todosSkus_shippingTo() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(0));
		//pickupOptionsRequest.setZipcode("12345-678");

		when(geolocationBusiness.getShippingToForBranches(eq(pickupOptionsRequest.getCompanyId()), eq(pickupOptionsRequest.getXApplicationName()), anyList())).thenReturn(
				Collections.singletonList(buildShippingToItem("1001", Collections.singletonList("35")))
		);

		BranchOfficeEntity branchOfficeEntityShipping = buildBranchOffice(companiesId.get(0), "35", true, true, true, "OK", null, "SP", false, "STORE");

		when(stockBusiness.findStoreWithStockWithString(any(), any(), anyList(), anyList())).thenReturn(ResponseEntity.ok(
				Arrays.asList(
						buildStockResponseItem(Arrays.asList(buildCartItem("SKU-UM", 11), buildCartItem("SKU-DOIS", 22)),
								buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", null, "RJ", false, "STORE"),
								Map.of("1001", new int[] { 21, 21})
								),
						buildStockResponseItem(Arrays.asList(buildCartItem("SKU-UM", 11), buildCartItem("SKU-DOIS", 22)),
								branchOfficeEntityShipping,
								Map.of("35", new int[] { 9999, 9999 })
						)
				)));

		when(branchOfficeService.getActiveBranchOffices(any())).thenReturn(Arrays.asList(
				buildBranchOffice(companiesId.get(0), "1001", true, true, true, "OK", null, "SP", false, "STORE"),
				branchOfficeEntityShipping
		));

		when(branchOfficeService.getActiveBranchOfficesForShipping(any())).thenReturn(Collections.singletonList(branchOfficeEntityShipping));

		when(queryBusiness.quoteFromBranch(any(), any(), anyList()))
				.thenReturn(new QuotationDTO(Map.of("35", Arrays.asList(buildCartItem("SKU-DOIS", 22))),
						Map.of("35", buildQuoteResponse(22222222L, 11)), null));

		when(branchOfficeService.getBranchOffice(eq(companiesId.get(0)), eq("35"))).thenReturn(branchOfficeEntityShipping);

		PickupOptionsReturn pickupOptions = business.getPickupOptions(pickupOptionsRequest);


		assertNotNull(pickupOptions);
		assertNotNull(pickupOptions.getPickupOptions());
		assertEquals(2, pickupOptions.getPickupOptions().size());

		PickupOption option = pickupOptions.getPickupOptions().get(0);

		validatePickupOptionBroker(option, "STORE-PICKUP-13-0-35", 13);
		validatePickupOptionOrigin(option, FulfillmentMethodEnum.STORE.getValue(), "1001", "35");
		assertEquals(DeliveryOptionsStockTypeEnum.SHIPPING_TO, option.getStockType());
    }

	@Test(expected = BranchOptionsNotFoundOnGeolocationException.class)
	public void noGeolocationResponse_throws_error() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("noGeolocationResponse_throws_error", companiesId.get(0));

		business.getPickupOptions(pickupOptionsRequest);

		fail();
	}

	@Test(expected = NoActiveBranchForPickupException.class)
	public void noActiveBranchOffice_throws_error() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("generalDeliveryOptionsRequest", companiesId.get(1));

		business.getPickupOptions(pickupOptionsRequest);

		fail();
	}

	@Test(expected = DeliveryOptionsRequestNotFoundException.class)
	public void notFoundDeliveryOptionId_throws_error() throws DeliveryOptionsRequestNotFoundException, BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException, BrokerException {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest("notFoundDeliveryOptionId_throws_error", companiesId.get(0));

		business.getPickupOptions(pickupOptionsRequest);

		fail();
	}


	private void validatePickupOptionOrigin(PickupOption pickupOption, String fulfill, String branchId, String originBranchId) {
		assertEquals(fulfill, pickupOption.getFulfillmentMethod());
		assertEquals(branchId, pickupOption.getBranchId());
		assertEquals(originBranchId, pickupOption.getOriginBranchOfficeId());
	}

	private void validatePickupOptionGeo(PickupOption pickupOption, double distance) {
		assertEquals(distance, pickupOption.getDistance(), 0.1);
	}

	private void validatePickupOptionIntelipost(PickupOption pickupOption, String methodId, Long quotationId) {
		if (methodId == null)
			assertNull(pickupOption.getDeliveryMethodId());
		else
			assertEquals(methodId, pickupOption.getDeliveryMethodId());

		if (quotationId == null)
			assertNull(pickupOption.getQuotationId());
		else
			assertEquals(quotationId, pickupOption.getQuotationId(), 0.1);
	}

	private void validatePickupOptionBroker(PickupOption pickupOption, String modalId, Integer days) {
		assertEquals(modalId, pickupOption.getDeliveryModeId());
		assertEquals(days, pickupOption.getDeliveryEstimateBusinessDays());
	}
}
