package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import java.util.ArrayList;
import java.util.List;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@Setter
@ToString
public class QuoteSettings {

	private Integer maxOriginsConfig;
	private Integer maxOriginsHeader;
	private Integer maxOriginsStoreConfig;
	private Integer maxOriginsStoreHeader;
	private Integer maxCombinationsTimeOutConfig;
	private Integer maxCombinationsTimeOutHeader;
	private Integer combinationApproachCartSizeLimitConfig;
	private Integer combinationApproachCartSizeLimitHeader;
	private BranchesForShippingStrategyEnum branchesForShippingStrategyHeader;
	private BranchesForShippingStrategyEnum branchesForShippingStrategyConfig;
	private List<String> eagerBranchesHeader;
	private List<String> eagerBranchesConfig;
	private List<String> blockedBranches;
	private List<String> extraBranchStatus;
	
	private Integer maxOriginsUsed;
	private Integer maxOriginsStoreUsed;
	private Integer maxCombinationsTimeOutUsed;
	private Integer combinationApproachCartSizeLimitUsed;
	private BranchesForShippingStrategyEnum branchesForShippingStrategyUsed;
	private List<String> eagerBranchesUsed;
	private Boolean reQuotePickup;

	public int getMaxOriginsUsed() {
		maxOriginsUsed = this.getMaxOriginsHeader() != null ? this.getMaxOriginsHeader() : this.getMaxOriginsConfig();

		if (maxOriginsUsed == null)
			return 0;

		return maxOriginsUsed;
	}

	public int getMaxOriginsStoreUsed() {
		maxOriginsStoreUsed = this.getMaxOriginsStoreHeader() != null ? this.getMaxOriginsStoreHeader() : this.getMaxOriginsStoreConfig();

		if (maxOriginsStoreUsed == null)
			return 0;

		return maxOriginsStoreUsed;
	}

	public int getMaxCombinationsTimeOutUsed() {
		maxCombinationsTimeOutUsed = this.getMaxCombinationsTimeOutHeader() != null ? this.getMaxCombinationsTimeOutHeader() : this.getMaxCombinationsTimeOutConfig();

		if (maxCombinationsTimeOutUsed == null)
			return 0;

		return maxCombinationsTimeOutUsed;
	}

	public BranchesForShippingStrategyEnum getBranchesForShippingStrategyUsed() {
		branchesForShippingStrategyUsed = this.getBranchesForShippingStrategyHeader() != null ? this.getBranchesForShippingStrategyHeader() : this.getBranchesForShippingStrategyConfig();
		return branchesForShippingStrategyUsed;
	}

	public List<String> getEagerBranchesUsed() {
		eagerBranchesUsed = this.getEagerBranchesHeader() != null ? this.getEagerBranchesHeader() : this.getEagerBranchesConfig();

		if (eagerBranchesUsed == null)
			return new ArrayList<>();

		return eagerBranchesUsed;
	}

	public int getCombinationApproachCartSizeLimitUsed() {
		combinationApproachCartSizeLimitUsed = this.getCombinationApproachCartSizeLimitHeader() != null ? this.getCombinationApproachCartSizeLimitHeader() : this.getCombinationApproachCartSizeLimitConfig();

		if (combinationApproachCartSizeLimitUsed == null)
			return 0;

		return combinationApproachCartSizeLimitUsed;
	}

	public List<String> getBlockedBranches() {
		if(blockedBranches == null)
			return new ArrayList<>();

		return blockedBranches;
	}


}
