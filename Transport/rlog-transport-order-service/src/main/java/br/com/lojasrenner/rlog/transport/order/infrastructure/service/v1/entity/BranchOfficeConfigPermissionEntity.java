package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BranchOfficeConfigPermissionEntity {
    private Boolean orderReceive;
    private Boolean doShipping;
    private Boolean branchWithdraw;
    private Boolean branchWithdrawStockCD;
}
