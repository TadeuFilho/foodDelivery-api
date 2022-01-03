package br.com.lojasrenner.rlog.transport.order.hub.mongo.repository;

import br.com.lojasrenner.rlog.transport.order.hub.mongo.entity.PartnerConfigEntity;
import org.springframework.data.repository.CrudRepository;

public interface PartnerConfigRepository extends CrudRepository<PartnerConfigEntity,String > {

}
