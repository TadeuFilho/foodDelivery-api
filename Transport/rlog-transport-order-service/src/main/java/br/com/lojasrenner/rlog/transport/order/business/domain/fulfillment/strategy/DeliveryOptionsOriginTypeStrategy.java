package br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.strategy;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.OriginPreview;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@FunctionalInterface
public interface DeliveryOptionsOriginTypeStrategy {

	Map<String, List<CartItemWithMode>> getMapCartItemWithMode(List<CartItemWithMode> fulfillmentRequestCartOrderItems,
	                                                           String fulfillmentRequestMainBranch,
	                                                           Optional<OriginPreview> reQuoteOriginItem,
	                                                           Optional<OriginPreview> unavailableOriginItem,
	                                                           Optional<OriginPreview> newOriginItem);
}
