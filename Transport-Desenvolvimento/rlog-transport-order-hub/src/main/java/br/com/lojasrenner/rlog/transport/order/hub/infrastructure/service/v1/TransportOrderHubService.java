package br.com.lojasrenner.rlog.transport.order.hub.infrastructure.service.v1;


import br.com.lojasrenner.rlog.transport.order.hub.configuration.PartnerConfiguration;
import br.com.lojasrenner.rlog.transport.order.hub.infrastructure.service.v1.util.DataUtils;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.PartnerInformationDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerConfigEntity;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerInformationEntity;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Log4j2
public class TransportOrderHubService {

	@Autowired
	private PartnerConfiguration configuration;

	@Autowired
	private PartnerInformationDBInfrastructure dbInfrastructure;

	@Autowired
	private RestTemplate restTemplate;

	public Object sendToRightPartner(Object body, String partnerId, String method) {
		PartnerConfigEntity entity = configuration.getUrlByPartnerIdAndMethod(partnerId,method);
		return restTemplate.postForEntity(entity.getUrl(),body,String.class);

	}


	public Object save(Object body, String partnerId, String method) {
		PartnerInformationEntity entity = new PartnerInformationEntity();
		entity.setBody(body);
		entity.setMethod(method);
		entity.setPartnerId(partnerId);
		entity.setReceivedDate((DataUtils.getDateTimeBR()));
		return dbInfrastructure.save(entity);
	}

}
