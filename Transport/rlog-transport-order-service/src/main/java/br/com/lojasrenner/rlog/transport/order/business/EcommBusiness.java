package br.com.lojasrenner.rlog.transport.order.business;

import java.util.List;
import java.util.Optional;

import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownBranchOfficeException;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;

@Component
public class EcommBusiness {
	
	@Autowired
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Autowired
	private LiveConfig config;

	public BranchOfficeEntity getEcommBranchOffice(String companyId, String channel) {
		List<BranchOfficeEntity> branchOffices = branchOfficeService
				.getEcommBranchOffices(companyId);

		String mainEcomm = config.getConfigValueString(companyId, Optional.ofNullable(channel),
				c -> c.getMainEcomm().getBranchOfficeId(),
				true);

		return branchOffices.stream()
				.filter(b -> b.getBranchOfficeId().equals(mainEcomm))
				.findFirst()
				.orElseThrow(() -> new UnknownBranchOfficeException("There is no branch office WEB_STORE for companyId " + companyId + " with branchOfficeId " + mainEcomm, "500"));
	}

	public List<BranchOfficeEntity> getAllEcommBranchOffices(String companyId, String channel){
		return branchOfficeService.getEcommBranchOffices(companyId);
	}
}
