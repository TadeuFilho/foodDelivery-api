package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.ScheduleBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.BrokerException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ScheduleTypeExceptionEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.ScheduleOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.UpdateBrokerRequestParams;
import lombok.extern.log4j.Log4j2;
import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.Collections;

@Service
@Log4j2
public class ScheduleService implements UpdateBrokerRequestParams {

    @Autowired
    private ScheduleBusiness business;

    @Autowired
    private ScheduleOptionsReactiveDBInfrastructure scheduleOptionsDB;

    //TODO: tratar com um @ControllerAdvice
    public ResponseEntity<ScheduleDetailsReturn> deliveryScheduleDetailsOptions(
            String xApplicationName,
            String xCurrentDate,
            String xLocale,
            String companyId,
            String id,
            String deliveryModeId,
            int quantity,
            int fromToday
    ) throws BrokerException {
        ScheduleDetailsRequest scheduleDetailsRequest = new ScheduleDetailsRequest();
        setDefaultParams(xApplicationName, xCurrentDate, xLocale, companyId, scheduleDetailsRequest);

        scheduleDetailsRequest.setDeliveryOptionsId(id);
        scheduleDetailsRequest.setDeliveryModeId(deliveryModeId);
        scheduleDetailsRequest.setQuantity(quantity);
        scheduleDetailsRequest.setFromToday(fromToday);

        log.info("schedule details request start: {}", scheduleDetailsRequest);

        try {
            ScheduleDetailsReturn scheduleDetailsReturn = business.getScheduleDetailsOptions(scheduleDetailsRequest);
            scheduleDetailsRequest.setResponse(scheduleDetailsReturn);
            return ResponseEntity.ok(scheduleDetailsReturn);
        } catch (ResponseStatusException e) {
            setErrorOnRequest(scheduleDetailsRequest, e);
            throw e;
        } catch (Exception e) {
            ScheduleDetailsReturn scheduleDetailsReturn = getScheduleDetailsReturnWithException(scheduleDetailsRequest, e);

            if (e.getCause() instanceof ConnectTimeoutException || e.getCause() instanceof SocketTimeoutException) {
                scheduleDetailsReturn.setStatus(ScheduleTypeExceptionEnum.TIMEOUT.toString());
                return new ResponseEntity<>(scheduleDetailsReturn, HttpStatus.GATEWAY_TIMEOUT);
            }  else {
                scheduleDetailsReturn.setStatus(ScheduleTypeExceptionEnum.ERROR.toString());
                return new ResponseEntity<>(scheduleDetailsReturn, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } finally {
            try {
                scheduleDetailsRequest.setErrorFlags();
                scheduleDetailsRequest.registerFinalTimestamp();
                scheduleOptionsDB.save(scheduleDetailsRequest);
            } catch (Exception e) {
                log.error("Error saving entity:", e);
            }

            log.info("schedule details request end: {}", scheduleDetailsRequest);
        }
    }

    private ScheduleDetailsReturn getScheduleDetailsReturnWithException(ScheduleDetailsRequest scheduleDetailsRequest, Exception e) {
        setErrorOnRequest(scheduleDetailsRequest, e);

        ScheduleDetailsReturn scheduleDetailsReturn = new ScheduleDetailsReturn();
        scheduleDetailsReturn.setMessages(Collections.singletonList(e.getMessage()));
        return scheduleDetailsReturn;
    }

    private void setErrorOnRequest(ScheduleDetailsRequest scheduleDetailsRequest, Exception e) {
        scheduleDetailsRequest.setErrorMessage(e.toString());
        scheduleDetailsRequest.setErrorStack(scheduleDetailsRequest.stackToString(e.getStackTrace()));
    }

}
