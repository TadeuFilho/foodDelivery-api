package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("liveConfig")
@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveConfigEntity {

	@Id
	protected String id;

	private CompanyConfigEntity business;
}
