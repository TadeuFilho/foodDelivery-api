package br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment;

public class OriginBusiness {

	private OriginBusiness() { throw new IllegalStateException("Utility class"); }

	public static DeliveryOptionsOriginTypeStrategyEnum getOrigin(final String newOriginBranchId,
	                                                              final String ecommBranchOfficeId,
	                                                              final Boolean reOrderActive,
	                                                              final Integer skusSize,
	                                                              final Integer reQuoteOriginSkusSize,
	                                                              final Integer newOriginSkuSize,
	                                                              final Integer unavailableOriginSkusSize) {

		if (reQuoteOriginSkusSize > 0 && skusSize.equals(reQuoteOriginSkusSize)) {
			return DeliveryOptionsOriginTypeStrategyEnum.OK_SAME_ORIGIN;
		} else if (newOriginSkuSize > 0 && skusSize.equals(newOriginSkuSize)) {
			if (shouldAllowReOrder(newOriginBranchId, ecommBranchOfficeId, reOrderActive)) {
				return DeliveryOptionsOriginTypeStrategyEnum.OK_NEW_ORIGIN;
			} else {
				return DeliveryOptionsOriginTypeStrategyEnum.NO_ORIGIN;
			}
		} else if (unavailableOriginSkusSize > 0 && skusSize.equals(unavailableOriginSkusSize)) {
			return DeliveryOptionsOriginTypeStrategyEnum.NO_ORIGIN;
		} else {
			if (newOriginSkuSize > 0 && shouldAllowReOrder(newOriginBranchId, ecommBranchOfficeId, reOrderActive)) {
				return DeliveryOptionsOriginTypeStrategyEnum.PARTIAL_NEW_ORIGIN;
			} else if (reQuoteOriginSkusSize > 0) {
				return DeliveryOptionsOriginTypeStrategyEnum.PARTIAL_SAME_ORIGIN;
			} else {
				return DeliveryOptionsOriginTypeStrategyEnum.PARTIAL_NO_ORIGIN;
			}
		}
	}

	private static boolean shouldAllowReOrder(final String branchId, final String ecommBranchOfficeId, final Boolean reOrderActive) {
		return (reOrderActive != null && reOrderActive.booleanValue()) || ecommBranchOfficeId.equals(branchId);
	}

}
