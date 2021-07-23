package br.com.lojasrenner.rlog.transport.order.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ShippingMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.SchedulingDateContentResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.SchedulingDateResponseV1;
import br.com.lojasrenner.rlog.transport.order.business.ScheduleBusiness;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

@RunWith(SpringJUnit4ClassRunner.class)
public class ScheduleBusinessTest {

    @InjectMocks
    private ScheduleBusiness business;

    @Mock
    private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

    @Mock
    private FreightServiceV1 freightService;

    @Before
    public void prepareTests(){
        Optional<DeliveryOptionsRequest> bdResponse = Optional.of(new DeliveryOptionsRequest());
        bdResponse.get().setDate(LocalDateTime.now().minusMinutes(1239));
        bdResponse.get().setShoppingCart(ShoppingCart.builder()
        		.items(Arrays.asList(
        				getItem("1UM", 11),
        				getItem("2DOIS", 22)
        		))
                .destination(CartDestination.builder().zipcode("04433050").build())
                .build());
        bdResponse.get().setCompanyId("001");

        bdResponse.get().setResponse(new DeliveryOptionsReturn("43d080ce-f897-4e0c-b06a-ff09ba302c02", bdResponse.get().getDate(),
        		Arrays.asList(DeliveryOption.builder()
        				.deliveryModesVerbose(new ArrayList<>(Arrays.asList(
        					DeliveryMode.builder()
        						.deliveryEstimateBusinessDays(2)
        						.estimatedDeliveryTimeValue("2")
        						.description("Loja Standard")
        						.distance(500)
        						.shippingMethod(ShippingMethodEnum.STANDARD)
        						.build(),
        					DeliveryMode.builder()
        						.deliveryEstimateBusinessDays(6)
        						.estimatedDeliveryTimeValue("6")
        						.description("Scheduled 1")
        						.id("CD-SCHEDULED-6-1000")
                                    .modalId("CD-SCHEDULED-6-1000")
        						.origin("00899000")
        						.destination("04433050")
        						.deliveryMethodId(555)
        						.shippingMethod(ShippingMethodEnum.SCHEDULED)
        						.fulfillmentMethod(FulfillmentMethodEnum.CD.getValue())
        						.build(),
        					DeliveryMode.builder()
        						.deliveryEstimateBusinessDays(5)
        						.estimatedDeliveryTimeValue("5")
        						.description("Scheduled 2")
        						.id("STORE-SCHEDULED-6-1000")
                                    .modalId("STORE-SCHEDULED-6-1000")
        						.origin("02002000")
        						.destination("04433050")
        						.deliveryMethodId(444)
        						.fulfillmentMethod(FulfillmentMethodEnum.STORE.getValue())
        						.shippingMethod(ShippingMethodEnum.SCHEDULED)
        						.build(),
        					DeliveryMode.builder()
        						.deliveryEstimateBusinessDays(2)
        						.estimatedDeliveryTimeValue("2")
        						.description("Loja Express")
        						.distance(200)
        						.branchOfficeId("5005")
        						.shippingMethod(ShippingMethodEnum.EXPRESS)
        						.build()
        				)))
        				.build()), 1, null));

        when(deliveryOptionsDB.findById(any())).thenReturn(bdResponse);
        
        ResponseEntity<SchedulingDateResponseV1> responseEntityFromFreightService = ResponseEntity.ok(SchedulingDateResponseV1.builder()
        		.content(SchedulingDateContentResponseV1.builder()
        				.availableBusinessDays(Arrays.asList("2020-01-01", "2020-01-02", "2020-01-03", "2020-01-04"))
        				.build())
        		.build());
		when(freightService.getScheduleDates(eq("001"), anyString(), eq(444), eq("02002000"), eq("04433050"), eq(16), eq(17))).thenReturn(responseEntityFromFreightService);
		

    }

	@Test
    public void getScheduleDetailsOptionsTest() throws Exception {
        //mock de entradas
		ScheduleDetailsRequest scheduleDetailsRequest = new ScheduleDetailsRequest();
		scheduleDetailsRequest.setCompanyId("001");
		scheduleDetailsRequest.setDeliveryOptionsId("43d080ce-f897-4e0c-b06a-ff09ba302c02");
		scheduleDetailsRequest.setDeliveryModeId("STORE-SCHEDULED-6-1000");
		scheduleDetailsRequest.setFromToday(17);
		scheduleDetailsRequest.setXApplicationName("[unit-test]");
		scheduleDetailsRequest.setQuantity(16);

		ScheduleDetailsReturn scheduleDetailsOptions = business.getScheduleDetailsOptions(scheduleDetailsRequest);

		assertNotNull(scheduleDetailsOptions);
		assertNotNull(scheduleDetailsOptions.getAvailableBusinessDays());
		assertEquals(4, scheduleDetailsOptions.getAvailableBusinessDays().size());
		assertEquals("2020-01-01", scheduleDetailsOptions.getAvailableBusinessDays().get(0));
		assertEquals("2020-01-02", scheduleDetailsOptions.getAvailableBusinessDays().get(1));
		assertEquals("2020-01-03", scheduleDetailsOptions.getAvailableBusinessDays().get(2));
		assertEquals("2020-01-04", scheduleDetailsOptions.getAvailableBusinessDays().get(3));
    }
    
    private static CartItem getItem(String sku, Integer quantity) {
    	CartItem c = new CartItem();
    	c.setSku(sku);
    	c.setQuantity(quantity);
		return c;
	}

}
