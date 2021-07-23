package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.DataLakeService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.HandleResponse;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;

import org.apache.logging.log4j.util.TriConsumer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class HandleResponseTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private DataLakeService dataLakeService;

    @Mock
    private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

    @Captor
    private ArgumentCaptor<DeliveryOptionsRequest> deliveryOptionsRequestArgumentCaptor;

    @InjectMocks
    private HandleResponse handleResponse;

    private static final String ZIPCODE = "00000-000";
    private static final String COMPANY_ID = "001";
    private static final String APPLICATION_NAME = "Teste unitario";

    final TriConsumer<DeliveryOptionsRequest, String, String> triConsumerSync = (final DeliveryOptionsRequest deliveryOptionsRequest,
                                                                                 final String companyId,
                                                                                 final String xApplicationName) -> handleResponse.handleQueryResponseSync(deliveryOptionsRequest, companyId, xApplicationName);

    final TriConsumer<DeliveryOptionsRequest, String, String> triConsumerAsync = (final DeliveryOptionsRequest deliveryOptionsRequest,
                                                                                  final String companyId,
                                                                                  final String xApplicationName) -> handleResponse.handleQueryResponse(deliveryOptionsRequest, companyId, xApplicationName);

    @Test
    public void handleQueryResponseSyncTest() {
        handleQueryResponseTest(triConsumerSync);
    }

    @Test
    public void handleQueryResponseSyncWhenThrowRuntimeException() {
        handleQueryResponseWhenThrowRuntimeException(triConsumerSync);
    }

    @Test
    public void handleQueryResponseAsyncTest() {
        handleQueryResponseTest(triConsumerAsync);
    }

    @Test
    public void handleQueryResponseAsyncWhenThrowRuntimeException() {
        handleQueryResponseWhenThrowRuntimeException(triConsumerAsync);
    }

    public void handleQueryResponseTest(TriConsumer<DeliveryOptionsRequest, String, String> triConsumer) {
        final ShoppingCart shoppingCart = ShoppingCart.builder()
                .destination(CartDestination.builder()
                        .zipcode(ZIPCODE)
                        .build())
                .items(Collections.singletonList(CartItem.builder().sku("5123124").quantity(2).build()))
                .build();
        final String id = "123";
        DeliveryOptionsRequest deliveryOptionsRequest = new DeliveryOptionsRequest();
        deliveryOptionsRequest.setId(id);
        deliveryOptionsRequest.setShoppingCart(shoppingCart);
        deliveryOptionsRequest.setCompanyId(COMPANY_ID);
        deliveryOptionsRequest.setXApplicationName(APPLICATION_NAME);

        when(deliveryOptionsDB.save(deliveryOptionsRequestArgumentCaptor.capture())).thenReturn(deliveryOptionsRequest);

        triConsumer.accept(deliveryOptionsRequest, COMPANY_ID, APPLICATION_NAME);

        final DeliveryOptionsRequest capture = deliveryOptionsRequestArgumentCaptor.getValue();
        Assert.assertEquals(id, capture.getId());
        Assert.assertEquals(COMPANY_ID, capture.getCompanyId());
        Assert.assertEquals(APPLICATION_NAME, capture.getXApplicationName());
    }

    public void handleQueryResponseWhenThrowRuntimeException(TriConsumer<DeliveryOptionsRequest, String, String> triConsumer) {
        final ShoppingCart shoppingCart = ShoppingCart.builder()
                .destination(CartDestination.builder()
                        .zipcode(ZIPCODE)
                        .build())
                .items(Collections.singletonList(CartItem.builder().sku("5123124").quantity(2).build()))
                .build();
        DeliveryOptionsRequest deliveryOptionsRequest = new DeliveryOptionsRequest();
        deliveryOptionsRequest.setShoppingCart(shoppingCart);
        when(deliveryOptionsDB.save(deliveryOptionsRequest)).thenThrow(RuntimeException.class);
        try {
            triConsumer.accept(deliveryOptionsRequest, COMPANY_ID, APPLICATION_NAME);
        } catch (Exception e) {
            Assert.fail("Erro ao logar mensagem de erro sem lançar exception");
        }
        verify(deliveryOptionsDB, times(1)).save(any(DeliveryOptionsRequest.class));
    }

}
