package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShippingToResponseV1 {

    private String id;
    private String destinationBranch;
    private List<String> originBranches;
    private Integer freightTime;
    private String fileId;
    private String groupId;

}
