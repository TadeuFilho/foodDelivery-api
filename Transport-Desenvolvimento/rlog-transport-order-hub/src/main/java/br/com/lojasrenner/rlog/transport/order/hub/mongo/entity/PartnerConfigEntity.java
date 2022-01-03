package br.com.lojasrenner.rlog.transport.order.hub.mongo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("partnerMethodConfig")
@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerConfigEntity {

    @Id
    protected String id;

    private String partnerPlatformId;
    private String method;
    private String url;
}
