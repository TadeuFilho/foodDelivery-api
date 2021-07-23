package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.business.exception.NoQuotationAvailableForFulfillment;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.CartOrderResult;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FulfillmentService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.FulfillmentController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartOrder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class FulfillmentControllerTest {

    @InjectMocks
    private FulfillmentController fulfillmentController;

    @Mock
    private FulfillmentService fulfillmentService;

    private static final String APPLICATION_NAME = "Teste unitario";
    private static final String COMPANY_ID_RENNER = "001";
    private static final String X_CURRENT_DATE = Instant.now().toString();
    private static final String X_LOCALE = "pt-br";

    @Test
    public void deliveryFulfillmentForShoppingCartTest() throws NoQuotationAvailableForFulfillment {
        final String id = "6666";
        final CartOrderResult cartOrderResult = CartOrderResult.builder()
                .id(id)
                .build();

        final CartOrder body = new CartOrder();

        when(fulfillmentService.deliveryFulfillmentForShoppingCart(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE,
                Collections.emptyList(), COMPANY_ID_RENNER, body))
                .thenReturn(cartOrderResult);

        ResponseEntity<CartOrderResult> response = fulfillmentController.deliveryFulfillmentForShoppingCart(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE,
                Collections.emptyList(), COMPANY_ID_RENNER, body);

        verify(fulfillmentService, times(1)).deliveryFulfillmentForShoppingCart(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE,
                Collections.emptyList(), COMPANY_ID_RENNER, body);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals(id, response.getBody().getId());
    }

}
