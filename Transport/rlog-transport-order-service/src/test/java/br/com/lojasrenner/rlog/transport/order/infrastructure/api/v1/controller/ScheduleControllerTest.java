package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.business.exception.BrokerException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.ScheduleService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.ScheduleController;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    private static final String APPLICATION_NAME = "Teste unitario";
    private static final String COMPANY_ID_RENNER = "001";
    private static final String X_CURRENT_DATE = Instant.now().toString();
    private static final String X_LOCALE = "pt-br";

    @Test
    public void deliveryScheduleDetailsOptionsTest() throws BrokerException {
        final String cartId = "5656565656";
        final String deliveryModeId = "98988989";
        final ScheduleDetailsReturn bodyResponse = ScheduleDetailsReturn.builder().build();

        when(scheduleService.deliveryScheduleDetailsOptions(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, COMPANY_ID_RENNER, cartId, deliveryModeId, 1, 2))
                .thenReturn(ResponseEntity.ok(bodyResponse));

        ResponseEntity<ScheduleDetailsReturn> response = scheduleController.deliveryScheduleDetailsOptions(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, COMPANY_ID_RENNER, cartId, deliveryModeId, 1, 2);

        Assert.assertEquals(bodyResponse, response.getBody());
    }

}
