package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OriginPreview {
	private String branchId;
	private List<String> skus;
}
