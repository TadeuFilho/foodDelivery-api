package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownCompanyException;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchClientServiceInfrastructureV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.ElasticBranchOfficeWithoutParamsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeConfigPermissionEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeConfigurationEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeStatusEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.BranchOfficeResponseV1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class BranchOfficeServiceV1Test {

	@InjectMocks
	private BranchOfficeCachedServiceV1 service;

	@Mock
	private BranchClientServiceInfrastructureV1 branchOfficeService;

	@Mock
	private CacheManager cacheManager;

	private Integer size = 3;

	@Mock
	private LiveConfig config;

	@Mock
	private ElasticBranchOfficeWithoutParamsService elasticService;

	@Before
	public void init() {
		ReflectionTestUtils.setField(service, "size", size);
		when(branchOfficeService.findByCompanyId("001", 0, size))
			.thenReturn(ResponseEntity.ok(BranchOfficeResponseV1.builder()
					.content(Arrays.asList(
							BranchOfficeEntity.builder()
								.id("001899")
								.companyId("001")
								.branchType("WEB_STORE")
								.configuration(BranchOfficeConfigurationEntity.builder()
										.permission(BranchOfficeConfigPermissionEntity.builder()
												.doShipping(true)
												.build())
										.active(true)
										.build())
								.build(),
							BranchOfficeEntity.builder()
								.id("001599")
								.companyId("001")
								.branchType("IN_STORE")
								.configuration(BranchOfficeConfigurationEntity.builder()
										.permission(BranchOfficeConfigPermissionEntity.builder()
												.doShipping(true)
												.build())
										.active(true)
										.build())
								.status(BranchOfficeStatusEntity.builder()
										.order("OK")
										.build())
								.build(),
							BranchOfficeEntity.builder()
								.id("0011001")
								.companyId("001")
								.branchType("IN_STORE")
								.configuration(BranchOfficeConfigurationEntity.builder()
										.permission(BranchOfficeConfigPermissionEntity.builder()
												.doShipping(true)
												.build())
										.active(true)
										.build())
								.status(BranchOfficeStatusEntity.builder()
										.order("OK")
										.build())
								.build(),
							BranchOfficeEntity.builder()
								.id("0013003")
								.companyId("001")
								.branchType("IN_STORE")
								.configuration(BranchOfficeConfigurationEntity.builder()
										.permission(BranchOfficeConfigPermissionEntity.builder()
												.branchWithdraw(true)
												.build())
										.active(true)
										.build())
								.status(BranchOfficeStatusEntity.builder()
										.order("OK")
										.build())
								.build()
					))
					.first(true)
					.last(false)
					.totalElements(5)
					.build()
			)
		);

		when(branchOfficeService.findByCompanyId("001", 1, size))
			.thenReturn(ResponseEntity.ok(BranchOfficeResponseV1.builder()
					.content(Arrays.asList(
							BranchOfficeEntity.builder()
								.id("0014004")
								.companyId("001")
								.branchType("IN_STORE")
								.configuration(BranchOfficeConfigurationEntity.builder()
										.permission(BranchOfficeConfigPermissionEntity.builder()
												.branchWithdraw(true)
												.doShipping(true)
												.build())
										.active(false)
										.build())
								.status(BranchOfficeStatusEntity.builder()
										.order("OK")
										.build())
								.build(),
							BranchOfficeEntity.builder()
								.id("0015005")
								.companyId("001")
								.branchType("IN_STORE")
								.configuration(BranchOfficeConfigurationEntity.builder()
										.permission(BranchOfficeConfigPermissionEntity.builder()
												.branchWithdraw(true)
												.doShipping(true)
												.build())
										.active(true)
										.build())
								.status(BranchOfficeStatusEntity.builder()
										.order("NOK")
										.build())
								.build(),
							BranchOfficeEntity.builder()
									.id("001900")
									.companyId("001")
									.branchType("WEB_STORE")
									.configuration(BranchOfficeConfigurationEntity.builder()
											.permission(BranchOfficeConfigPermissionEntity.builder()
													.doShipping(true)
													.build())
											.active(true)
											.build())
									.build(),
							BranchOfficeEntity.builder()
									.id("001901")
									.companyId("001")
									.branchType("WEB_STORE")
									.configuration(BranchOfficeConfigurationEntity.builder()
											.permission(BranchOfficeConfigPermissionEntity.builder()
													.doShipping(true)
													.build())
											.active(false)
											.build())
									.build()
					))
					.first(false)
					.last(true)
					.totalElements(5)
					.build()
			)
		);

		final ConcurrentMapCacheManager concurrentMapCacheManager = new ConcurrentMapCacheManager("branchOffice", "ecommBranchOffice");

		when(cacheManager.getCache(eq("branchOffice"))).thenReturn(concurrentMapCacheManager.getCache("branchOffice"));
		when(cacheManager.getCache(eq("ecommBranchOffice"))).thenReturn(concurrentMapCacheManager.getCache("ecommBranchOffice"));

		when(config.getConfigEcomms(any(), any(), any())).thenReturn(Collections.singletonList(BranchOfficeEntity.builder()
				.id("001899")
				.zipcode("23575-450")
				.configuration(BranchOfficeConfigurationEntity.builder()
						.active(true)
						.build())
				.build()));
		when(config.getConfigValueString(any(), any(), any(), eq(false))).thenReturn("001900");
	}

	@Test
	public void deve_retonar_lista_vazia_quando_der_problema_branchService() throws Exception {
		when(branchOfficeService.findByCompanyId("555", 0, size))
			.thenReturn(ResponseEntity.unprocessableEntity().body(BranchOfficeResponseV1.builder().build())
		);

		List<BranchOfficeEntity> branchOfficesCached = service.getBranchOffices("555");

		assertTrue(branchOfficesCached.isEmpty());
	}

	@Test
	public void deve_retonar_lista_vazia_para_company_desconhecido_getBranchOffices() throws Exception {
		List<BranchOfficeEntity> branchOfficesCached = service.getBranchOffices("999");

		assertTrue(branchOfficesCached.isEmpty());
	}

	@Test
	public void deve_retonar_vazio_branch_offices_ecomm_companyId_invalido_getEcommBranchOffices() throws UnknownCompanyException {
		//um companyId invalido tem 0 branches. Essa lista de 0 branches gera 0 ecomms. Não gera um erro.
		when(config.getConfigEcomms(any(), any(), any())).thenReturn(null);
		final List<BranchOfficeEntity> ecommBranchOffices = service.getEcommBranchOffices("999");
		assertTrue(ecommBranchOffices.isEmpty());
	}

	@Test
	public void deve_atualizar_mapa_de_branches() throws Exception {
		when(config.getConfigValueBoolean(eq("001"), eq(Optional.of("Ecommerce")), any(), eq(false))).thenReturn(Boolean.TRUE);
		when(config.getConfigValueString(eq("001"), eq(Optional.of("Ecommerce")), any(), eq(true))).thenReturn("001900");
		List<BranchOfficeEntity> branchOfficesCached = service.getBranchOffices("001");

		List<BranchOfficeEntity> ecommBranchOfficesCached = service.getEcommBranchOffices("001");

		assertEquals(8, branchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());

		assertEquals("899", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("599", branchOfficesCached.get(1).getBranchOfficeId());
		assertEquals("1001", branchOfficesCached.get(2).getBranchOfficeId());
		assertEquals("3003", branchOfficesCached.get(3).getBranchOfficeId());
		assertEquals("4004", branchOfficesCached.get(4).getBranchOfficeId());
		assertEquals("5005", branchOfficesCached.get(5).getBranchOfficeId());
		assertEquals("899", ecommBranchOfficesCached.get(0).getBranchOfficeId());
	}

	@Test
	public void nao_deve_sobrescrever_o_cache_com_uma_lista_vazia() throws Exception {
		//fluxo normal de load de branch offices
		when(config.getConfigValueBoolean(eq("001"), eq(Optional.of("Ecommerce")), any(), eq(false))).thenReturn(Boolean.TRUE);
		when(config.getConfigValueString(eq("001"), eq(Optional.of("Ecommerce")), any(), eq(true))).thenReturn("001900");
		List<BranchOfficeEntity> branchOfficesCached = service.getBranchOffices("001");

		List<BranchOfficeEntity> ecommBranchOfficesCached = service.getEcommBranchOffices("001");

		assertEquals(8, branchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());

		assertEquals("899", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("599", branchOfficesCached.get(1).getBranchOfficeId());
		assertEquals("1001", branchOfficesCached.get(2).getBranchOfficeId());
		assertEquals("3003", branchOfficesCached.get(3).getBranchOfficeId());
		assertEquals("4004", branchOfficesCached.get(4).getBranchOfficeId());
		assertEquals("5005", branchOfficesCached.get(5).getBranchOfficeId());
		assertEquals("899", ecommBranchOfficesCached.get(0).getBranchOfficeId());

		//dai chamamos novamente o update cache, retornando vazio do branch service
		when(branchOfficeService.findByCompanyId("001", 0, size))
				.thenReturn(ResponseEntity.unprocessableEntity().body(BranchOfficeResponseV1.builder().build()));
		service.loadBranches();

		//e todas as listas devem estar inalteradas no cache
		branchOfficesCached = service.getBranchOffices("001");
		ecommBranchOfficesCached = service.getEcommBranchOffices("001");

		assertEquals(8, branchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());

		assertEquals("899", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("599", branchOfficesCached.get(1).getBranchOfficeId());
		assertEquals("1001", branchOfficesCached.get(2).getBranchOfficeId());
		assertEquals("3003", branchOfficesCached.get(3).getBranchOfficeId());
		assertEquals("4004", branchOfficesCached.get(4).getBranchOfficeId());
		assertEquals("5005", branchOfficesCached.get(5).getBranchOfficeId());
		assertEquals("899", ecommBranchOfficesCached.get(0).getBranchOfficeId());
	}

	@Test
	public void deve_atualizar_mapa_de_branches_sp_off() throws Exception {
		when(config.getConfigValueBoolean(eq("001"), eq(Optional.of("Ecommerce")), any(), eq(false))).thenReturn(Boolean.FALSE);

		List<BranchOfficeEntity> branchOfficesCached = service.getBranchOffices("001");

		List<BranchOfficeEntity> ecommBranchOfficesCached = service.getEcommBranchOffices("001");


		assertEquals(8, branchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());
		assertEquals(3, ecommBranchOfficesCached.size());

		assertEquals("899", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("599", branchOfficesCached.get(1).getBranchOfficeId());
		assertEquals("1001", branchOfficesCached.get(2).getBranchOfficeId());
		assertEquals("3003", branchOfficesCached.get(3).getBranchOfficeId());
		assertEquals("4004", branchOfficesCached.get(4).getBranchOfficeId());
		assertEquals("5005", branchOfficesCached.get(5).getBranchOfficeId());
		assertEquals("900", branchOfficesCached.get(6).getBranchOfficeId());
		assertEquals("901", branchOfficesCached.get(7).getBranchOfficeId());

		assertEquals("899", ecommBranchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("900", ecommBranchOfficesCached.get(1).getBranchOfficeId());
		assertEquals("901", ecommBranchOfficesCached.get(2).getBranchOfficeId());
	}

	@Test
	public void deve_retornar_somente_ativos() throws Exception {
		List<BranchOfficeEntity> branchOfficesCached = service.getActiveBranchOffices("001");

		assertEquals(6, branchOfficesCached.size());

		assertEquals("899", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("599", branchOfficesCached.get(1).getBranchOfficeId());
		assertEquals("1001", branchOfficesCached.get(2).getBranchOfficeId());
		assertEquals("3003", branchOfficesCached.get(3).getBranchOfficeId());
		assertEquals("5005", branchOfficesCached.get(4).getBranchOfficeId());
		assertEquals("900", branchOfficesCached.get(5).getBranchOfficeId());
	}

	@Test
	public void deve_retornar_somente_ativos_ecomm() throws Exception {
		List<BranchOfficeEntity> branchOfficesCached = service.getActiveEcommBranchOffices("001");

		assertEquals(2, branchOfficesCached.size());

		assertEquals("899", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("900", branchOfficesCached.get(1).getBranchOfficeId());
	}

	@Test
	public void deve_retornar_somente_shipping() throws Exception {
		List<BranchOfficeEntity> branchOfficesCached = service.getActiveBranchOfficesForShipping("001");

		assertEquals(2, branchOfficesCached.size());

		assertEquals("599", branchOfficesCached.get(0).getBranchOfficeId());
		assertEquals("1001", branchOfficesCached.get(1).getBranchOfficeId());
	}

	@Test
	public void deve_retornar_somente_pickup() throws Exception {
		List<BranchOfficeEntity> branchOfficesCached = service.getActiveBranchOfficesForPickup("001");

		assertEquals(1, branchOfficesCached.size());

		assertEquals("3003", branchOfficesCached.get(0).getBranchOfficeId());
	}

	@Test
	public void deve_usar_o_cache() throws Exception {
		int n = 5;

		ArgumentCaptor<String> ids = ArgumentCaptor.forClass(String.class);

		when(branchOfficeService.findByCompanyId(ids.capture(), eq(0), eq(size)))
			.thenReturn(ResponseEntity.ok(BranchOfficeResponseV1.builder()
					.content(Arrays.asList(
							BranchOfficeEntity.builder()
								.id("0011001")
								.companyId("001")
								.branchType("IN_STORE")
								.build()
					))
					.first(true)
					.last(true)
					.totalElements(1)
					.build()
			)
		);

		for (int i = 0; i < n; i++)
			service.getBranchOffices("001");

		assertEquals(1, ids.getAllValues().size());
	}

	@Test
	public void deve_buscar_n_vezes() throws Exception {
		int n = 5;

		ArgumentCaptor<String> ids = ArgumentCaptor.forClass(String.class);

		when(branchOfficeService.findByCompanyId(ids.capture(), eq(0), eq(size)))
			.thenReturn(ResponseEntity.ok(BranchOfficeResponseV1.builder()
					.content(Arrays.asList(
							BranchOfficeEntity.builder()
								.id("0011001")
								.companyId("001")
								.branchType("IN_STORE")
								.build()
					))
					.first(true)
					.last(true)
					.totalElements(1)
					.build()
			)
		);

		for (int i = 0; i < n; i++)
			service.loadBranches();

		assertEquals(n, ids.getAllValues().size());
	}

}
