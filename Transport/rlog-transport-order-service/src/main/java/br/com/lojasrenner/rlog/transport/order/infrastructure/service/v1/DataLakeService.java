package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;


import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.RestTemplateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DataLakeTypeEnum;


@Component
public class DataLakeService {

    private static final Gson GSON = new Gson();

    private static final String DISABLED = "DISABLED";
    private static final String DISCARDED = "DISCARDED";
    private static final String NO_TRANSACTION_ID = "NO TRANSACTION ID";
    private static final String ERROR = "ERROR";

    @Autowired
    private LiveConfig config;

    @Value("${bfl.endpoint.datalake.url}")
    private String baseUrl;

    @Value("${bfl.endpoint.datalake.token}")
    private String token;

    @Value("${bfl.timeout.datalake:10000}")
    private long timeoutMili;

    private final Random random = new Random();

    @Autowired
    private RestTemplateUtils restTemplateUtils;

    private RestTemplate restTemplate;

    public String send(String companyId, Optional<String> xApplicationNameOptional, Object payload, DataLakeTypeEnum typeEnum) {
        Double configRate = config.getConfigValueDouble(companyId, xApplicationNameOptional, CompanyConfigEntity::getDataLakeSendDataRate, true);
        if (configRate == null || configRate == 0.0)
            return DISABLED;

        if (random.nextDouble() > configRate)
            return DISCARDED;

        try {
            String json = GSON.toJson(payload);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("ContentType", "application/json");
            bodyMap.put("RnrType", typeEnum.toString());
            bodyMap.put("SendDateTime", Instant.now().toString());
            bodyMap.put("Event", json);

            String body = GSON.toJson(bodyMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            if(restTemplate == null)
                restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate
                    .exchange(baseUrl + "api/v1/event/in_renner_oms",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody() && response.getBody().containsKey("TransactionUUID"))
                return response.getBody().get("TransactionUUID").toString();

            return NO_TRANSACTION_ID;
        } catch (Exception ex) {
            return ERROR;
        }
    }
}
