package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchOfficeSortResponseV1 {
    private boolean sorted;
    private boolean unsorted;
    private boolean empty;
}
