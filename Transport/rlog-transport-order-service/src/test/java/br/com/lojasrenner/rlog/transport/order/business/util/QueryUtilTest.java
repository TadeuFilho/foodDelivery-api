package br.com.lojasrenner.rlog.transport.order.business.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import br.com.lojasrenner.rlog.transport.order.business.util.QueryUtil;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryUtilTest {
	
	List<LocationStockV1Response> stockResponse = Arrays.asList(
			LocationStockV1Response.builder()
					.branchOfficeId("1001")
					.branchOfficeStatus("OK")
					.items(Arrays.asList(
							LocationStockItemV1Response.builder()
									.sku("SKU-UM")
									.amountSaleable(11)
									.build(),
							LocationStockItemV1Response.builder()
									.sku("SKU-DOIS")
									.amountSaleable(22)
									.build(),
							LocationStockItemV1Response.builder()
									.sku("SKU-TRES")
									.amountSaleable(300)
									.build()
					))
					.positionBasedOnPriority(1)
					.okCount(2)
					.okItems(Arrays.asList("SKU-UM", "SKU-DOIS"))
					.build(),
			LocationStockV1Response.builder()
					.branchOfficeId("2002")
					.branchOfficeStatus("OK")
					.items(Arrays.asList(
							LocationStockItemV1Response.builder()
									.sku("SKU-UM")
									.amountSaleable(11)
									.build(),
							LocationStockItemV1Response.builder()
									.sku("SKU-CINCO")
									.amountSaleable(55)
									.build()
					))
					.positionBasedOnPriority(2)
					.okCount(2)
					.okItems(Arrays.asList("SKU-UM", "SKU-CINCO"))
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
					.sku("SKU-QUATRO")
					.amountSaleable(11)
					.build(),
					LocationStockItemV1Response.builder()
					.sku("SKU-CINCO")
					.amountSaleable(55)
					.build(),
					LocationStockItemV1Response.builder()
					.sku("SKU-SEIS")
					.amountSaleable(55)
					.build()
					))
			.positionBasedOnPriority(3)
			.okCount(3)
			.okItems(Arrays.asList("SKU-UM", "SKU-CINCO", "SKU-SEIS"))
			.build(),
			LocationStockV1Response.builder()
					.branchOfficeId("4004")
					.branchOfficeStatus("OK")
					.items(Arrays.asList(
							LocationStockItemV1Response.builder()
									.sku("SKU-UM")
									.amountSaleable(14)
									.build(),
							LocationStockItemV1Response.builder()
									.sku("SKU-DOIS")
									.amountSaleable(14)
									.build(),
							LocationStockItemV1Response.builder()
									.sku("SKU-TRES")
									.amountSaleable(300)
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
					.positionBasedOnPriority(4)
					.okCount(4)
					.okItems(Arrays.asList("SKU-UM", "SKU-DOIS", "SKU-TRES", "SKU-QUATRO"))
					.build()
	);
	
	@Test
	public void distinctStockOverlapTest() {
		List<LocationStockV1Response> stockResponseFiltred = stockResponse.stream()
				.sorted((a, b) -> b.getOkCount() - a.getOkCount())
				.filter(QueryUtil.distinctStockOverlap(LocationStockV1Response::getOkItems, LocationStockV1Response::getPositionBasedOnPriority))
				.sorted((a, b) -> a.getPositionBasedOnPriority() - b.getPositionBasedOnPriority())
				.collect(Collectors.toList());
		
		assertEquals(2, stockResponseFiltred.size());
		assertEquals("3003", stockResponseFiltred.get(0).getBranchOfficeId());
		assertEquals(3, stockResponseFiltred.get(0).getOkItems().size());
		assertEquals("4004", stockResponseFiltred.get(1).getBranchOfficeId());
		assertEquals(4, stockResponseFiltred.get(1).getOkItems().size());
		
	}
}
