package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.QueryBusiness;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.GetQuotationRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.HandleResponse;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.QueryService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryServiceTest {

    @Mock
    private QueryBusiness business;

    @Mock
    private MetricsService metricsService;

    @Mock
    private LiveConfig liveConfig;

    @Mock
    private HandleResponse handleResponse;

    @InjectMocks
    private QueryService queryService;

    private static final String xLocale = "LOCALE";
    private static final String xApplicationName = "APP";
    private static final String xCurrentDate = "AGORA";
    private static final String ZIPCODE = "00000-000";

    private static final List<String> companiesId = Collections.singletonList("001");

    @Before
    public void init() {
        ReflectionTestUtils.setField(queryService, "config", liveConfig);
    }

    @Test
    public void deliveryModesQueryForShoppingCartTest() throws Exception {
        ShoppingCart body = ShoppingCart.builder()
                .destination(CartDestination.builder()
                        .zipcode(ZIPCODE)
                        .build())
                .items(Collections.singletonList(CartItem.builder().sku("5123124").quantity(2).build()))
                .build();

        final String id = "123";
        final DeliveryOptionsReturn deliveryOptionsReturn = DeliveryOptionsReturn.builder()
                .id(id)
                .build();

        when(business.getDeliveryModesForShoppingCart(any(DeliveryOptionsRequest.class)))
                .thenReturn(deliveryOptionsReturn);

        when(liveConfig
                .getConfigValueBoolean(eq(companiesId.get(0)), eq(Optional.ofNullable(xApplicationName)), any(), eq(false)))
                .thenReturn(false);

        DeliveryOptionsReturn response = queryService.deliveryModesQueryForShoppingCart(
                xApplicationName,
                xCurrentDate,
                xLocale,
                1,
                0,
                null,
                Collections.singletonList("899"),
                null,
                null,
                companiesId.get(0),
                null,
                false,
                false,
                body);

        assertEquals(deliveryOptionsReturn, response);

        verify(handleResponse, times(1))
                .handleQueryResponseSync(any(DeliveryOptionsRequest.class), eq(companiesId.get(0)), eq(xApplicationName));
    }

    @Test
    public void deliveryModesQueryForShoppingCartWhenHandleResponseAsyncTest() throws Exception {
        ShoppingCart body = ShoppingCart.builder()
                .destination(CartDestination.builder()
                        .zipcode(ZIPCODE)
                        .build())
                .items(Collections.singletonList(CartItem.builder().sku("5123124").quantity(2).build()))
                .build();

        final String id = "123";
        final DeliveryOptionsReturn deliveryOptionsReturn = DeliveryOptionsReturn.builder()
                .id(id)
                .build();

        when(business.getDeliveryModesForShoppingCart(any(DeliveryOptionsRequest.class)))
                .thenReturn(deliveryOptionsReturn);

        when(liveConfig
                .getConfigValueBoolean(eq(companiesId.get(0)), any(), any(), eq(false)))
                .thenReturn(true);

        DeliveryOptionsReturn response = queryService.deliveryModesQueryForShoppingCart(
                xApplicationName,
                xCurrentDate,
                xLocale,
                1,
                0,
                null,
                Collections.singletonList("899"),
                null,
                null,
                companiesId.get(0),
                null,
                false,
                false,
                body);

        assertEquals(deliveryOptionsReturn, response);
        verify(handleResponse, times(1))
                .handleQueryResponse(any(DeliveryOptionsRequest.class), eq(companiesId.get(0)), eq(xApplicationName));
    }

    @Test
    public void getDeliveryModesQueryByIdTest() {
        final String id = "21121212";
        DeliveryOptionsReturn deliveryOptionsReturn = DeliveryOptionsReturn.builder().id(id).build();
        DeliveryOptionsRequest deliveryOptionsRequest = new DeliveryOptionsRequest();
        deliveryOptionsRequest.setResponse(deliveryOptionsReturn);

        when(business.getDeliveryOptionsById(any(GetQuotationRequest.class), eq(false)))
                .thenReturn(deliveryOptionsRequest);

        final DeliveryOptionsReturn response = queryService.getDeliveryModesQueryById(xApplicationName, xCurrentDate, xLocale, companiesId.get(0), id);
        assertEquals(deliveryOptionsReturn, response);
    }


}
