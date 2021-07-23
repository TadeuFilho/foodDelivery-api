package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.PickupBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownBranchOfficeException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.PickupOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.PickupService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;

@RunWith(SpringJUnit4ClassRunner.class)
public class PickupServiceTest {

    @Mock
    private PickupBusiness business;

    @Mock
    private PickupOptionsReactiveDBInfrastructure pickupOptionsDB;

    @Mock
    private MetricsService metricsService;

    private InOrder getInOrderObject() {
        return inOrder(business, pickupOptionsDB);
    }

    @InjectMocks
    private PickupService pickupService;

    private static final String xLocale = "LOCALE";
    private static final String xApplicationName = "APP";
    private static final String xCurrentDate = "AGORA";
    private static final String COMPANY_ID = "001";
    private static final String ZIPCODE = "00000-000";

    @Test
    public void deve_salvar_resposta_pickupOptions_no_mongo() throws Exception {
        String deliveryOptionsId = "11111-22222-333333";
        String state = "SP";

        ArgumentCaptor<PickupOptionsRequest> requestCapture = ArgumentCaptor.forClass(PickupOptionsRequest.class);
        Mockito.when(business.getPickupOptions(any())).thenReturn(PickupOptionsReturn.builder().build());
        Mockito.when(pickupOptionsDB.save(requestCapture.capture())).thenReturn(new PickupOptionsRequest());

        InOrder _inOrder = getInOrderObject();

        pickupService.deliveryPickupOptions(xApplicationName, xCurrentDate, xLocale, COMPANY_ID, deliveryOptionsId, state, ZIPCODE, null, null);

        PickupOptionsRequest value = requestCapture.getValue();

        assertEquals(xLocale, value.getXLocale());
        assertEquals(xApplicationName, value.getXApplicationName());
        assertEquals(xCurrentDate, value.getXCurrentDate());
        assertEquals(COMPANY_ID, value.getCompanyId());
        assertEquals(ZIPCODE, value.getZipcode());
        assertEquals(state, value.getState());
        assertEquals(deliveryOptionsId, value.getDeliveryOptionsId());
        assertNull(value.getErrorMessage());

        _inOrder.verify(business, Mockito.times(1)).getPickupOptions(any());
        _inOrder.verify(pickupOptionsDB, Mockito.times(1)).save(any());
        _inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void deve_salvar_resposta_pickupOptions_no_mongo_mesmo_com_erro() throws Exception {
        String deliveryOptionsId = "11111-22222-333333";
        String state = "SP";
        String zipcode = "01010-101";

        ArgumentCaptor<PickupOptionsRequest> requestCapture = ArgumentCaptor.forClass(PickupOptionsRequest.class);
        Mockito.when(business.getPickupOptions(any())).thenThrow(UnknownBranchOfficeException.class);
        Mockito.when(pickupOptionsDB.save(requestCapture.capture())).thenReturn(new PickupOptionsRequest());

        InOrder _inOrder = getInOrderObject();

        try {
            pickupService.deliveryPickupOptions(xApplicationName, xCurrentDate, xLocale, COMPANY_ID, deliveryOptionsId, state, zipcode, null, null);
        } catch (Exception e) {
            PickupOptionsRequest value = requestCapture.getValue();

            assertNotNull(value.getErrorMessage());

            _inOrder.verify(business, Mockito.times(1)).getPickupOptions(any());
            _inOrder.verify(pickupOptionsDB, Mockito.times(1)).save(any());
            _inOrder.verifyNoMoreInteractions();

            return;
        }

        fail();
    }
}
