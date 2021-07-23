package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServicesErrorRegexEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TypeTimeoutEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingToResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.RestTemplateUtils;
import br.com.lojasrenner.rlog.transport.order.metrics.ServiceErrorsMetrics;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;
import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeoLocationServiceV1 {

	@Autowired
	private TimeoutMetrics timeoutMetrics;

	@Autowired
	private ServiceErrorsMetrics serviceErrorsMetrics;

	@Value("${bfl.timeout.branch-geolocation-service:5000}")
	private long timeoutMili;

	@Value("${bfl.endpoint.branch-geolocation-service}")
	private String baseUrl;

	private static final String COMPANY_ID = "companyId";

	@Autowired
	private RestTemplateUtils restTemplateUtils;

	private RestTemplate restTemplate;

	public ResponseEntity<List<GeoLocationResponseV1>> getClosestStoresInState(String companyId, String channel, String clientZipCode) {
		return getNearestBranch(companyId, channel, clientZipCode, EndpointEnum.GEOLOCATION_SERVICE_GET_CLOSEST_STORES_IN_STATE);
	}

	public ResponseEntity<List<GeoLocationResponseV1>> getClosestStoresInRange(String companyId, String channel, String clientZipCode) {
		return getNearestBranch(companyId, channel, clientZipCode, EndpointEnum.GEOLOCATION_SERVICE_GET_CLOSEST_STORES_IN_RANGE);
	}

	public ResponseEntity<List<ShippingGroupResponseV1>> getGroupsByZipcode(String companyId, String xApplicationName, String clientZipCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Application-Name", xApplicationName);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		Map<String, Object> params = new HashMap<>();
		params.put(COMPANY_ID, companyId);
		params.put("zipcode", clientZipCode);

		try {
			if (restTemplate == null)
				restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
			return restTemplate
					.exchange(baseUrl + "companies/{companyId}/range-nearest-branch/{zipcode}",
							HttpMethod.GET,
							requestEntity,
							new ParameterizedTypeReference<List<ShippingGroupResponseV1>>() {
							},
							params);
		} catch (Exception ex) {
			if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
				TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
				timeoutMetrics.sendTimeoutMetrics(companyId, xApplicationName, ServiceTypeEnum.GEOLOCATION_SERVICE, EndpointEnum.GEOLOCATION_SERVICE_GET_GROUPS_BY_ZIPCODE, type);
			} else if (ex.getMessage() != null) {
				serviceErrorsMetrics.sendErrorMetric(companyId, xApplicationName, ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
						ServiceTypeEnum.GEOLOCATION_SERVICE, EndpointEnum.GEOLOCATION_SERVICE_GET_GROUPS_BY_ZIPCODE);
			}
			throw ex;
		}
	}

	public ResponseEntity<List<String>> getStoresByState(String companyId, String channel, String state) {
		HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

		Map<String, Object> params = new HashMap<>();
		params.put(COMPANY_ID, companyId);
		params.put("state", state);

		try {
			if (restTemplate == null)
				restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
			return restTemplate
					.exchange(baseUrl + "companies/{companyId}/state/{state}",
							HttpMethod.GET,
							requestEntity,
							new ParameterizedTypeReference<List<String>>() {
							},
							params);
		} catch (Exception ex) {
			if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
				TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
				timeoutMetrics.sendTimeoutMetrics(companyId, channel, ServiceTypeEnum.GEOLOCATION_SERVICE, EndpointEnum.GEOLOCATION_SERVICE_GET_STORES_BY_STATE, type);
			} else if (ex.getMessage() != null) {
				serviceErrorsMetrics.sendErrorMetric(companyId, channel, ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
						ServiceTypeEnum.GEOLOCATION_SERVICE, EndpointEnum.GEOLOCATION_SERVICE_GET_STORES_BY_STATE);
			}
			throw ex;
		}
	}

	private ResponseEntity<List<GeoLocationResponseV1>> getNearestBranch(String companyId, String xApplicationName, String zipcode, EndpointEnum endpointEnum) {
		HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

		Map<String, Object> params = new HashMap<>();
		params.put(COMPANY_ID, companyId);
		params.put("zipcode", zipcode);

		try {
			if (restTemplate == null)
				restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
			return restTemplate
					.exchange(baseUrl + endpointEnum.toString(),
							HttpMethod.GET,
							requestEntity,
							new ParameterizedTypeReference<List<GeoLocationResponseV1>>() {
							},
							params);
		} catch (Exception ex) {
			if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
				TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
				timeoutMetrics.sendTimeoutMetrics(companyId, xApplicationName, ServiceTypeEnum.GEOLOCATION_SERVICE, endpointEnum, type);
			} else if (ex.getMessage() != null) {
				serviceErrorsMetrics.sendErrorMetric(companyId, xApplicationName, ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
						ServiceTypeEnum.GEOLOCATION_SERVICE, endpointEnum);
			}
			throw ex;
		}
	}

	public ResponseEntity<List<ShippingToResponseV1>> getShippingToForBranches(
			String companyId,
			String applicationName,
			List<String> branches
	) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Application-Name", applicationName);

		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		Map<String, Object> params = new HashMap<>();
		params.put(COMPANY_ID, companyId);
		params.put("branches", String.join(",", branches));

		try {
			if (restTemplate == null)
				restTemplate = restTemplateUtils.getRestTemplate(timeoutMili);
			return restTemplate
					.exchange(baseUrl + "companies/{companyId}/shipping-to/list-branches?branches={branches}",
							HttpMethod.GET,
							requestEntity,
							new ParameterizedTypeReference<List<ShippingToResponseV1>>() {
							},
							params);
		} catch (Exception ex) {
			if (ex.getCause() instanceof ConnectTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
				TypeTimeoutEnum type = ex.getCause() instanceof ConnectTimeoutException ? TypeTimeoutEnum.CONNECT_TIMEOUT : TypeTimeoutEnum.SOCKET_TIMEOUT;
				timeoutMetrics.sendTimeoutMetrics(companyId,
						applicationName,
						ServiceTypeEnum.GEOLOCATION_SERVICE,
						EndpointEnum.GEOLOCATION_SHIPPING_TO_PER_BRANCHES,
						type);
			} else if (ex.getMessage() != null) {
				serviceErrorsMetrics.sendErrorMetric(companyId,
						applicationName,
						ServicesErrorRegexEnum.anyMatch(ex.getMessage()),
						ServiceTypeEnum.GEOLOCATION_SERVICE,
						EndpointEnum.GEOLOCATION_SHIPPING_TO_PER_BRANCHES);
			}
			throw ex;
		}
	}
}
