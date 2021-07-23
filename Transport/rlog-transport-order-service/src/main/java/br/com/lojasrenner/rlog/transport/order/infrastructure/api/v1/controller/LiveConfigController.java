package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.lojasrenner.exception.BadRequestException;
import br.com.lojasrenner.rlog.transport.order.business.LiveConfigBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.AuthorizationTokenException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.LiveConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LiveConfigRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LiveConfigResponseV1;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@CrossOrigin
public class LiveConfigController extends BaseController {

	@Value("${bfl.auth.token}")
	private String token;

	@Autowired
	private LiveConfigBusiness business;

	@CrossOrigin
	@PostMapping(value = "/v1/companies/{companyId}/broker/liveConfig", produces = {"application/json"})
	public ResponseEntity<LiveConfigResponseV1> postLiveConfig(
			@RequestHeader(value = "X-Authorization", required = true) String authorization,
			@PathVariable("companyId") String companyId,
			@RequestParam(value = "channel", required = false) String channel,
			@RequestBody LiveConfigRequestV1 request
	) {
		validateToken(authorization);
		validateParams(request);

		return new ResponseEntity<>(business.postLiveConfig(companyId, request, Optional.ofNullable(channel)), HttpStatus.OK);
	}

	@PutMapping(value = "/v1/companies/{companyId}/broker/liveConfig", produces = {"application/json"})
	public ResponseEntity<LiveConfigResponseV1> putLiveConfig(
			@RequestHeader(value = "X-Authorization", required = true) String authorization,
			@PathVariable("companyId") String companyId,
			@RequestBody Map<String, Object> request
	) {
		validateToken(authorization);
		ObjectMapper mapper = new ObjectMapper();
		LiveConfigRequestV1 requestObject = mapper.convertValue(request, LiveConfigRequestV1.class);
		validateParams(requestObject);

		return new ResponseEntity<>(business.putLiveConfig(companyId, request), HttpStatus.OK);
	}

	@CrossOrigin
	@GetMapping(value = "/v1/companies/{companyId}/broker/liveConfig", produces = {"application/json"})
	public ResponseEntity<LiveConfigEntity> getLiveConfig(
			@RequestHeader(value = "X-Authorization", required = true) String authorization,
			@PathVariable("companyId") String companyId,
			@RequestParam(value = "channel", required = false) String channel
	) {
		validateToken(authorization);

		return new ResponseEntity<>(business.getLiveConfig(companyId, Optional.ofNullable(channel)), HttpStatus.OK);
	}

	private void validateToken(String authorization) {
		if (!authorization.equals(token))
			throw new AuthorizationTokenException("Unauthorized access token");
	}

	private void validateParams(LiveConfigRequestV1 request) {
		if (request.getBusiness() == null)
			throw new BadRequestException("company configuration cannot be null", "400");
	}
}
