package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchOfficePageableResponseV1 {
    private BranchOfficeSortResponseV1 sort;
    private int offset;
    private int pageSize;
    private int pageNumber;
    private boolean paged;
    private boolean unpaged;
}
