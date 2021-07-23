package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.business.exception.BranchOptionsNotFoundOnGeolocationException;
import br.com.lojasrenner.rlog.transport.order.business.exception.EmptyDeliveryOptionsRequestIdException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoActiveBranchForPickupException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.PickupService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.PickupController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

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
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class PickupControllerTest {

    @Mock
    private PickupService pickupService;

    @InjectMocks
    private PickupController pickupController;

    private static final String APPLICATION_NAME = "Teste unitario";
    private static final String COMPANY_ID_RENNER = "001";
    private static final String X_CURRENT_DATE = Instant.now().toString();
    private static final String X_LOCALE = "pt-br";
    private static final String ANY_SKU = "548860081";

    @Test
    public void deliveryPickupOptionsTest() throws EmptyDeliveryOptionsRequestIdException, NoActiveBranchForPickupException, BranchOptionsNotFoundOnGeolocationException {
        final String deliveryOptionsId = "6666";
        final String state = "RS";
        final String zipCode = "93000000";
        final List<String> skus = Collections.singletonList(ANY_SKU);
        final String branchId = "40";
        final String originBranchOfficeId = "41";

        final PickupOptionsReturn pickupOptionsReturn = PickupOptionsReturn.builder()
                .pickupOptions(Collections.singletonList(PickupOption.builder()
                        .branchId(branchId)
                        .originBranchOfficeId(originBranchOfficeId)
                        .build()))
                .build();

        when(pickupService.deliveryPickupOptions(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, COMPANY_ID_RENNER, deliveryOptionsId, state, zipCode, skus, null))
                .thenReturn(pickupOptionsReturn);

        final ResponseEntity<PickupOptionsReturn> response = pickupController
                .deliveryPickupOptions(APPLICATION_NAME, X_CURRENT_DATE, X_LOCALE, COMPANY_ID_RENNER, deliveryOptionsId, state, zipCode, skus, null);

        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertTrue(response.getBody().getPickupOptions().stream().map(PickupOption::getBranchId).allMatch(branchId::equals));
        Assert.assertTrue(response.getBody().getPickupOptions().stream().map(PickupOption::getOriginBranchOfficeId).allMatch(originBranchOfficeId::equals));
    }


}
