package com.algaworks.algafood;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CadastroCozinhaIT {

	@LocalServerPort
	private int port;

	@Test
	public void deveRetornarStatus200_QuandoConsultarCozinhas() {
		enableLoggingOfRequestAndResponseIfValidationFails();
			given()
				.basePath("/cozinhas")
				.port(port)
				.accept(ContentType.JSON)
			.when()
				.get()
			.then()
				.statusCode(HttpStatus.OK.value());
	}














//	@Autowired
//	private CadastroCozinhaService cadastroCozinha;
//
//	// apenas exemplos
//	public void testarCadastroCozinhaComSucesso() {
//		//cenário
//		Cozinha novaCozinha = new Cozinha();
//		novaCozinha.setNome("Chinesa");
//
//		//ação
//		novaCozinha = cadastroCozinha.salvar(novaCozinha);
//
//		//validação
//		assertThat(novaCozinha).isNotNull();
//		assertThat(novaCozinha.getId()).isNotNull();
//
//	}
//
//	public void deveFalharAoExcluirCozinhaInexistente() {
//
//		CozinhaNaoEncontradaException exceptionEsperada = assertThrows(CozinhaNaoEncontradaException.class, () -> {
//			cadastroCozinha.excluir(97l);
//		});
//
//		assertThat(exceptionEsperada).isNotNull();
//	}


}
