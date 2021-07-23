package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import java.util.Map;

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
public class GeoLocationResponseV1 {
	
    private String branchOfficeId;
    private boolean inRange;
    private int distance;
    private Double currentLimitKm;
    
    private Map<String, Object> settings;
    
}
