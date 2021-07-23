package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.PickupBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.BranchOptionsNotFoundOnGeolocationException;
import br.com.lojasrenner.rlog.transport.order.business.exception.EmptyDeliveryOptionsRequestIdException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoActiveBranchForPickupException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ReasonTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.PickupOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.UpdateBrokerRequestParams;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.PickupController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
public class PickupService implements UpdateBrokerRequestParams {

    @Autowired
    private PickupBusiness business;

    @Autowired
    private PickupOptionsReactiveDBInfrastructure pickupOptionsDB;

    @Autowired
    private MetricsService metricsService;

    public PickupOptionsReturn deliveryPickupOptions(
            String xApplicationName,
            String xCurrentDate,
            String xLocale,
            String companyId,
            String deliveryOptionsId,
            String state,
            String zipcode,
            List<String> skus,
            List<String> blockedBranches
    ) throws NoActiveBranchForPickupException, EmptyDeliveryOptionsRequestIdException, BranchOptionsNotFoundOnGeolocationException {
        PickupOptionsRequest pickupOptionsRequest = new PickupOptionsRequest();
        try {
            setDefaultParams(xApplicationName, xCurrentDate, xLocale, companyId, pickupOptionsRequest);
            pickupOptionsRequest.setDeliveryOptionsId(deliveryOptionsId.trim());
            pickupOptionsRequest.setState(state);
            pickupOptionsRequest.setZipcode(zipcode);
            pickupOptionsRequest.setSkus(skus);
            pickupOptionsRequest.setQuoteSettings(QuoteSettings.builder()
                    .blockedBranches(blockedBranches)
                    .build());

            log.info("pickup options request start: {}", pickupOptionsRequest);

            String idToValidate = deliveryOptionsId.trim().toLowerCase();
            if (idToValidate.equals("null") || idToValidate.isEmpty()) {
                metricsService.sendBadRequestMetrics(companyId, xApplicationName, ReasonTypeEnum.EMPTY_QUOTE_ID, PickupController.class.getSimpleName());
                throw new EmptyDeliveryOptionsRequestIdException();
            }

            PickupOptionsReturn pickupOptionsReturn = business.getPickupOptions(pickupOptionsRequest);
            pickupOptionsRequest.setResponse(pickupOptionsReturn);

            return pickupOptionsReturn;
        } catch (Exception e) {
            pickupOptionsRequest.setErrorMessage(e.toString());
            pickupOptionsRequest.setErrorStack(pickupOptionsRequest.stackToString(e.getStackTrace()));
            throw e;
        } finally {
            try {
                pickupOptionsRequest.setErrorFlags();
                pickupOptionsRequest.registerFinalTimestamp();
                pickupOptionsDB.save(pickupOptionsRequest);
            } catch (Exception e) {
                log.error("Error saving entity:", e);
            }

            log.info("pickup options request end: {}", pickupOptionsRequest);
        }

    }

}
