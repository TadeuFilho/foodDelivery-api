package br.com.lojasrenner.rlog.transport.order.business;

import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.buildBranchOffice;
import static br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper.buildCartItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.test.BuildHelper;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.StockServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LocationStockV1Request;

@RunWith(SpringJUnit4ClassRunner.class)
public class StockBusinessTest {

	@InjectMocks
	private StockBusiness business;
	
	@Mock
	private StockServiceV1 stockService;
	
	@Mock
	private LiveConfig config;
	
	private static final List<LocationStockV1Response> stockResponse = Arrays.asList(
			LocationStockV1Response.builder()
			.branchOfficeId("1001")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(1)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-DOIS")
						.amountSaleable(2000)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-QUATRO")
						.amountSaleable(44)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-CINCO")
						.amountSaleable(55)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("2002")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(10000)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-DOIS")
						.amountSaleable(2000)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-TRES")
						.amountSaleable(33)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-SEIS")
						.amountSaleable(66)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("3003")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(11)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-DOIS")
						.amountSaleable(22)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("4004")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(110)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-DOIS")
						.amountSaleable(220)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("5005")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(11)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-DOIS")
						.amountSaleable(200000)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("6006")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
					LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(10000000)
						.build(),
					LocationStockItemV1Response.builder()
						.sku("SKU-TRES")
						.amountSaleable(3)
						.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("7007")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(11000000)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(22000000)
					.blocked(true)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("8008")
			.branchOfficeStatus("NOK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(11000000)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(22000000)
					.build()
			))
			.build(),
		LocationStockV1Response.builder()
			.branchOfficeId("9009")
			.branchOfficeStatus("OK")
			.items(Arrays.asList(
				LocationStockItemV1Response.builder()
					.sku("SKU-UM")
					.amountSaleable(11000000)
					.build(),
				LocationStockItemV1Response.builder()
					.sku("SKU-DOIS")
					.amountSaleable(22000000)
					.build()
			))
			.build()
	);
	
	DeliveryOptionsRequest request = BuildHelper.buildDeliveryOptionsRequest("001", new ArrayList<>(), "11111-111", null, 4, 4, 3, 5000, 6);
	
	@Before
	public void init() {
		when(config.getConfigValueString(eq("001"), any(), any(), eq(false))).thenReturn(BranchSortingEnum.COST.toString());
	}
	
	@Test
	public void deve_chamar_api_estoque() {
		LocationStockV1Request locationStockRequest = LocationStockV1Request.builder()
				.skus(Arrays.asList("SKU-UM", "SKU-DOIS"))
				.branchesOfficeId(Arrays.asList("1001", "2002"))
				.build();
		
		when(stockService.postLocationStock(eq("001"), anyString(), eq(locationStockRequest))).thenReturn(ResponseEntity.ok(Arrays.asList(LocationStockV1Response.builder()
				.branchOfficeId("1001")
				.branchOfficeStatus("OK")
				.items(Arrays.asList(LocationStockItemV1Response.builder()
						.sku("SKU-UM")
						.amountSaleable(963)
						.build()))
				.build())));
		
		ResponseEntity<List<LocationStockV1Response>> findStoreWithStock = business.findStoreWithStock("001", "", Arrays.asList(
				buildCartItem("SKU-UM", 11),
				buildCartItem("SKU-DOIS", 22)
			), Arrays.asList(
				buildBranchOffice("001", "1001", true, true, true, "OK", 1, "SP", false, "STORE"),
				buildBranchOffice("001", "2002", true, true, true, "OK", 1, "SP", false, "STORE")
			)
		);
		
		assertNotNull(findStoreWithStock);
		assertTrue(findStoreWithStock.getStatusCode().is2xxSuccessful());
		assertEquals(1, findStoreWithStock.getBody().size());
		assertEquals(Integer.valueOf(963), findStoreWithStock.getBody().get(0).getItems().get(0).getAmountSaleable());
	}
	
	@Test
	public void deve_escolher_melhor_location() {
		when(config.getConfigValueString(eq("001"), any(), any(), eq(false))).thenReturn(BranchSortingEnum.COUNT.toString());
		List<String> activeBranchOfficeList = Arrays.asList("1001", "2002", "3003", "4004", "5005", "6006", "7007", "8008");
		
		List<CartItem> items = Arrays.asList(
				buildCartItem("SKU-UM", 11),
				buildCartItem("SKU-DOIS", 22)
			);
		List<GeoLocationResponseV1> geolocationItems =  new ArrayList<>();
		geolocationItems.add(BuildHelper.buildGeolocationItem("5005", 1500, true));
		LocationStockV1Response bestLocation = business.findBestLocation(items, stockResponse, activeBranchOfficeList, geolocationItems, request);
		
		assertEquals("5005", bestLocation.getBranchOfficeId());
	}
	
	@Test
	public void deve_retornar_null_se_nao_houver_branch_apta() {
		List<String> activeBranchOfficeList = Arrays.asList("8008");
		
		List<CartItem> items = Arrays.asList(
				buildCartItem("SKU-UM", 11),
				buildCartItem("SKU-DOIS", 22)
			);

		List<GeoLocationResponseV1> geolocationItems =  new ArrayList<>();
		geolocationItems.add(BuildHelper.buildGeolocationItem("5005", 1500, true));
		LocationStockV1Response bestLocation = business.findBestLocation(items, stockResponse, activeBranchOfficeList, geolocationItems, request);
		
		assertNull(bestLocation);
	}

	@Test
	public void deve_retorna_loja_mais_proxima() throws NoSuchFieldException, IllegalAccessException {
		List<String> activeBranchOfficeList = Arrays.asList("4004", "3003");

		List<GeoLocationResponseV1> geolocationItems =  new ArrayList<>();
		geolocationItems.add(BuildHelper.buildGeolocationItem("4004", 1500, true));
		geolocationItems.add(BuildHelper.buildGeolocationItem("3003", 1200, true));

		List<CartItem> items = Arrays.asList(
				buildCartItem("SKU-UM", 11),
				buildCartItem("SKU-DOIS", 22)
		);

		items.forEach((i) -> i.setCostOfGoods(39.0));
		LocationStockV1Response bestLocation = business.findBestLocation(items, stockResponse, activeBranchOfficeList, geolocationItems, request);

		assertNotNull(bestLocation);
		assertEquals("3003", bestLocation.getBranchOfficeId());
		assertEquals(2, bestLocation.getItems().size());

	}

	@Test
	public void deve_retorna_loja_maior_valor_monetario() throws NoSuchFieldException, IllegalAccessException {
		List<String> activeBranchOfficeList = Arrays.asList("7007", "3003");

		List<GeoLocationResponseV1> geolocationItems =  new ArrayList<>();
		geolocationItems.add(BuildHelper.buildGeolocationItem("7007", 1500, true));
		geolocationItems.add(BuildHelper.buildGeolocationItem("3003", 1500, true));

		List<CartItem> items = Arrays.asList(
				buildCartItem("SKU-UM", 12),
				buildCartItem("SKU-DOIS", 22)
		);

		items.forEach((i) -> i.setCostOfGoods(39.0));
		LocationStockV1Response bestLocation = business.findBestLocation(items, stockResponse, activeBranchOfficeList, geolocationItems, request);

		assertNotNull(bestLocation);
		assertEquals("3003", bestLocation.getBranchOfficeId());
		assertEquals(2, bestLocation.getItems().size());

	}

	@Test
	public void deve_retorna_loja_atendimento_completo() throws NoSuchFieldException, IllegalAccessException {
		List<String> activeBranchOfficeList = Arrays.asList("4004", "3003");

		List<GeoLocationResponseV1> geolocationItems =  new ArrayList<>();
		geolocationItems.add(BuildHelper.buildGeolocationItem("3003", 500, true));
		geolocationItems.add(BuildHelper.buildGeolocationItem("4004", 1500, true));

		List<CartItem> items = Arrays.asList(
				buildCartItem("SKU-UM", 12),
				buildCartItem("SKU-DOIS", 22)
		);

		items.forEach((i) -> i.setCostOfGoods(39.0));
		LocationStockV1Response bestLocation = business.findBestLocation(items, stockResponse, activeBranchOfficeList, geolocationItems, request);

		assertNotNull(bestLocation);
		assertEquals("4004", bestLocation.getBranchOfficeId());
		assertEquals(2, bestLocation.getItems().size());

	}
	
	@Test
	public void deve_sobrescrever_estoque_ecomm() throws NoSuchFieldException, IllegalAccessException {
		List<CartItem> itemsList = new ArrayList<>();
		itemsList.add(CartItem.builder()
				.sku("SKUA")
				.quantity(1)
				.build());
		
		itemsList.add(CartItem.builder()
				.sku("SKUB")
				.quantity(1)
				.stockStatus(StockStatusEnum.INOMNISTOCK)
				.build());
		
		List<LocationStockV1Response> stockList = new ArrayList<>();
		stockList.add(LocationStockV1Response.builder()
				.branchOfficeId("666")
				.branchOfficeStatus("OK")
				.companyId("008")
				.items(Arrays.asList(
						LocationStockItemV1Response.builder()
							.sku("SKUA")
							.amountPhysical(5)
							.amountSaleable(5)
							.blocked(true)
							.build(),
						LocationStockItemV1Response.builder()
							.sku("SKUB")
							.amountPhysical(5)
							.amountSaleable(5)
							.blocked(true)
							.build()
						))
				.build());
		
		List<BranchOfficeEntity> branchOffices = new ArrayList<>();
		branchOffices.add(BranchOfficeEntity.builder().id("008666").build());
		
		List<LocationStockV1Response> resultList = business.overrideStockQuantities(itemsList, ResponseEntity.ok(stockList), branchOffices, BranchOfficeEntity.builder().id("008666").build());

		assertEquals(1, resultList.size());
		assertEquals("666", resultList.get(0).getBranchOfficeId());
		assertEquals("OK", resultList.get(0).getBranchOfficeStatus());
		assertEquals("008", resultList.get(0).getCompanyId());
		assertEquals(2, resultList.get(0).getItems().size());
		
		LocationStockItemV1Response skuA = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUA")).findFirst().get();
		
		assertEquals(Integer.valueOf(9999), skuA.getAmountPhysical());
		assertEquals(Integer.valueOf(9999), skuA.getAmountSaleable());
		assertEquals(Boolean.valueOf(false), skuA.getBlocked());
		
		LocationStockItemV1Response skuB = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUB")).findFirst().get();
		
		assertEquals(Integer.valueOf(0), skuB.getAmountPhysical());
		assertEquals(Integer.valueOf(0), skuB.getAmountSaleable());
		assertEquals(Boolean.valueOf(true), skuB.getBlocked());
	}
	
	@Test
	public void deve_sobrescrever_estoque_ecomm_mesmo_que_nao_tenha_o_item() throws NoSuchFieldException, IllegalAccessException {
		List<CartItem> itemsList = new ArrayList<>();
		itemsList.add(CartItem.builder()
				.sku("SKUA")
				.quantity(1)
				.build());
		
		itemsList.add(CartItem.builder()
				.sku("SKUB")
				.quantity(1)
				.stockStatus(StockStatusEnum.INOMNISTOCK)
				.build());
		
		List<LocationStockV1Response> stockList = new ArrayList<>();
		stockList.add(LocationStockV1Response.builder()
				.branchOfficeId("666")
				.branchOfficeStatus("OK")
				.companyId("008")
				.items(Arrays.asList(
						LocationStockItemV1Response.builder()
							.sku("SKUA")
							.amountPhysical(5)
							.amountSaleable(5)
							.blocked(true)
							.build()
						))
				.build());
		
		List<BranchOfficeEntity> branchOffices = new ArrayList<>();
		branchOffices.add(BranchOfficeEntity.builder().id("008666").build());
		
		List<LocationStockV1Response> resultList = business.overrideStockQuantities(itemsList, ResponseEntity.ok(stockList), branchOffices, BranchOfficeEntity.builder().id("008666").build());

		assertEquals(1, resultList.size());
		assertEquals("666", resultList.get(0).getBranchOfficeId());
		assertEquals("OK", resultList.get(0).getBranchOfficeStatus());
		assertEquals("008", resultList.get(0).getCompanyId());
		assertEquals(2, resultList.get(0).getItems().size());
		
		LocationStockItemV1Response skuA = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUA")).findFirst().get();
		
		assertEquals(Integer.valueOf(9999), skuA.getAmountPhysical());
		assertEquals(Integer.valueOf(9999), skuA.getAmountSaleable());
		assertEquals(Boolean.valueOf(false), skuA.getBlocked());
		
		LocationStockItemV1Response skuB = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUB")).findFirst().get();
		
		assertEquals(Integer.valueOf(0), skuB.getAmountPhysical());
		assertEquals(Integer.valueOf(0), skuB.getAmountSaleable());
		assertEquals(Boolean.valueOf(true), skuB.getBlocked());
	}
	
	@Test
	public void deve_sobrescrever_estoque_ecomm_mesmo_que_nao_tenha_a_location() throws NoSuchFieldException, IllegalAccessException {
		List<CartItem> itemsList = new ArrayList<>();
		itemsList.add(CartItem.builder()
				.sku("SKUA")
				.quantity(1)
				.build());
		
		itemsList.add(CartItem.builder()
				.sku("SKUB")
				.quantity(1)
				.stockStatus(StockStatusEnum.INOMNISTOCK)
				.build());
		
		List<LocationStockV1Response> stockList = new ArrayList<>();
		
		List<BranchOfficeEntity> branchOffices = new ArrayList<>();
		branchOffices.add(BranchOfficeEntity.builder().id("008666").companyId("008").build());
		
		List<LocationStockV1Response> resultList = business.overrideStockQuantities(itemsList, ResponseEntity.ok(stockList), branchOffices, BranchOfficeEntity.builder().id("008666").companyId("008").build());

		assertEquals(1, resultList.size());
		assertEquals("666", resultList.get(0).getBranchOfficeId());
		assertEquals("OK", resultList.get(0).getBranchOfficeStatus());
		assertEquals("008", resultList.get(0).getCompanyId());
		assertEquals(2, resultList.get(0).getItems().size());
		
		LocationStockItemV1Response skuA = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUA")).findFirst().get();
		
		assertEquals(Integer.valueOf(9999), skuA.getAmountPhysical());
		assertEquals(Integer.valueOf(9999), skuA.getAmountSaleable());
		assertEquals(Boolean.valueOf(false), skuA.getBlocked());
		
		LocationStockItemV1Response skuB = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUB")).findFirst().get();
		
		assertEquals(Integer.valueOf(0), skuB.getAmountPhysical());
		assertEquals(Integer.valueOf(0), skuB.getAmountSaleable());
		assertEquals(Boolean.valueOf(true), skuB.getBlocked());
	}
	
	@Test
	public void nao_deve_sobrescrever_estoque_ecomm_se_nao_foi_passado_ecomm_na_consulta_de_estoque() throws NoSuchFieldException, IllegalAccessException {
		List<CartItem> itemsList = new ArrayList<>();
		itemsList.add(CartItem.builder()
				.sku("SKUA")
				.quantity(1)
				.build());
		
		itemsList.add(CartItem.builder()
				.sku("SKUB")
				.quantity(1)
				.stockStatus(StockStatusEnum.INOMNISTOCK)
				.build());
		
		List<LocationStockV1Response> stockList = new ArrayList<>();
		stockList.add(LocationStockV1Response.builder()
				.branchOfficeId("666")
				.branchOfficeStatus("OK")
				.companyId("008")
				.items(Arrays.asList(
						LocationStockItemV1Response.builder()
							.sku("SKUA")
							.amountPhysical(5)
							.amountSaleable(5)
							.blocked(true)
							.build(),
						LocationStockItemV1Response.builder()
							.sku("SKUB")
							.amountPhysical(5)
							.amountSaleable(5)
							.blocked(true)
							.build()
						))
				.build());
		
		List<BranchOfficeEntity> branchOffices = new ArrayList<>();
		
		List<LocationStockV1Response> resultList = business.overrideStockQuantities(itemsList, ResponseEntity.ok(stockList), branchOffices, BranchOfficeEntity.builder().id("008666").build());

		assertEquals(1, resultList.size());
		assertEquals("666", resultList.get(0).getBranchOfficeId());
		assertEquals("OK", resultList.get(0).getBranchOfficeStatus());
		assertEquals("008", resultList.get(0).getCompanyId());
		assertEquals(2, resultList.get(0).getItems().size());
		
		LocationStockItemV1Response skuA = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUA")).findFirst().get();
		
		assertEquals(Integer.valueOf(5), skuA.getAmountPhysical());
		assertEquals(Integer.valueOf(5), skuA.getAmountSaleable());
		assertEquals(Boolean.valueOf(true), skuA.getBlocked());
		
		LocationStockItemV1Response skuB = resultList.get(0).getItems().stream().filter(i -> i.getSku().equals("SKUB")).findFirst().get();
		
		assertEquals(Integer.valueOf(5), skuB.getAmountPhysical());
		assertEquals(Integer.valueOf(5), skuB.getAmountSaleable());
		assertEquals(Boolean.valueOf(true), skuB.getBlocked());
	}
	
	@Test
	public void deve_manter_o_CD_apos_o_stockResponse_passar_pelo_prepareStockResponse_com_eagerBranches() {
		List<CartItem> itemsList = Arrays.asList(
				BuildHelper.buildCartItem("SKY-UM", 1),
				BuildHelper.buildCartItem("SKY-DOIS", 1),
				BuildHelper.buildCartItem("SKY-TRES", 1),
				BuildHelper.buildCartItem("SKY-QUATRO", 1),
				BuildHelper.buildCartItem("SKY-CINCO", 1),
				BuildHelper.buildCartItem("SKY-SEIS", 1),
				BuildHelper.buildCartItem("SKY-SETE", 1)
		);
		
		Map<String, Integer> skuQuantityMap = new HashMap<>();
		skuQuantityMap.put("SKU-UM", 1);
		skuQuantityMap.put("SKU-DOIS", 1);
		skuQuantityMap.put("SKU-TRES", 1);
		skuQuantityMap.put("SKU-QUATRO", 1);
		skuQuantityMap.put("SKU-CINCO", 1);
		skuQuantityMap.put("SKU-SEIS", 1);
		skuQuantityMap.put("SKU-SETE", 1);
		
		List<LocationStockV1Response> stock = new ArrayList<>();
		stock.addAll(stockResponse);

		stock.add(LocationStockV1Response.builder()
				.branchOfficeId("888")
				.branchOfficeStatus("OK")
				.items(Arrays.asList(
						LocationStockItemV1Response.builder()
								.sku("SKU-DOIS")
								.amountSaleable(22)
								.build(),
						LocationStockItemV1Response.builder()
								.sku("SKU-QUATRO")
								.amountSaleable(44)
								.build(),
						LocationStockItemV1Response.builder()
								.sku("SKU-CINCO")
								.amountSaleable(55)
								.build()
				))
				.build());
		
		List<String> eagerBranches = Arrays.asList("888");
		List<String> storesInRange = Arrays.asList("1001", "2002", "3003", "888");

		List<ShippingGroupResponseV1> geolocationResponse = Arrays.asList(
				ShippingGroupResponseV1.builder()
						.priority(0)
						.name("CD")
						.branches(Arrays.asList(888))
						.build(),
				ShippingGroupResponseV1.builder()
						.priority(1)
						.name("OUTRAS-LOJAS")
						.branches(Arrays.asList(1001,2002,3003))
						.build()
		);
		
		DeliveryOptionsRequest deliveryRequest = BuildHelper.buildDeliveryOptionsRequest("111", itemsList, "11111-111", 1111L, 3, 3, 2, 5000, 5000);
		deliveryRequest.setShippingGroupResponseObject(ShippingGroupResponseObjectV1.builder().shippingGroupResponse(geolocationResponse).build());
		
		List<LocationStockV1Response> stockResponseFiltred = business.prepareStockResponse(stock, deliveryRequest, storesInRange, skuQuantityMap, eagerBranches);
		
		String skuFingerPrintCD = "SKU-UM:NOK/SKU-CINCO:OK/SKU-SETE:NOK/SKU-QUATRO:OK/SKU-SEIS:NOK/SKU-DOIS:OK/SKU-TRES:NOK/";
		
		assertNotNull(stockResponseFiltred);
		assertEquals("888", stockResponseFiltred.get(0).getBranchOfficeId());
		assertEquals(skuFingerPrintCD, stockResponseFiltred.get(0).getFingerPrint());
		assertEquals(3, stockResponseFiltred.get(0).getOkCount());
	}

	@Test
	public void deve_manter_o_CD_apos_o_stockResponse_passar_pelo_prepareStockResponse_sem_eagerBranches() {
		List<CartItem> itemsList = Arrays.asList(
				BuildHelper.buildCartItem("SKY-UM", 1),
				BuildHelper.buildCartItem("SKY-DOIS", 1),
				BuildHelper.buildCartItem("SKY-TRES", 1),
				BuildHelper.buildCartItem("SKY-QUATRO", 1),
				BuildHelper.buildCartItem("SKY-CINCO", 1),
				BuildHelper.buildCartItem("SKY-SEIS", 1),
				BuildHelper.buildCartItem("SKY-SETE", 1)
		);

		Map<String, Integer> skuQuantityMap = new HashMap<>();
		skuQuantityMap.put("SKU-UM", 1);
		skuQuantityMap.put("SKU-DOIS", 1);
		skuQuantityMap.put("SKU-TRES", 1);
		skuQuantityMap.put("SKU-QUATRO", 1);
		skuQuantityMap.put("SKU-CINCO", 1);
		skuQuantityMap.put("SKU-SEIS", 1);
		skuQuantityMap.put("SKU-SETE", 1);

		List<LocationStockV1Response> stock = new ArrayList<>();
		stock.addAll(stockResponse);

		stock.add(LocationStockV1Response.builder()
				.branchOfficeId("888")
				.branchOfficeStatus("OK")
				.items(Arrays.asList(
						LocationStockItemV1Response.builder()
								.sku("SKU-DOIS")
								.amountSaleable(22)
								.build(),
						LocationStockItemV1Response.builder()
								.sku("SKU-QUATRO")
								.amountSaleable(44)
								.build(),
						LocationStockItemV1Response.builder()
								.sku("SKU-CINCO")
								.amountSaleable(55)
								.build()
				))
				.build());

		List<String> storesInRange = Arrays.asList("888", "1001", "2002", "3003");

		List<ShippingGroupResponseV1> geolocationResponse = Arrays.asList(
				ShippingGroupResponseV1.builder()
						.priority(0)
						.name("CD")
						.branches(Arrays.asList(888))
						.build(),
				ShippingGroupResponseV1.builder()
						.priority(1)
						.name("OUTRAS-LOJAS")
						.branches(Arrays.asList(1001,2002,3003))
						.build()
		);

		DeliveryOptionsRequest deliveryRequest = BuildHelper.buildDeliveryOptionsRequest("111", itemsList, "11111-111", 1111L, 3, 3, 2, 5000, 5000);
		deliveryRequest.setShippingGroupResponseObject(ShippingGroupResponseObjectV1.builder().shippingGroupResponse(geolocationResponse).build());

		List<LocationStockV1Response> stockResponseFiltred = business.prepareStockResponse(stock, deliveryRequest, storesInRange, skuQuantityMap, new ArrayList<>());

		String skuFingerPrintCD = "SKU-UM:NOK/SKU-CINCO:OK/SKU-SETE:NOK/SKU-QUATRO:OK/SKU-SEIS:NOK/SKU-DOIS:OK/SKU-TRES:NOK/";

		assertNotNull(stockResponseFiltred);
		assertEquals("888", stockResponseFiltred.get(0).getBranchOfficeId());
		assertEquals(skuFingerPrintCD, stockResponseFiltred.get(0).getFingerPrint());
		assertEquals(3, stockResponseFiltred.get(0).getOkCount());
	}
}
