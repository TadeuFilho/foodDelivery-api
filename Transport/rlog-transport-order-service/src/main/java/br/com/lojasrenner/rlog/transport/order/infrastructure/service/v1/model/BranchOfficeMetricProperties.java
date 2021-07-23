package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BranchOfficeMetricProperties {
	private String branchOfficeId;
	private String companyId;
	private String cdManagement;
	private String branchType;
}
