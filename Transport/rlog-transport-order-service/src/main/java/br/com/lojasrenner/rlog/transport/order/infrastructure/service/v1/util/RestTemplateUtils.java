package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util;

import br.com.lojasrenner.exception.RennerResponseErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class RestTemplateUtils {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    public RestTemplate getRestTemplate(Long timeoutMili) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(timeoutMili))
                .setReadTimeout(Duration.ofMillis(timeoutMili))
                .errorHandler(new RennerResponseErrorHandler())
                .build();
    }

}
