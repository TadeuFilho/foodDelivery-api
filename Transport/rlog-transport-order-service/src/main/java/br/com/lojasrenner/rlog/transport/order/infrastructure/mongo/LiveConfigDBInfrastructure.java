package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.LiveConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository.LiveConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class LiveConfigDBInfrastructure {

    @Autowired
    private LiveConfigRepository liveConfigRepository;

    public Optional<LiveConfigEntity> findById(String id) {
        return liveConfigRepository.findById(id);
    }

    public Optional<LiveConfigEntity> findByIdAndChannel(String id, String channel) {
        LiveConfigEntity finalLiveConfigEntity = new LiveConfigEntity();
        Optional<LiveConfigEntity> liveConfigEntity = liveConfigRepository.findById(id);

        if (liveConfigEntity.isPresent()) {
            Optional<Map.Entry<String, CompanyConfigEntity>> mapChannelConfig = liveConfigEntity.get().getBusiness().getChannelConfig()
                    .entrySet()
                    .stream()
                    .filter(i -> i.getKey().equals(channel)).findFirst();

            mapChannelConfig.ifPresent(companyConfig -> finalLiveConfigEntity.setBusiness(companyConfig.getValue()));
        }

        return Optional.of(finalLiveConfigEntity);
    }

    public Map<String, LiveConfigEntity> findAll() {
        Map<String, LiveConfigEntity> liveConfigEntities = new HashMap<>();
        liveConfigRepository.findAll().forEach(entity -> liveConfigEntities.put(entity.getId(), entity));
        return liveConfigEntities;
    }

    public LiveConfigEntity save(LiveConfigEntity entity) {
        return liveConfigRepository.save(entity);
    }

}
