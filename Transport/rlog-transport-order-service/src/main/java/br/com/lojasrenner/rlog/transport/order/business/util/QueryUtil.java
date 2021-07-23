package br.com.lojasrenner.rlog.transport.order.business.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;

public final class QueryUtil {

	private QueryUtil() {
        //not called
    }

	public static <T> Predicate<T> distinctByKey(
	        Function<? super T, ?> keyExtractor) {

	        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
	        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

	public static <T> Predicate<T> distinctStockOverlap(
			Function<? super T, List<String>> keyExtractor, ToIntFunction<? super T> keyExtractorPriority) {

            Set<String> skusOk = new HashSet<>();
            return t -> {
                if (keyExtractorPriority.applyAsInt(t) < 0)
                   return true;

                List<String> currentStockSkus = keyExtractor.apply(t);
                int before = skusOk.size();
                skusOk.addAll(currentStockSkus);
                int after = skusOk.size();

                return before != after;
            };
    }

	public static int sortByEagerBranches(LocationStockV1Response a, LocationStockV1Response b,
			List<String> eagerBranches) {
		if(eagerBranches.contains(a.getBranchOfficeId()))
			return -1;
		else if(eagerBranches.contains(b.getBranchOfficeId()))
			return 1;

		return 0;
	}

	public static int emptyBranchOfficeId(DeliveryMode a, DeliveryMode b) {
	    if (a.getBranchOfficeId() == null) {
	        return (b.getBranchOfficeId() == null) ? 0 : -1;
	    }
	    if (b.getBranchOfficeId() == null) {
	        return 1;
	    }
	    return a.getBranchOfficeId().compareTo(b.getBranchOfficeId());
	}

	public static int fulfillmentTypeStoreFirst(DeliveryMode a, DeliveryMode b) {
		if (a.getFulfillmentMethod().equals(b.getFulfillmentMethod()))
			return 0;

		if (FulfillmentMethodEnum.STORE.isMatch(a.getFulfillmentMethod()))
			return 1;

		return 2;
	}

	public static int fulfillmentTypeCDFirst(DeliveryMode a, DeliveryMode b) {
		if (a.getFulfillmentMethod().equals(b.getFulfillmentMethod()))
			return 0;

		if (FulfillmentMethodEnum.CD.isMatch(a.getFulfillmentMethod()))
			return 1;

		return 2;
	}

	public static int lowestFreightCost(DeliveryMode a, DeliveryMode b) {
		return ((int)(a.getFreightCost() * 100)) - ((int)(b.getFreightCost() * 100));
	}

	public static int lowestDeliveryEstimate(DeliveryMode a, DeliveryMode b) {
		return a.getDeliveryEstimateBusinessDays() - b.getDeliveryEstimateBusinessDays();
	}

}
