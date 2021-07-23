package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.ScheduleBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.DeliveryOptionsRequestNotFoundException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.ScheduleOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.ScheduleService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;

@RunWith(SpringJUnit4ClassRunner.class)
public class ScheduleServiceTest {

    @Mock
    private ScheduleBusiness business;

    @Mock
    private ScheduleOptionsReactiveDBInfrastructure scheduleOptionsDB;

    private InOrder getInOrderObject() {
        return inOrder(business, scheduleOptionsDB);
    }

    @InjectMocks
    private ScheduleService scheduleService;

    private static final String xLocale = "LOCALE";
    private static final String xApplicationName = "APP";
    private static final String xCurrentDate = "AGORA";
    private static final String COMPANY_ID = "001";

    @Test
    public void deve_salvar_resposta_scheduleOptions_no_mongo() throws Exception {
        String deliveryOptionsId = "11111-22222-333333";
        String deliveryModeId = "CD-SCHEDULED-123-132";
        int quantity = 16;
        int fromToday = 17;

        ArgumentCaptor<ScheduleDetailsRequest> requestCapture = ArgumentCaptor.forClass(ScheduleDetailsRequest.class);
        Mockito.when(business.getScheduleDetailsOptions(any())).thenReturn(ScheduleDetailsReturn.builder().build());
        Mockito.when(scheduleOptionsDB.save(requestCapture.capture())).thenReturn(new ScheduleDetailsRequest());

        InOrder _inOrder = getInOrderObject();

        scheduleService.deliveryScheduleDetailsOptions(xApplicationName, xCurrentDate, xLocale, COMPANY_ID, deliveryOptionsId, deliveryModeId, quantity, fromToday);

        ScheduleDetailsRequest value = requestCapture.getValue();

        assertEquals(xLocale, value.getXLocale());
        assertEquals(xApplicationName, value.getXApplicationName());
        assertEquals(xCurrentDate, value.getXCurrentDate());
        assertEquals(COMPANY_ID, value.getCompanyId());
        assertEquals(deliveryOptionsId, value.getDeliveryOptionsId());
        assertEquals(deliveryModeId, value.getDeliveryModeId());
        assertEquals(quantity, value.getQuantity());
        assertEquals(fromToday, value.getFromToday());
        assertNull(value.getErrorMessage());

        _inOrder.verify(business, Mockito.times(1)).getScheduleDetailsOptions(any());
        _inOrder.verify(scheduleOptionsDB, Mockito.times(1)).save(any());
        _inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void deve_salvar_resposta_scheduleOptions_no_mongo_mesmo_com_erro() throws Exception {
        String deliveryOptionsId = "11111-22222-333333";
        String deliveryModeId = "CD-SCHEDULED-123-132";
        int quantity = 16;
        int fromToday = 17;

        ArgumentCaptor<ScheduleDetailsRequest> requestCapture = ArgumentCaptor.forClass(ScheduleDetailsRequest.class);
        Mockito.when(business.getScheduleDetailsOptions(any())).thenThrow(DeliveryOptionsRequestNotFoundException.class);
        Mockito.when(scheduleOptionsDB.save(requestCapture.capture())).thenReturn(new ScheduleDetailsRequest());

        InOrder _inOrder = getInOrderObject();

        try {
            scheduleService.deliveryScheduleDetailsOptions(xApplicationName, xCurrentDate, xLocale, COMPANY_ID, deliveryOptionsId, deliveryModeId, quantity, fromToday);
        } catch (Exception e) {
            ScheduleDetailsRequest value = requestCapture.getValue();

            assertNotNull(value.getErrorMessage());

            _inOrder.verify(business, Mockito.times(1)).getScheduleDetailsOptions(any());
            _inOrder.verify(scheduleOptionsDB, Mockito.times(1)).save(any());
            _inOrder.verifyNoMoreInteractions();

            return;
        }

        fail();
    }
}
