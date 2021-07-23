package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServicesErrorRegexEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TypeTimeoutEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.BranchOfficeResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.RestTemplateUtils;
import br.com.lojasrenner.rlog.transport.order.metrics.ServiceErrorsMetrics;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;

import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;


@Service
public class BranchClientServiceInfrastructureV1 {

    @Autowired
    private TimeoutMetrics timeoutMetrics;
    
    @Autowired
    private ServiceErrorsMetrics serviceErrorsMetrics;

    @Value("${bfl.timeout.branch-service:10000}")
    private long timeoutMili;

    @Value("${bfl.endpoint.branch-service}")
    private String baseUrl;

    @Autowired
    private RestTemplateUtils restTemplateUtils;

    private RestTemplate restTemplate;

    public ResponseEntity<BranchOfficeResponseV1> findByCompanyId(String companyId, Integer page, Integer size) {
        try {
            if(restTemplate == null)
                restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
            return restTemplate
                    .exchange(baseUrl + "companies/" + companyId + "/branch-offices?size=" + size + "&page=" + page,
                            HttpMethod.GET,
                            null,
                            BranchOfficeResponseV1.class);
        } catch (Exception ex) {
        	if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
                TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
                timeoutMetrics.sendTimeoutMetrics(companyId, "[async]", ServiceTypeEnum.BRANCH_SERVICE, EndpointEnum.BRANCH_SERVICE_FIND_BY_COMPANY_ID, type);
            }
        	else if(ex.getMessage() != null) {
        		serviceErrorsMetrics.sendErrorMetric(companyId, "[async]", ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
        				ServiceTypeEnum.BRANCH_SERVICE, EndpointEnum.BRANCH_SERVICE_FIND_BY_COMPANY_ID);
    		}
            throw ex;
        }
    }
}
