package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsAvailabilityEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsErrorEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsOriginTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsStockTypeEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryRequestStatistics {
	
	private int skuCount;
	private int availableSkuCount;
	private DeliveryOptionsAvailabilityEnum availability;
	private DeliveryOptionsErrorEnum reason;
	private DeliveryOptionsOriginTypeEnum originType;
	private DeliveryOptionsStockTypeEnum stockType;
	private String branchId;
	private String skusWithProblems;
	private Boolean combinationTimeOut;
	private Boolean passedOnPriorityGroup;
	private Boolean passedOnExtraGroup;
	private List<String> attemptedCombinations;
	private Map<String, Integer> counterMap;
	private List<BranchWithError> branchesWithErrors = new ArrayList<>();

	public void incrementCounter(String key) {
		if (counterMap == null)
			counterMap = new HashMap<>();
		
		counterMap.compute(key, (k, v) -> v == null ? Integer.valueOf(1) : v + 1);
	}
	
	public boolean checkAndAddIfItIsTheFirstTimeAttemptingThisCombination(String combination) {
		if (attemptedCombinations == null)
			attemptedCombinations = new ArrayList<>();
		
		if (attemptedCombinations.contains(combination))
			return false;
		else {
			attemptedCombinations.add(combination);
			return true;
		}
	}

}
