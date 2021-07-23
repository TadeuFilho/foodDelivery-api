package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class ShippingGroupResponseV1 {

	private String id;
	private String name;
	private String companyId;
	private int priority;
	private List<Integer> branches;
	private List<ZipCodeRange> zipCodeRanges;
	private Boolean statePriority;
	
	@Builder.Default
	private Map<Integer, Map<String, Object>> settings = new HashMap<>();
	
}
