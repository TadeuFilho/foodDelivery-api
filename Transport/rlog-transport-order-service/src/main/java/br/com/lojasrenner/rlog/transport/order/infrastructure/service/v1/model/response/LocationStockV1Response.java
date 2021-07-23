package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class LocationStockV1Response {
    private String companyId;
    private String branchOfficeId;
    private String branchOfficeStatus;
    private List<LocationStockItemV1Response> items;
    
    private String fingerPrint;
    private int positionBasedOnPriority;
    private int okCount;
    private List<String> okItems;
    private List<String> remainderOkItems;
    private int groupIndex;
    
	public enum BranchOfficeStatus {
		OK,
		RESOURCE_NOT_ENOUGH,
		SKU_NOT_FOUND,
		REFERENCE_ID_NOT_FOUND,
		RELEASE_MORE_THEN_ZERO,
		SKU_ALREADY_RESERVED,
		UNKNOWN_PROBLEM,
		BRANCH_OFFICE_INACTIVE,
		BRANCH_OFFICE_NOT_FOUND,
		BRANCH_OFFICE_NOT_ALLOW_RECEIVE_RESERVATION,
		BRANCH_OFFICE_NOT_ALLOW_RECEIVE_ORDER,
		STOCK_ITEM_IN_BLACKLIST,
		BRANCH_OFFICE_ORDER_OR_RESERVATION_EXCEEDED
	}
    
}
