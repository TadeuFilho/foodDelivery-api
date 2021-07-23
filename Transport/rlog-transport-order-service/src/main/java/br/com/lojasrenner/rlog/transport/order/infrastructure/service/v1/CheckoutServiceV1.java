package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServicesErrorRegexEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TypeTimeoutEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.CheckoutRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.RestTemplateUtils;
import br.com.lojasrenner.rlog.transport.order.metrics.ServiceErrorsMetrics;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;
import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutServiceV1 {

    @Autowired
    private TimeoutMetrics timeoutMetrics;

    @Autowired
    private RestTemplateUtils restTemplateUtils;

    private RestTemplate restTemplate;

    private static final String COMPANY_ID = "companyId";

    @Value("${bfl.timeout.checkout-service:10000}")
    private long timeoutMili;

    @Value("${bfl.endpoint.checkout-service}")
    private String baseUrl;

    @Autowired
    private ServiceErrorsMetrics serviceErrorsMetrics;

    public boolean checkoutQuotation(String companyId, String quoteId, String extOrderCode) {
        CheckoutRequestV1 request = CheckoutRequestV1.builder().extOrderCode(extOrderCode).id(quoteId).build();
        ResponseEntity<String> checkout = postCheckout(companyId, request, EndpointEnum.CHECKOUT_API_CHECKOUT_QUOTATION);
        if(checkout.getStatusCode() == HttpStatus.OK)
            return true;
        else
            return false;
    }

    public ResponseEntity<String> postCheckout(String companyId, CheckoutRequestV1 request, EndpointEnum endpointEnum) {
        HttpEntity<CheckoutRequestV1> requestEntity = new HttpEntity<>(request);

        Map<String, Object> params = new HashMap<>();

        params.put(COMPANY_ID, companyId);
        try {
            if(restTemplate == null)
                restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
            return restTemplate
                    .exchange(baseUrl + endpointEnum,
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<String>() {
                            },
                            params);
        } catch (Exception ex) {
            if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
                TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
                timeoutMetrics.sendTimeoutMetrics(companyId, "channel", ServiceTypeEnum.CHECKOUT_SERVICE, EndpointEnum.CHECKOUT_API_CHECKOUT_QUOTATION, type);
            }
            else if(ex.getMessage() != null) {
                serviceErrorsMetrics.sendErrorMetric(companyId, "channel", ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
                        ServiceTypeEnum.CHECKOUT_SERVICE, EndpointEnum.CHECKOUT_API_CHECKOUT_QUOTATION);
            }
            throw ex;
        }
    }
}
