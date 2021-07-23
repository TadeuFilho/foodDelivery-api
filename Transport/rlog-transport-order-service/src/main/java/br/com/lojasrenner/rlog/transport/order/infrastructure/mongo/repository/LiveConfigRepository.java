package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.LiveConfigEntity;

public interface LiveConfigRepository extends CrudRepository<LiveConfigEntity, String> {

}
