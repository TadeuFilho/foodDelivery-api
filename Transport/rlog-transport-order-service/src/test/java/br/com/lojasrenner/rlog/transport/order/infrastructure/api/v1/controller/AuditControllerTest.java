package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.FulfillmentReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.PickupOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.ScheduleOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.audit.DatabaseDocument;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.AuditController;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuditControllerTest {

    @Mock
    private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

    @Mock
    private PickupOptionsReactiveDBInfrastructure pickupOptionsDB;

    @Mock
    private ScheduleOptionsReactiveDBInfrastructure scheduleOptionsDB;

    @Mock
    private FulfillmentReactiveDBInfrastructure fulfillmentDB;

    @Mock
    private DatabaseDocument databaseDocument;

    @InjectMocks
    private AuditController auditController;

    private static final String EXTERNAL_CODE = "111";
    private static final String ID = "123-abc";

    @Test
    public void findIdsByExternalCodeTest() {
        final List<DatabaseDocument> databaseDocuments = Collections.singletonList(databaseDocument);
        Mockito.when(deliveryOptionsDB.findIdsForExternalCode(EXTERNAL_CODE))
                .thenReturn(databaseDocuments);
        final ResponseEntity<List<DatabaseDocument>> response = auditController.findIdsByExternalCode(EXTERNAL_CODE);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(databaseDocuments, response.getBody());
    }

    @Test
    public void findIdsByExternalCodeWhenEmptyDocumentsTest() {
        final List<DatabaseDocument> databaseDocuments = Collections.emptyList();
        Mockito.when(deliveryOptionsDB.findIdsForExternalCode(EXTERNAL_CODE))
                .thenReturn(databaseDocuments);
        final ResponseEntity<List<DatabaseDocument>> response = auditController.findIdsByExternalCode(EXTERNAL_CODE);
        Assert.assertTrue(response.getStatusCode().is4xxClientError());
        Assert.assertNull(response.getBody());
    }

    @Test
    public void findByQuoteIdTest() {
        final DeliveryOptionsRequest deliveryOptions = new DeliveryOptionsRequest();
        deliveryOptions.setId("123");
        final Optional<DeliveryOptionsRequest> deliveryOptionsRequest = Optional.of(deliveryOptions);
        Mockito.when(deliveryOptionsDB.findById(ID))
                .thenReturn(deliveryOptionsRequest);
        final ResponseEntity<DeliveryOptionsRequest> response = auditController.findByQuoteId(ID);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(deliveryOptions, response.getBody());
    }

    @Test
    public void findByQuoteIdWhenNotHaveDeliveryOptionsTest() {
        Mockito.when(deliveryOptionsDB.findById(ID))
                .thenReturn(Optional.empty());
        final ResponseEntity<DeliveryOptionsRequest> response = auditController.findByQuoteId(ID);
        Assert.assertTrue(response.getStatusCode().is4xxClientError());
        Assert.assertNull(response.getBody());
    }

    @Test
    public void findPickupsForQuoteTest() {
        final List<PickupOptionsRequest> pickupOptionsRequests = Collections.singletonList(new PickupOptionsRequest());
        Mockito.when(pickupOptionsDB.findByDeliveryOptionsId(ID))
                .thenReturn(pickupOptionsRequests);
        final ResponseEntity<List<PickupOptionsRequest>> response = auditController.findPickupsForQuote(ID);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(pickupOptionsRequests, response.getBody());
    }

    @Test
    public void findPickupsForQuoteWhenEmptyTest() {
        Mockito.when(pickupOptionsDB.findByDeliveryOptionsId(ID))
                .thenReturn(Collections.emptyList());
        final ResponseEntity<List<PickupOptionsRequest>> response = auditController.findPickupsForQuote(ID);
        Assert.assertTrue(response.getStatusCode().is4xxClientError());
        Assert.assertNull(response.getBody());
    }

    @Test
    public void findSchedulesForQuoteTest() {
        final List<ScheduleDetailsRequest> scheduleDetailsRequests = Collections.singletonList(new ScheduleDetailsRequest());
        Mockito.when(scheduleOptionsDB.findByDeliveryOptionsId(ID))
                .thenReturn(scheduleDetailsRequests);
        final ResponseEntity<List<ScheduleDetailsRequest>> response = auditController.findSchedulesForQuote(ID);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(scheduleDetailsRequests, response.getBody());
    }

    @Test
    public void findSchedulesForQuoteWhenEmptyTest() {
        Mockito.when(scheduleOptionsDB.findByDeliveryOptionsId(ID))
                .thenReturn(Collections.emptyList());
        final ResponseEntity<List<ScheduleDetailsRequest>> response = auditController.findSchedulesForQuote(ID);
        Assert.assertTrue(response.getStatusCode().is4xxClientError());
        Assert.assertNull(response.getBody());
    }

    @Test
    public void findFulfillsForQuoteTest() {
        final List<FulfillmentRequest> fulfillmentRequests = Collections.singletonList(new FulfillmentRequest());
        Mockito.when(fulfillmentDB.findByDeliveryOptionsRequestId(ID))
                .thenReturn(fulfillmentRequests);
        final ResponseEntity<List<FulfillmentRequest>> response = auditController.findFulfillsForQuote(ID);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(fulfillmentRequests, response.getBody());
    }

    @Test
    public void findFulfillsForQuoteWhenEmptyTest() {
        Mockito.when(fulfillmentDB.findByDeliveryOptionsRequestId(ID))
                .thenReturn(Collections.emptyList());
        final ResponseEntity<List<FulfillmentRequest>> response = auditController.findFulfillsForQuote(ID);
        Assert.assertTrue(response.getStatusCode().is4xxClientError());
        Assert.assertNull(response.getBody());
    }

    @Test
    public void findByFulfillIdTest() {
        final FulfillmentRequest fulfillmentRequest = new FulfillmentRequest();
        fulfillmentRequest.setId("1455");
        Mockito.when(fulfillmentDB.findById(ID)).thenReturn(Optional.ofNullable(fulfillmentRequest));
        final ResponseEntity<FulfillmentRequest> response = auditController.findByFulfillId(ID);
        Assert.assertTrue(response.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(fulfillmentRequest, response.getBody());
    }

    @Test
    public void findByFulfillIdWhenEmptyTest() {
        Mockito.when(fulfillmentDB.findById(ID)).thenReturn(Optional.empty());
        final ResponseEntity<FulfillmentRequest> response = auditController.findByFulfillId(ID);
        Assert.assertTrue(response.getStatusCode().is4xxClientError());
        Assert.assertNull(response.getBody());
    }

}
