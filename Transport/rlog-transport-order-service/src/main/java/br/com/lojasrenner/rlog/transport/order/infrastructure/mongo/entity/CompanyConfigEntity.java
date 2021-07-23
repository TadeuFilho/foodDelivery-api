package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import java.util.List;
import java.util.Map;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.CountryEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.UnavailableSkuStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;

@Component
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class CompanyConfigEntity {
	private Integer maxOrigins;
	private Integer maxOriginsStore;
	private BranchSortingEnum branchSorting;
	private UnavailableSkuStrategyEnum unavailableSkusStrategy;
	private Boolean allowFulfillWithMaxCapacityAchieved;
	private BranchesForShippingStrategyEnum branchesForShippingStrategy;
	private List<String> eagerBranches;
	private Integer combinationApproachCartSizeLimit;
	private Map<String, CompanyConfigEntity> channelConfig;
	private List<BranchOfficeEntity> ecomms;
	private Double dataLakeSendDataRate;
	private String freightCostCurrency;
	private String defaultQuotingMode;
	private TimeoutConfig timeout;
	private CountryEnum country;
	private Boolean useParallelStockFetch;
	private Integer maxCartCapacity;

	private PickupConfig pickup;

	private ShippingToConfig shippingTo;

	private FulfillmentConfig fulfillment;

	private ReOrderConfig reOrder;

	private GroupConfig groupConfig;

	private Boolean executeAsyncQueryResponseHandle;

	private Map<String, List<String>> metrics;

	private MainEcommConfig mainEcomm;
}
