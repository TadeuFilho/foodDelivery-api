package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LocationStockV1Request {
    private List<String> branchesOfficeId;

    private List<String> skus;
}
