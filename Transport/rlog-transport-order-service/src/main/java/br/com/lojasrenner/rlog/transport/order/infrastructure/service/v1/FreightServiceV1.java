package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServicesErrorRegexEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TypeTimeoutEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BusinessDaysDTO;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BusinessDaysModel;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteBusinessDaysResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.SchedulingDateResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.RestTemplateUtils;
import br.com.lojasrenner.rlog.transport.order.metrics.ServiceErrorsMetrics;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;
import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FreightServiceV1 {

    @Autowired
    private TimeoutMetrics timeoutMetrics;
    
    @Autowired
    private ServiceErrorsMetrics serviceErrorsMetrics;

    private String baseUrl;

    @Value("${bfl.timeout.freight-service:5000}")
    private long timeoutMili;

    @Autowired
    private RestTemplateUtils restTemplateUtils;

    private RestTemplate restTemplate;

    @Autowired
    public FreightServiceV1(@Value("${bfl.endpoint.freight-service}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ResponseEntity<SchedulingDateResponseV1> getScheduleDates(String companyId, String channel, Integer deliveryMethodId, String origin, String destination, Integer quantity, Integer fromToday) {
        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        Map<String, Object> params = new HashMap<>();
        params.put("deliveryMethodId", deliveryMethodId);
        params.put("zipSource", origin);
        params.put("zipDestination", destination);
        params.put("quantity", quantity);
        params.put("fromToday", fromToday);

        try {
            if(restTemplate == null)
                restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
            return restTemplate
                    .exchange(baseUrl + "companies/" + companyId + "/quotes/scheduling-dates?quantity={quantity}&fromToday={fromToday}&deliveryMethodId={deliveryMethodId}&zipSource={zipSource}&zipDestination={zipDestination}",
                            HttpMethod.GET,
                            requestEntity,
                            SchedulingDateResponseV1.class,
                            params);
        } catch (Exception ex) {
        	if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
                TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
                timeoutMetrics.sendTimeoutMetrics(companyId, channel, ServiceTypeEnum.FREIGHT_SERVICE, EndpointEnum.FREIGHT_SERVICE_GET_SCHEDULE_DATES, type);
            }
        	else if(ex.getMessage() != null) {
        		serviceErrorsMetrics.sendErrorMetric(companyId, channel, ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
        				ServiceTypeEnum.FREIGHT_SERVICE, EndpointEnum.FREIGHT_SERVICE_GET_SCHEDULE_DATES);
    		}
            throw ex;
        }
    }

    public Map<String, QuoteBusinessDaysResponseV1> getMultipleDeliveryDates(Set<BusinessDaysModel> daysList) {
        if (daysList == null || daysList.isEmpty())
            return new HashMap<>();

        return daysList
                .parallelStream()
                .map(d -> getDeliveryDateAsync(d.getCompanyId(), d.getOriginZipCode(), d.getDestinationZipCode(), d.getBusinessDays()))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(result -> BusinessDaysModel.builder()
                        .companyId(result.getCompanyId())
                        .originZipCode(result.getOriginZipCode())
                        .destinationZipCode(result.getDestinationZipCode())
                        .businessDays(result.getBusinessDays())
                        .build().getKey(), BusinessDaysDTO::getResponse));
    }

    private BusinessDaysDTO getDeliveryDateAsync(String companyId, String originZipCode, String destinationZipCode, int businessDay) {
        return BusinessDaysDTO.builder()
                .response(QuoteBusinessDaysResponseV1.builder().dateDelivery(getDatePlusBusinessDays(businessDay)).build())
                .companyId(companyId)
                .originZipCode(originZipCode)
                .destinationZipCode(destinationZipCode)
                .businessDays(businessDay)
                .build();
    }

    static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getDatePlusBusinessDays(int workdays) {
        LocalDateTime date = LocalDateTime.now();

        if (workdays < 1) {
            return dateFormatter.format(date);
        }

        int addedDays = 0;
        while (addedDays < workdays) {
            date = date.plusDays(1);
            if (!(date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                ++addedDays;
            }
        }

        return dateFormatter.format(date);
    }

    public ResponseEntity<QuoteResponseV1> getQuote(String companyId, String channel, QuoteRequestV1 request) {
        HttpEntity<QuoteRequestV1> requestEntity = new HttpEntity<>(request);

        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);

        try {
            if(restTemplate == null)
                restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
            return restTemplate
                    .exchange(baseUrl + "companies/{companyId}/quotes",
                            HttpMethod.POST,
                            requestEntity,
                            QuoteResponseV1.class,
                            params);
        } catch (Exception ex) {
            if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
                TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
                timeoutMetrics.sendTimeoutMetrics(companyId, channel, ServiceTypeEnum.FREIGHT_SERVICE, EndpointEnum.FREIGHT_SERVICE_GET_QUOTE, type);
            }
            else if(ex.getMessage() != null) {
                serviceErrorsMetrics.sendErrorMetric(companyId, channel, ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
                        ServiceTypeEnum.FREIGHT_SERVICE, EndpointEnum.FREIGHT_SERVICE_GET_QUOTE);
            }
            throw ex;
        }
    }
}
