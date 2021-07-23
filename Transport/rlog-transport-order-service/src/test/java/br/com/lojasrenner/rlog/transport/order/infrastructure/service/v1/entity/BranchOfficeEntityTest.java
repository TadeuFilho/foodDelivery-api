package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.CountryEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeConfigurationEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
public class BranchOfficeEntityTest {
	private final static String zipCode = "01311-000";

	@Test
	public void test_quotationZipCode_null_deve_retornar_o_zipcode_padrao() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode(null).build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_NonNull_deve_retornar_o_quotationZipCode() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("01245000").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getConfiguration().getQuotationZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_maior_que_o_limite_deve_retornar_o_zipcode_padrao_no_lugar() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("999999999").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_menor_que_o_limite_deve_retornar_o_zipcode_padrao_no_lugar() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("00245000").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_que_contem_letras_deve_retornar_o_zipcode_padrao_no_lugar() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("01245Ab0").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_com_hifen_deve_normalizar_e_retornar_o_quotationZipCode() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("01245-000").build()).build();

		String expectedZipcode = branch.getConfiguration().getQuotationZipcode().replace("-", "");
		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(expectedZipcode, finalZipcode);
	}

	@Test
	public void test_quotationZipCode_que_contem_pontos_deve_normalizar_e_retornar_o_quotationZipCode() {
		String quotationZipCode = "01245000";

		String zipcodeWithDots = quotationZipCode.substring(0, 2) + "." + quotationZipCode.substring(2);
		zipcodeWithDots = zipcodeWithDots.substring(0, 5) + "." + zipcodeWithDots.substring(5);

		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode(zipcodeWithDots).build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(quotationZipCode, finalZipcode);
	}

	@Test
	public void test_quotationZipCode_que_contem_espacos_deve_normalizar_e_retornar_o_quotationZipCode() {
		String quotationZipCode = "01245000";

		StringBuilder zipcodeWithSpaces = new StringBuilder(quotationZipCode);
		zipcodeWithSpaces.insert(0, " ");
		zipcodeWithSpaces.insert(3, " ");
		zipcodeWithSpaces.insert(6, " ");
		zipcodeWithSpaces.append(" ");

		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode(zipcodeWithSpaces.toString()).build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(quotationZipCode, finalZipcode);
	}

	@Test
	public void test_quotationZipCode_omitindo_zeros_a_esquerda_deve_retornar_o_quotationZipCode() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("1245000").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getConfiguration().getQuotationZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_string_vazia_deve_retornar_o_zipcode_padrao_no_lugar() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode(zipCode).configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode(" ").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.BR));

		assertNotNull(finalZipcode);
		assertEquals(branch.getZipcode(), finalZipcode);
	}

	@Test
	public void test_quotationZipCode_uruguai_nao_implementado_deve_usar_o_padrao() {
		BranchOfficeEntity branch = BranchOfficeEntity.builder().zipcode("11111111").configuration(BranchOfficeConfigurationEntity.builder().quotationZipcode("22222222").build()).build();

		String finalZipcode = branch.getQuoteZipCode(Optional.of(CountryEnum.UY));

		assertNotNull(finalZipcode);
		assertEquals("11111111", finalZipcode);
	}

}
