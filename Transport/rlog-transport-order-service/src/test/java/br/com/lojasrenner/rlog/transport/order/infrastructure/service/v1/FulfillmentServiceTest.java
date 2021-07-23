package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.FulfillmentBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownBranchOfficeException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.CartOrderResult;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.FulfillmentReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.DataLakeService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FulfillmentService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartDestination;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartOrder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;

@RunWith(SpringJUnit4ClassRunner.class)
public class FulfillmentServiceTest {

    @Mock
    private FulfillmentBusiness business;

    @Mock
    private FulfillmentReactiveDBInfrastructure fulfillmentDB;

    @Mock
    private MetricsService metricsService;

    @Mock
    private DataLakeService dataLakeService;

    private InOrder getInOrderObject() {
        return inOrder(business, fulfillmentDB);
    }

    @InjectMocks
    private FulfillmentService fulfillmentService;

    private static final String xLocale = "LOCALE";
    private static final String xApplicationName = "APP";
    private static final String xCurrentDate = "AGORA";
    private static final String COMPANY_ID = "001";
    private static final String ZIPCODE = "00000-000";

    @Test
    public void deve_salvar_resposta_fulfill_no_mongo() throws Exception {
        CartItemWithMode cartItem = new CartItemWithMode();
        cartItem.setModalId("STORE-STANDARD-0-0-1");
        cartItem.setSku("SKU-UM");
        cartItem.setBranchOfficeId(0);
        cartItem.setQuantity(2);
        cartItem.setProductType(ProductTypeEnum.DEFAULT);
        cartItem.setStockStatus(StockStatusEnum.INSTOCK);
        cartItem.setDeliveryMode(null);
        cartItem.setCartItem(CartItem.builder()
                .sku("SKU-UM")
                .branchOfficeId(0)
                .build());

        CartOrder body = CartOrder.builder()
                .destination(CartDestination.builder()
                        .zipcode(ZIPCODE)
                        .build())
                .items(Collections.singletonList(cartItem))
                .build();

        ArgumentCaptor<FulfillmentRequest> requestCapture = ArgumentCaptor.forClass(FulfillmentRequest.class);
        Mockito.when(business.getSimplifiedDeliveryFulfillmentForShoppingCart(any())).thenReturn(CartOrderResult.builder().build());
        Mockito.when(fulfillmentDB.save(requestCapture.capture())).thenReturn(new FulfillmentRequest());

        InOrder _inOrder = getInOrderObject();

        fulfillmentService.deliveryFulfillmentForShoppingCart(xApplicationName, xCurrentDate, xLocale, new ArrayList<>(), COMPANY_ID, body);

        FulfillmentRequest value = requestCapture.getValue();

        assertEquals(xLocale, value.getXLocale());
        assertEquals(xApplicationName, value.getXApplicationName());
        assertEquals(xCurrentDate, value.getXCurrentDate());
        assertEquals(COMPANY_ID, value.getCompanyId());
        assertEquals(ZIPCODE, value.getCartOrder().getDestination().getZipcode());
        assertNull(value.getErrorMessage());

        _inOrder.verify(business, Mockito.times(1)).getSimplifiedDeliveryFulfillmentForShoppingCart(any());
        _inOrder.verify(fulfillmentDB, Mockito.times(1)).save(any());
        _inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void deve_salvar_resposta_fulfill_no_mongo_mesmo_com_erro() throws Exception {
        CartItemWithMode cartItem = new CartItemWithMode();
        cartItem.setModalId("STORE-STANDARD-0-0-1");
        cartItem.setSku("SKU-UM");
        cartItem.setBranchOfficeId(0);
        cartItem.setQuantity(5);
        cartItem.setProductType(ProductTypeEnum.DEFAULT);
        cartItem.setStockStatus(StockStatusEnum.INSTOCK);
        cartItem.setDeliveryMode(null);
        cartItem.setCartItem(CartItem.builder()
                .sku("SKU-UM")
                .branchOfficeId(0)
                .build());

        CartOrder body = CartOrder.builder()
                .destination(CartDestination.builder()
                        .zipcode(ZIPCODE)
                        .build())
                .items(Collections.singletonList(cartItem))
                .build();

        ArgumentCaptor<FulfillmentRequest> requestCapture = ArgumentCaptor.forClass(FulfillmentRequest.class);
        Mockito.when(business.getSimplifiedDeliveryFulfillmentForShoppingCart(any())).thenThrow(UnknownBranchOfficeException.class);
        Mockito.when(fulfillmentDB.save(requestCapture.capture())).thenReturn(new FulfillmentRequest());

        InOrder _inOrder = getInOrderObject();

        try {
            fulfillmentService.deliveryFulfillmentForShoppingCart(xApplicationName, xCurrentDate, xLocale, new ArrayList<>(), COMPANY_ID, body);
        } catch (Exception e) {
            FulfillmentRequest value = requestCapture.getValue();

            assertNotNull(value.getErrorMessage());

            _inOrder.verify(business, Mockito.times(1)).getSimplifiedDeliveryFulfillmentForShoppingCart(any());
            _inOrder.verify(fulfillmentDB, Mockito.times(1)).save(any());
            _inOrder.verifyNoMoreInteractions();

            return;
        }

        fail();
    }
}
