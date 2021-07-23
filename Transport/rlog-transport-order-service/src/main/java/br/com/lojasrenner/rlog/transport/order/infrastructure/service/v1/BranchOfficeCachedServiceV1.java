package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownBranchOfficeException;
import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownCompanyException;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchOfficeTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeConfigurationEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.BranchOfficeMetricProperties;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.BranchOfficeResponseV1;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Log4j2
public class BranchOfficeCachedServiceV1 {

	private static final String RENNER_ID = "001";
	private static final List<String> VALID_CD_MANAGEMENTS = Arrays.asList("STORE", "CD", "CD2", "CD3", "CD4", "CD5", "CD6", "CD7", "CD8", "CD9");
	private static final String NO_CD_MANAGEMENT = "NO_CD_MANAGEMENT";
	private static final String NO_BRANCH_TYPE = "NO_BRANCH_TYPE";

	@Autowired
	private BranchClientServiceInfrastructureV1 branchClientServiceInfrastructureV1;

	@Value("${oms.pagination-branch-size}")
	private Integer size;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private LiveConfig config;

	@Autowired
	private ElasticBranchOfficeWithoutParamsService elasticService;

	private final List<String> companies = Collections.singletonList(RENNER_ID);

	@PostConstruct
	@Scheduled(fixedRateString = "${bfl.loader.ttl.branch-office:60000}",
			initialDelayString = "${bfl.loader.ttl.branch-office:60000}")
	public void loadBranches() {
		companies.forEach(this::updateCache);
	}

	public List<BranchOfficeEntity> getBranchOffices(String id) {
		return getBranchOfficeEntities(id, "branchOffice");
	}

	private List<BranchOfficeEntity> getBranchOfficeEntities(final String id, final String cacheName) {
		return Optional.ofNullable(cacheManager.getCache(cacheName))
				.map(cache -> cache.get(id))
				.map(cache -> (List<BranchOfficeEntity>) cache.get())
				.filter(Predicate.not(List::isEmpty))
				.orElseGet(() -> {
					updateCache(id);
					return Optional.ofNullable(cacheManager.getCache(cacheName))
							.map(cache -> cache.get(id))
							.map(cache -> (List<BranchOfficeEntity>) cache.get())
							.orElseGet(Collections::emptyList);
				});
	}

	public List<BranchOfficeEntity> getEcommBranchOffices(String id) {
		return getBranchOfficeEntities(id, "ecommBranchOffice");
	}

	public BranchOfficeEntity getBranchOffice(String id, String branchId) {
		List<BranchOfficeEntity> branchOffices = getBranchOffices(id);
		Optional<BranchOfficeEntity> findFirst = branchOffices.stream().filter(b -> b.getBranchOfficeId().equals(branchId)).findFirst();

		if (findFirst.isEmpty()) {
			findFirst = getEcommBranchOffices(id).stream().filter(b -> b.getBranchOfficeId().equals(branchId)).findFirst();

			if (findFirst.isEmpty()) {
				throw new UnknownBranchOfficeException("Unknown BranchOffice id: " + id + " branchId: " + branchId, branchId);
			}
		}

		return findFirst.get();
	}

	private void updateCache(String id) {
		Cache cacheBranch = cacheManager.getCache("branchOffice");
		List<BranchOfficeEntity> branchOfficeList = getBranchOfficeListForCompany(id);
		if (cacheBranch != null && !branchOfficeList.isEmpty()) {
			cacheBranch.put(id, branchOfficeList);

			Predicate<BranchOfficeEntity> filterBranchType = this::filterBranchTypeNullOrDifferentBranchOfficeTypeEnum;
			Predicate<BranchOfficeEntity> filterConfiguration = this::filterCdManagementNullOrValidCdManagementList;

			branchOfficeList.stream()
					.filter(branchOffice -> filterBranchType.test(branchOffice) ||
							filterConfiguration.test(branchOffice))
					.map(branchOffice ->
							BranchOfficeMetricProperties.builder()
									.branchOfficeId(branchOffice.getId())
									.companyId(branchOffice.getCompanyId())
									.branchType(Objects.nonNull(branchOffice.getBranchType()) ?
											branchOffice.getBranchType() :
											NO_BRANCH_TYPE)
									.cdManagement(Objects.nonNull(branchOffice.getConfiguration()) &&
											Objects.nonNull(branchOffice.getConfiguration().getCdManagement()) ?
											branchOffice.getConfiguration().getCdManagement() :
											NO_CD_MANAGEMENT)
									.build()
					).forEach(elasticService::registerMetrics);

			Cache ecommCacheBranch = cacheManager.getCache("ecommBranchOffice");
			List<BranchOfficeEntity> ecommBranchOfficeList = branchOfficeList.stream()
					.filter(b -> b.getBranchType() != null && b.getBranchType().equals(BranchOfficeTypeEnum.WEB_STORE.toString()))
					.collect(Collectors.toList());

			ecommCacheBranch.put(id, ecommBranchOfficeList);
		}

	}

	private boolean filterCdManagementNullOrValidCdManagementList(BranchOfficeEntity branchOffice) {
		return Objects.isNull(branchOffice.getConfiguration()) ||
				Objects.isNull(branchOffice.getConfiguration().getCdManagement()) ||
				!VALID_CD_MANAGEMENTS.contains(branchOffice.getConfiguration().getCdManagement());
	}

	private boolean filterBranchTypeNullOrDifferentBranchOfficeTypeEnum(BranchOfficeEntity branchOffice) {
		return Objects.isNull(branchOffice.getBranchType()) ||
				!BranchOfficeTypeEnum.getBranchOfficeList().contains(branchOffice.getBranchType());
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private List<BranchOfficeEntity> getBranchOfficeListForCompany(String id) {
		log.info("BEGIN load branchOffices for company " + id);

		List<BranchOfficeEntity> branchOfficeList = new ArrayList<>();
		try {
			for (int page = 0; ; page++) {
				ResponseEntity<BranchOfficeResponseV1> redisResponse = branchClientServiceInfrastructureV1.findByCompanyId(id, page, size);
				if (redisResponse.getStatusCodeValue() != 200) {
					log.info("ERROR get branchOffices for company " + id);
					return new ArrayList<>();
				}
				BranchOfficeResponseV1 pageBranchOffice = redisResponse.getBody();

				if (pageBranchOffice != null && !pageBranchOffice.isEmpty() && !pageBranchOffice.getContent().isEmpty())
					branchOfficeList.addAll(new ArrayList<>(pageBranchOffice.getContent()));

				if (pageBranchOffice != null && pageBranchOffice.isLast()) break;
			}
		} catch (Exception e) {
			log.info("ERROR load branchOffices for company " + id, e);
			return new ArrayList<>();
		}
		log.info("END load branchOffices for company " + id);
		return branchOfficeList;
	}

	private static List<BranchOfficeEntity> filter(List<BranchOfficeEntity> all, Predicate<BranchOfficeEntity> filter) {
		return all.stream().filter(filter).collect(Collectors.toList());
	}

	public List<BranchOfficeEntity> getActiveEcommBranchOffices(String companyId) {
		return filter(getEcommBranchOffices(companyId),
				b -> b.getConfiguration() != null
						&& b.getConfiguration().getActive() != null
						&& b.getConfiguration().getActive()
		);
	}

	public List<BranchOfficeEntity> getActiveBranchOffices(String companyId) {
		return filter(getBranchOffices(companyId),
				b -> b.getConfiguration() != null
						&& b.getConfiguration().getActive() != null
						&& b.getConfiguration().getActive()
		);
	}

	public List<BranchOfficeEntity> getActiveBranchOfficesForShipping(String companyId) {
		List<String> status = new ArrayList<>();
		status.add("OK");
		return getActiveBranchOfficesForShipping(companyId, status);
	}

	public List<BranchOfficeEntity> getActiveBranchOfficesForPickup(String companyId) {
		List<String> status = new ArrayList<>();
		status.add("OK");
		return getActiveBranchOfficesForPickup(companyId, status);
	}

	public List<BranchOfficeEntity> getActiveBranchOfficesForShipping(String companyId, List<String> validStatus) {
		return filter(getActiveBranchOffices(companyId),
				b -> b.getConfiguration().getPermission() != null
						&& b.getConfiguration().getPermission().getDoShipping() != null
						&& b.getConfiguration().getPermission().getDoShipping()
						&& b.getStatus() != null
						&& b.getStatus().getOrder() != null
						&& validStatus.contains(b.getStatus().getOrder()));
	}

	public List<BranchOfficeEntity> getActiveBranchOfficesForPickup(String companyId, List<String> validStatus) {
		return filter(getActiveBranchOffices(companyId),
				b -> b.getConfiguration().getPermission() != null
						&& b.getConfiguration().getPermission().getBranchWithdraw() != null
						&& b.getConfiguration().getPermission().getBranchWithdraw()
						&& b.getStatus() != null
						&& b.getStatus().getOrder() != null
						&& validStatus.contains(b.getStatus().getOrder()));
	}

}
