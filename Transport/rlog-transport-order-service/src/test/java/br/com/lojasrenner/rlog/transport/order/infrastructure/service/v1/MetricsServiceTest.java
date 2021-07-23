package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class MetricsServiceTest {

    @InjectMocks
    private MetricsService metricsService;

    @Mock
    private TimeoutMetrics timeoutMetrics;

    @Test
    public void deve_retornar_tempo_excedente_enviando_as_metricas() {
        var deliveryOptionsRequest = new DeliveryOptionsRequest();
        deliveryOptionsRequest.setInitialTimestamp(System.currentTimeMillis());
        deliveryOptionsRequest.setQuoteSettings(QuoteSettings.builder()
                .maxCombinationsTimeOutHeader(-1)
                .build());
        boolean combinationTimeoutExceeded = metricsService.combinationTimeoutExceeded(deliveryOptionsRequest, true);
        verify(timeoutMetrics, times(1)).sendTimeoutMetrics(any(), any(), any(), any(), any());
        Assert.assertTrue(combinationTimeoutExceeded);
    }

    @Test
    public void deve_retornar_tempo_nao_excedente_nao_enviando_metricas() {
        var deliveryOptionsRequest = new DeliveryOptionsRequest();
        deliveryOptionsRequest.setInitialTimestamp(System.currentTimeMillis());
        deliveryOptionsRequest.setQuoteSettings(QuoteSettings.builder()
                .maxCombinationsTimeOutHeader(60000)
                .build());
        boolean combinationTimeoutExceeded = metricsService.combinationTimeoutExceeded(deliveryOptionsRequest, true);
        verify(timeoutMetrics, never()).sendTimeoutMetrics(any(), any(), any(), any(), any());
        Assert.assertFalse(combinationTimeoutExceeded);
    }

}
