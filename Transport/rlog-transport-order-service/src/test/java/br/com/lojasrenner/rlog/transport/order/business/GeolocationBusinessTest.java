package br.com.lojasrenner.rlog.transport.order.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.CountryEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseObjectV1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.GeolocationBusiness;
import br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.GeoLocationServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

@RunWith(SpringJUnit4ClassRunner.class)
public class GeolocationBusinessTest {

	@InjectMocks
	private GeolocationBusiness business;
	
	@Mock
	private BranchOfficeCachedServiceV1 branchOfficeService;
	
	@Mock
	private GeoLocationServiceV1 geolocationService;
	
	@Mock
	private EcommBusiness ecommBusiness;

	@Mock
	private LiveConfig config;
	
	Map<String, List<Integer>> branchesList = Map.of(
			"SP",
			Arrays.asList(1001, 2002, 6006, 8008),
			"BRASIL",
			Arrays.asList(3003, 5005)
	);
	
	List<GeoLocationResponseV1> geolocationInRangeResponse = Arrays.asList(
			BuildHelper.buildGeolocationItem("1001", 1001, true),
			BuildHelper.buildGeolocationItem("2002", 2002, true),
			BuildHelper.buildGeolocationItem("6006", 6006, true),
			BuildHelper.buildGeolocationItem("8008", 8008, true)
	);
	
	@Before
	public void init() {
		List<BranchOfficeEntity> branchOfficeList = Arrays.asList(
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "1001", true, true, true, "OK", 1, "SP", false, "STORE"), 1001, -1001),
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "2002", true, true, true, "OK", 1, "SP", false, "STORE"), 2002, -2002),
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "3003", true, true, true, "OK", 1, "RJ", false, "STORE"), 3003, -3003),
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "4004", true, true, true, "OK", 1, "MG", false, "STORE"), 4004, -4004),
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "5005", true, true, true, "OK", 1, "BA", false, "STORE"), 5005, -5005),
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "6006", true, true, true, "OK", 1, "SP", false, "STORE"), 6006, -6006),
				BuildHelper.addGeolocation(BuildHelper.buildBranchOffice("001", "8008", true, true, true, "OK", 1, "SP", false, "STORE"), 8008, -8008)
		);
		
		ResponseEntity<List<ShippingGroupResponseV1>> shippingGroupList = ResponseEntity.ok(Arrays.asList(
				BuildHelper.buildShippingGroup("001", "1001", "SP", 1, branchesList.get("SP"), 1001, 2002),
				BuildHelper.buildShippingGroup("001", "1001", "BRASIL", 1, branchesList.get("BRASIL"), 1001, 9009)
		));
		
		when(geolocationService.getGroupsByZipcode(eq("001"), anyString(), anyString())).thenReturn(shippingGroupList);
		
		when(branchOfficeService.getBranchOffices(eq("001"))).thenReturn(branchOfficeList);
		when(branchOfficeService.getActiveBranchOffices(eq("001"))).thenReturn(branchOfficeList);
		
		when(geolocationService.getClosestStoresInState(eq("001"), anyString(), eq("11111-111"))).thenReturn(ResponseEntity.ok(Arrays.asList(
				BuildHelper.buildGeolocationItem("1001", 1001, true),
				BuildHelper.buildGeolocationItem("2002", 2002, true),
				BuildHelper.buildGeolocationItem("6006", 6006, false),
				BuildHelper.buildGeolocationItem("7007", 7007, false),
				BuildHelper.buildGeolocationItem("8008", 8, false)
		)));

		when(config.getConfigValueString(eq("001"), any(), any(), eq(false))).thenReturn(CountryEnum.BR.toString());
		
		when(geolocationService.getClosestStoresInRange(eq("001"), anyString(), anyString())).thenReturn(ResponseEntity.ok(geolocationInRangeResponse));
		when(ecommBusiness.getEcommBranchOffice(eq("001"), eq("Ecommerce"))).thenReturn(BuildHelper.buildBranchOffice("001", "888", true, false, true, "OK", 1, "RJ", true, "CD"));
		
		when(geolocationService.getStoresByState(eq("001"), anyString(), eq("RJ"))).thenReturn(ResponseEntity.ok(Arrays.asList("3003")));
	}
	
	@Test
	public void deve_buscar_geolocation_data() {
		DeliveryOptionsRequest request = new DeliveryOptionsRequest();
		request.setCompanyId("001");
		request.setShoppingCart(ShoppingCart.builder()
				.destination(CartDestination.builder()
						.zipcode("11111-111")
						.build())
				.build());
		
		List<GeoLocationResponseV1> geolocationResponse = business.getBranchesForPickup(request.getCompanyId(), "", request.getDestinationZipcode());
	
		assertNotNull(geolocationResponse);
		assertEquals(5, geolocationResponse.size());
		assertNull(request.getExceptions());
		
		GeoLocationResponseV1 item = geolocationResponse.get(0);
		
		assertEquals("1001", item.getBranchOfficeId());
		assertEquals(1001, item.getDistance());
		assertEquals("1001", item.getSettings().get("latitude"));
		assertEquals("-1001", item.getSettings().get("longitude"));
		
		item = geolocationResponse.get(1);
		
		assertEquals("2002", item.getBranchOfficeId());
		assertEquals(2002, item.getDistance());
		assertEquals("2002", item.getSettings().get("latitude"));
		assertEquals("-2002", item.getSettings().get("longitude"));
		
		item = geolocationResponse.get(2);
		
		assertEquals("6006", item.getBranchOfficeId());
		assertEquals(6006, item.getDistance());
		assertEquals("6006", item.getSettings().get("latitude"));
		assertEquals("-6006", item.getSettings().get("longitude"));
		
		item = geolocationResponse.get(3);
		
		assertEquals("7007", item.getBranchOfficeId());
		assertEquals(7007, item.getDistance());
		assertNull(item.getSettings());
		
		item = geolocationResponse.get(4);
		
		assertEquals("8008", item.getBranchOfficeId());
		assertEquals(8, item.getDistance());
		assertEquals("8008", item.getSettings().get("latitude"));
		assertEquals("-8008", item.getSettings().get("longitude"));
	}
	
	@Test
	public void deve_buscar_geolocation_data_por_estado() {
		List<GeoLocationResponseV1> geolocationResponse = business.getGeolocationResponseForState("001", "", "RJ");
	
		assertNotNull(geolocationResponse);
		assertEquals(1, geolocationResponse.size());
		
		GeoLocationResponseV1 item = geolocationResponse.get(0);
		
		assertEquals("3003", item.getBranchOfficeId());
		assertEquals(0, item.getDistance());
		assertNull(item.getSettings());
	}
	
	@Test
	public void deve_tratar_erro_geolocation_data_por_estado() {
		when(geolocationService.getStoresByState(eq("001"), anyString(), eq("RJ"))).thenThrow(RuntimeException.class);
		
		List<GeoLocationResponseV1> geolocationResponse = business.getGeolocationResponseForState("001", "", "RJ");
		
		assertNotNull(geolocationResponse);
		assertEquals(0, geolocationResponse.size());
	}
	
	@Test
	public void deve_buscar_por_zipcode_range() {
		QuoteSettings settings = QuoteSettings.builder().branchesForShippingStrategyHeader(BranchesForShippingStrategyEnum.ZIPCODE_RANGE).build();


		List<ShippingGroupResponseV1> zipcodeRangeResponse = business.getShippingGroups("001", "Ecommerce", "1010", settings, null);
		
		assertNotNull(zipcodeRangeResponse);
		assertEquals(2, zipcodeRangeResponse.size());
		
		assertEquals("SP", zipcodeRangeResponse.get(0).getName());
		assertEquals(4, zipcodeRangeResponse.get(0).getBranches().size());
		assertEquals("BRASIL", zipcodeRangeResponse.get(1).getName());
		assertEquals(2, zipcodeRangeResponse.get(1).getBranches().size());
	}
	
	@Test
	public void deve_buscar_por_geolocation() {
		QuoteSettings settings = QuoteSettings.builder().branchesForShippingStrategyHeader(BranchesForShippingStrategyEnum.GEOLOCATION).build();

		List<ShippingGroupResponseV1> geolocationResponse = business.getShippingGroups("001", "Ecommerce", "1010", settings, new DeliveryOptionsRequest());
		
		assertNotNull(geolocationResponse);
		assertEquals(2, geolocationResponse.size());
		
		assertEquals("GEOLOCATION", geolocationResponse.get(0).getName());
		assertEquals(4, geolocationResponse.get(0).getBranches().size());
		assertEquals("GEOLOCATION-ECOMM", geolocationResponse.get(1).getName());
		assertEquals(1, geolocationResponse.get(1).getBranches().size());
	}
	
	@Test
	public void deve_buscar_lojas_em_range() {
		List<GeoLocationResponseV1> geolocationResponse = business.getBranchesForShipping("001", "", "1010");
		
		GeoLocationResponseV1 item = geolocationResponse.get(0);
		
		assertNotNull(geolocationResponse);
		assertEquals(4, geolocationResponse.size());
		assertEquals("1001", item.getBranchOfficeId());
		assertTrue(item.isInRange());
	}
	
	
}
