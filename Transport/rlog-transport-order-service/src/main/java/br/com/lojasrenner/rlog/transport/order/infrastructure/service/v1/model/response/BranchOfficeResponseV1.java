package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BranchOfficeResponseV1 {
    private List<BranchOfficeEntity> content;
    private BranchOfficePageableResponseV1 pageable;
    private int totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
    private BranchOfficeSortResponseV1 sort;
    private int size;
    private int number;
    private int numberOfElements;
    private boolean empty;
}
