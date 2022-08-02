package br.com.lojasrenner.rlog.transport.order.hub.configuration;

import br.com.lojasrenner.rlog.transport.order.hub.mongo.PartnerConfigDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerConfigEntity;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Configuration
public class PartnerConfiguration {

    @Autowired
    private PartnerConfigDBInfrastructure db;

    private Map<String, PartnerConfigEntity> map;

    @PostConstruct
    @Scheduled(fixedRateString = "${rlog.loader.ttl.partner-platform-config:300000}",
            initialDelayString = "${rlog.loader.ttl.partner-platform-config:1000}")
    public void updateConfigMap() {
    log.info("Execution Partner Config Map");
    Map<String, PartnerConfigEntity> newMap = new ConcurrentHashMap<>();
        Optional<Map<String, PartnerConfigEntity>> allCompanyAndPartnerPlatformOptionalMap = Optional.ofNullable(db.findAll());
        allCompanyAndPartnerPlatformOptionalMap.ifPresent(partnerPlatformConfigEntity -> newMap.putAll(allCompanyAndPartnerPlatformOptionalMap.get()));
        map = newMap;
    }

    public PartnerConfigEntity getUrlByPartnerIdAndMethod(String partnerId, String method) {
        return Objects.nonNull(map.get(method + "-" + partnerId)) ? map.get(method + "-" + partnerId) : null;
   }

}
