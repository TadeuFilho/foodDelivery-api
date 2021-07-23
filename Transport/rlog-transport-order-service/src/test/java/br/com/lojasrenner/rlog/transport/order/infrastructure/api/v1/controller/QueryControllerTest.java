package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.QueryService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.QueryController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryControllerTest {

    @InjectMocks
    private QueryController queryController;

    @Mock
    private QueryService queryService;

    private static final String APPLICATION_NAME = "Teste unitario";
    private static final String ANY_SKU = "548860081";
    private static final String COMPANY_ID_RENNER = "001";
    private static final String X_CURRENT_DATE = Instant.now().toString();
    private static final String X_LOCALE = "pt-br";

    @Test
    public void deliveryModesQueryForShoppingCartTest() throws ExecutionException, InterruptedException {
        final Integer maxOrigins = 4;
        final Integer maxOriginsStore = 3;
        final List<String> xEagerBranches = Collections.singletonList("899");
        final List<String> blockedBranches = Collections.singletonList("900");
        final Integer combinationsTimeOut = 6000;
        final Integer combinationApproachCartSizeLimit = 5;
        final ShoppingCart body = ShoppingCart.builder()
                .items(Collections.singletonList(CartItem.builder().sku(ANY_SKU).build()))
                .build();
        final DeliveryOptionsReturn deliveryOptionsReturn = DeliveryOptionsReturn.builder()
                .deliveryOptions(Collections.singletonList(
                        DeliveryOption.builder()
                                .sku(ANY_SKU)
                                .build())
                )
                .build();

        when(queryService.deliveryModesQueryForShoppingCart(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, maxOrigins, maxOriginsStore,
                BranchesForShippingStrategyEnum.GEOLOCATION, xEagerBranches, combinationsTimeOut, combinationApproachCartSizeLimit,
                COMPANY_ID_RENNER, blockedBranches, true, false, body
        )).thenReturn(deliveryOptionsReturn);

        ResponseEntity<DeliveryOptionsReturn> response = queryController.deliveryModesQueryForShoppingCart(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, maxOrigins, maxOriginsStore,
                BranchesForShippingStrategyEnum.GEOLOCATION, xEagerBranches, combinationsTimeOut, combinationApproachCartSizeLimit,
                COMPANY_ID_RENNER, blockedBranches, true, false, body);

        verify(queryService, Mockito.times(1)).deliveryModesQueryForShoppingCart(
                APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, maxOrigins, maxOriginsStore,
                BranchesForShippingStrategyEnum.GEOLOCATION, xEagerBranches, combinationsTimeOut, combinationApproachCartSizeLimit,
                COMPANY_ID_RENNER, blockedBranches, true, false, body
        );

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertTrue(response.getBody().getDeliveryOptions().stream()
                .map(DeliveryOption::getSku)
                .allMatch(ANY_SKU::equals));
    }

    @Test
    public void getDeliveryModesQueryByIdTest() {
        final String id = "6666";
        final DeliveryOptionsReturn deliveryOptionsReturn = DeliveryOptionsReturn.builder()
                .deliveryOptions(Collections.singletonList(
                        DeliveryOption.builder()
                                .sku(ANY_SKU)
                                .build())
                )
                .build();

        when(queryService.getDeliveryModesQueryById(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, COMPANY_ID_RENNER, id))
                .thenReturn(deliveryOptionsReturn);

        ResponseEntity<DeliveryOptionsReturn> response = queryController
                .getDeliveryModesQueryById(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, COMPANY_ID_RENNER, id);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertTrue(response.getBody().getDeliveryOptions().stream()
                .map(DeliveryOption::getSku)
                .allMatch(ANY_SKU::equals));
    }

}
