package br.com.lojasrenner.rlog.transport.order.hub.mongo;

import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerConfigEntity;
import br.com.lojasrenner.rlog.transport.order.hub.mongo.repository.PartnerConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PartnerConfigDBInfrastructure {

    @Autowired
    private PartnerConfigRepository repository;

    public PartnerConfigEntity save(PartnerConfigEntity transportEntity) {
        return repository.save(transportEntity);
    }

    public Optional<PartnerConfigEntity> findById(String id) {
        return repository.findById(id);
    }

    public Map<String, PartnerConfigEntity> findAll() {
        Map<String, PartnerConfigEntity> configEntities = new HashMap<>();
        repository.findAll().forEach(entity -> configEntities.put(entity.getMethod() + "-" + entity.getPartnerPlatformId(), entity));
        return configEntities;
    }


}
