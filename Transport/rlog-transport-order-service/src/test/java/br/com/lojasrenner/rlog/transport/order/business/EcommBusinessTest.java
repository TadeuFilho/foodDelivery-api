package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownBranchOfficeException;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
public class EcommBusinessTest {

    @InjectMocks
    private EcommBusiness ecommBusiness;

    @Mock
    private BranchOfficeCachedServiceV1 branchOfficeService;

    @Mock
    private LiveConfig config;

    private static final String COMPANY_ID_RENNER = "001";

    @Test
    public void getEcommBranchOfficeTest() {

        String ecommBranchOffice = "777";

        Map<String, BranchOfficeEntity> branchOffices = Map.of(
                "889", BranchOfficeEntity.builder().id("000889").build(),
                "777", BranchOfficeEntity.builder().id("000777").build(),
                "900", BranchOfficeEntity.builder().id("000900").build()
        );

        Mockito.when(branchOfficeService
                .getEcommBranchOffices(COMPANY_ID_RENNER)).thenReturn(branchOffices.values().stream().collect(Collectors.toList()));

        Mockito.when(config.getConfigValueString(eq(COMPANY_ID_RENNER), any(), any(), eq(Boolean.TRUE)))
                .thenReturn(ecommBranchOffice);

        final BranchOfficeEntity response =
                ecommBusiness.getEcommBranchOffice(COMPANY_ID_RENNER, "teste");

        Assert.assertEquals(branchOffices.get(ecommBranchOffice), response);
    }

    @Test(expected = UnknownBranchOfficeException.class)
    public void getEcommBranchOfficeWhenEcommBranchOfficeNotFoundTest() {

        String ecommBranchOffice = "123";

        Map<String, BranchOfficeEntity> branchOffices = Map.of(
                "889", BranchOfficeEntity.builder().id("000889").build(),
                "777", BranchOfficeEntity.builder().id("000777").build(),
                "900", BranchOfficeEntity.builder().id("000900").build()
        );

        Mockito.when(branchOfficeService
                .getEcommBranchOffices(COMPANY_ID_RENNER)).thenReturn(branchOffices.values().stream().collect(Collectors.toList()));

        Mockito.when(config.getConfigValueString(eq(COMPANY_ID_RENNER), any(), any(), eq(Boolean.TRUE)))
                .thenReturn(ecommBranchOffice);

        final BranchOfficeEntity response =
                ecommBusiness.getEcommBranchOffice(COMPANY_ID_RENNER, "teste");
    }

}
