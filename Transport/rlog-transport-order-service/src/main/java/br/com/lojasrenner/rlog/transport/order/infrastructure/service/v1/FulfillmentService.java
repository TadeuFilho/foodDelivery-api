package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.FulfillmentBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.InvalidModalIdException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoQuotationAvailableForFulfillment;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.CartOrderResult;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DataLakeTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsErrorEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ReasonTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.FulfillmentReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.UpdateBrokerRequestParams;
import br.com.lojasrenner.exception.BadRequestException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.FulfillmentController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartOrder;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Log4j2
public class FulfillmentService implements UpdateBrokerRequestParams {

    @Autowired
    private FulfillmentBusiness business;

    @Autowired
    private FulfillmentReactiveDBInfrastructure fulfillmentDB;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private DataLakeService dataLakeService;

    public CartOrderResult deliveryFulfillmentForShoppingCart(
            String xApplicationName,
            String xCurrentDate,
            String xLocale,
            List<String> blockedBranches,
            String companyId,
            CartOrder cartOrder
    ) throws NoQuotationAvailableForFulfillment {
        FulfillmentRequest fulfillmentRequest = new FulfillmentRequest();
        try {
            setDefaultParams(xApplicationName, xCurrentDate, xLocale, companyId, fulfillmentRequest);
            fulfillmentRequest.setQuoteSettings(QuoteSettings.builder().blockedBranches(blockedBranches).build());
            fulfillmentRequest.setCartOrder(cartOrder);

            log.info("fulfillment request start: {}", fulfillmentRequest);

            prepareCartOrder(cartOrder, companyId, fulfillmentRequest);

            CartOrderResult deliveryFullfilmentForShoppingCart = business.getSimplifiedDeliveryFulfillmentForShoppingCart(fulfillmentRequest);
            fulfillmentRequest.setResponse(deliveryFullfilmentForShoppingCart);

            return deliveryFullfilmentForShoppingCart;
        } catch (Exception e) {
            fulfillmentRequest.setErrorMessage(e.toString());
            fulfillmentRequest.setErrorStack(fulfillmentRequest.stackToString(e.getStackTrace()));
            throw e;
        } finally {
            try {
                fulfillmentRequest.setErrorFlags();
                fulfillmentRequest.calculateStatistics();
                metricsService.sendAvailabilityMetricsForFulfill(fulfillmentRequest);
                String transactionId = dataLakeService.send(companyId, Optional.ofNullable(xApplicationName), fulfillmentRequest, DataLakeTypeEnum.FULFILLMENT);
                fulfillmentRequest.setTransactionId(transactionId);
                fulfillmentDB.save(fulfillmentRequest);
            } catch (Exception e) {
                log.error("Error saving entity:", e);
            }

            log.info("fulfillment request end: {}", fulfillmentRequest);
        }
    }

    private void prepareCartOrder(CartOrder cartOrder, String companyId, FulfillmentRequest fulfillmentRequest) {
        if (cartOrder == null || cartOrder.getItems() == null || cartOrder.getItems().isEmpty()) {
            fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
            throw new BadRequestException("cartOrder cannot be null and must have at least 1 item", "400");
        }

        validate(cartOrder, companyId, fulfillmentRequest);

        cartOrder.getItems().forEach(i -> {
            if (i.getBranchOfficeId() != null && i.getBranchOfficeId() == 0)
                //se mandarem 0 no branchOfficeId vamos considerar null
                i.setBranchOfficeId(null);
        });
    }

    private void validate(CartOrder cartOrder, String companyId, FulfillmentRequest fulfillmentRequest) {
        if (cartOrder == null) {
            metricsService.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.ITEMS_IS_NULL, FulfillmentController.class.getSimpleName());
            fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
            throw new BadRequestException("cartOrder cannot be null", "400");
        }

        if (cartOrder.getDestination() == null || cartOrder.getDestination().getZipcode() == null || Strings.isBlank(cartOrder.getDestination().getZipcode())) {
            metricsService.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.ZIPCODE_IS_NULL, FulfillmentController.class.getSimpleName());
            fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
            throw new BadRequestException("zipcode cannot be null or empty", "400");
        }

        for (CartItemWithMode item : cartOrder.getItems()) {
            if (item.getModalId() == null) {
                metricsService.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.MODAL_ID_IS_NULL, FulfillmentController.class.getSimpleName());
                fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
                throw new InvalidModalIdException("modalId cannot be null. item: " + item.getSku());
            }
        }
    }

}
