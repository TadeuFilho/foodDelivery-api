package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillAllowedStatusEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class FulfillmentConfig {
	private FulfillAllowedStatusEnum extraBranchStatusStrategy;
	private List<String> validBranchOfficeStatus;
	private Boolean autoReQuote;
}
