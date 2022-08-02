package br.com.lojasrenner.rlog.transport.order.hub.infrastructure.service;

import br.com.lojasrenner.rlog.transport.order.hub.configuration.PartnerConfiguration;
import br.com.lojasrenner.rlog.transport.order.hub.infrastructure.service.v1.TransportOrderHubService;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.PartnerInformationDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerConfigEntity;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerInformationEntity;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@ContextConfiguration(classes = {PartnerConfiguration.class,TransportOrderHubService.class, RestTemplate.class} )
@RunWith(SpringRunner.class)
@Service
@Log4j2
public class TransportOrderHubServiceTest {

    @Autowired
    private TransportOrderHubService service;

    @MockBean
    private PartnerConfiguration configuration;

    @MockBean
    private PartnerInformationDBInfrastructure dbInfrastructure;

    @Test
    public void deveEnviarParaOParceiroCorreto() {

        String o = "";
        String partnerId = "1";
        String method = "analytics";
        PartnerConfigEntity entity = getPartnerConfigEntity();

    when(configuration.getUrlByPartnerIdAndMethod(any(),any())).thenReturn(entity);

    service.sendToRightPartner(o,partnerId,method);

    assertEquals("https://www.netvasco.com.br/",entity.getUrl());
    assertThat("https://www.netvasco.com.br/", is(entity.getUrl()));

    }

    @Test
    public void deveSalvarNaCollectionInformation() {
        PartnerInformationEntity entity = getPartnerInformationEntity();

        when(dbInfrastructure.save(any())).thenReturn(entity);
        service.save(entity.getBody(),entity.getPartnerId(),entity.getMethod());

        assertThat("method",is(entity.getMethod()));
    }

    private PartnerConfigEntity getPartnerConfigEntity() {
        PartnerConfigEntity entity = new PartnerConfigEntity();
        entity.setId("1");
        entity.setMethod("analytics");
        entity.setUrl("https://www.netvasco.com.br/");
        entity.setPartnerPlatformId("1");
        return entity;
    }

    private PartnerInformationEntity getPartnerInformationEntity() {
        PartnerInformationEntity entity = new PartnerInformationEntity();
        entity.setMethod("method");
        entity.setReceivedDate(LocalDateTime.now());
        entity.setBody("body");
        entity.setPartnerId("1");
        return entity;
    }

}


