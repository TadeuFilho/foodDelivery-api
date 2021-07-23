package br.com.lojasrenner.rlog.transport.order.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.lojasrenner.rlog.transport.order.business.QueryBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.QuotationExpiredException;
import br.com.lojasrenner.rlog.transport.order.business.exception.QuotationNotFoundException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.GetQuotationRequest;


@RunWith(SpringJUnit4ClassRunner.class)
public class GetQuoteBusinessTest {

    @InjectMocks
    private QueryBusiness business;

    @Mock
    private DeliveryOptionsDBInfrastructure deliveryOptionsDB;


    @Test(expected = QuotationNotFoundException.class)
    public void notFoundQuoteTest(){
        //invalid id
        String id = "U81N8A-ANSDASDA-Dasd";

        GetQuotationRequest getQuotationRequest = new GetQuotationRequest();
        getQuotationRequest.setDeliveryOptionsId(id);

        when(deliveryOptionsDB.findById(id)).thenReturn(Optional.empty());
        business.getDeliveryOptionsById(getQuotationRequest, false);
    }


    @Test(expected = QuotationExpiredException.class)
    public void expiredQuoteTest(){
        String id = "43d080ce-f897-4e0c-b06a-ff09ba302c02";

        ReflectionTestUtils.setField(business, "expirationMinutes", 1440);
        GetQuotationRequest getQuotationRequest = new GetQuotationRequest();
        getQuotationRequest.setDeliveryOptionsId(id);
        Optional<DeliveryOptionsRequest> bdResponse = Optional.of(new DeliveryOptionsRequest());
        bdResponse.get().setDate(LocalDateTime.now().minusMinutes(1441));

        when(deliveryOptionsDB.findById(id)).thenReturn(bdResponse);
        business.getDeliveryOptionsById(getQuotationRequest, false);
    }

    @Test
    public void getQuoteTest(){
        String id = "43d080ce-f897-4e0c-b06a-ff09ba302c02";
        ReflectionTestUtils.setField(business, "expirationMinutes", 1440);

        GetQuotationRequest getQuotationRequest = new GetQuotationRequest();
        getQuotationRequest.setDeliveryOptionsId(id);

        Optional<DeliveryOptionsRequest> bdResponse = Optional.of(new DeliveryOptionsRequest());
        bdResponse.get().setDate(LocalDateTime.now().minusMinutes(1239));
        List<DeliveryOption> deliverys = new ArrayList<>();
        bdResponse.get().setResponse(new DeliveryOptionsReturn(id, bdResponse.get().getDate(), deliverys, 1, null));

        when(deliveryOptionsDB.findById(id)).thenReturn(bdResponse);
        DeliveryOptionsReturn retorno = business.getDeliveryOptionsById(getQuotationRequest, false).getResponse();

        assertEquals(retorno.getId(),id);
    }
}
