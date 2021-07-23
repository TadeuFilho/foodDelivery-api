package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TypeTimeoutEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.RestTemplateUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServicesErrorRegexEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LocationStockV1Request;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.metrics.ServiceErrorsMetrics;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;

@Service
public class StockServiceV1 {

    @Value("${bfl.timeout.stock-api:5000}")
    private long timeoutMili;

    @Value("${oms.endpoint.stock-api}")
    private String baseUrl;

    @Autowired
    private TimeoutMetrics timeoutMetrics;
    
    @Autowired
    private ServiceErrorsMetrics serviceErrorsMetrics;

    @Autowired
    private RestTemplateUtils restTemplateUtils;

    private RestTemplate restTemplate;

    public ResponseEntity<List<LocationStockV1Response>> postLocationStock(String companyId, String channel, LocationStockV1Request locationStockRequest) {

        HttpEntity<LocationStockV1Request> requestEntity = new HttpEntity<>(locationStockRequest);

        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);

        try {
            if(restTemplate == null)
                restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);

            return restTemplate
                    .exchange(baseUrl + "companies/{companyId}/stocks",
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<List<LocationStockV1Response>>() {
                            },
                            params);
        } catch (Exception ex) {
        	if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
                TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
                timeoutMetrics.sendTimeoutMetrics(companyId, channel, ServiceTypeEnum.STOCK_API, EndpointEnum.STOCK_API_POST_LOCATION_STOCK, type);
            }
        	else if(ex.getMessage() != null) {
        		serviceErrorsMetrics.sendErrorMetric(companyId, channel, ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
        				ServiceTypeEnum.STOCK_API, EndpointEnum.STOCK_API_POST_LOCATION_STOCK);
    		}
            throw ex;
        }
    }

}
