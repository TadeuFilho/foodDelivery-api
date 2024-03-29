package com.algaworks.algafood.api.controller;

import com.algaworks.algafood.api.assembler.CozinhaAssembler;
import com.algaworks.algafood.api.disassembler.CozinhaInputDisassembler;
import com.algaworks.algafood.api.model.CozinhaModel;
import com.algaworks.algafood.api.model.input.CozinhaInput;
import com.algaworks.algafood.domain.exception.EntidadeNaoEncontradaException;
import com.algaworks.algafood.domain.exception.NegocioException;
import com.algaworks.algafood.domain.model.Cozinha;
import com.algaworks.algafood.domain.service.CadastroCozinhaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/cozinhas")
public class CozinhaController {

    @Autowired
    private CadastroCozinhaService cadastroCozinha;

    @Autowired
    private CozinhaAssembler cozinhaAssembler;

    @Autowired
    private CozinhaInputDisassembler cozinhaInputDisassembler;

    @GetMapping
    public List<CozinhaModel> listar() {
        return cozinhaAssembler.toCollectionModel(cadastroCozinha.listar());
    }

    @GetMapping("/{cozinhaId}")
    public CozinhaModel buscar(@PathVariable Long cozinhaId) {
        Cozinha cozinha = cadastroCozinha.buscarOuFalhar(cozinhaId);
        return cozinhaAssembler.toModel(cozinha);


    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CozinhaModel adicionar(@RequestBody @Valid CozinhaInput cozinhaInput) {
        try {
            Cozinha cozinha = cozinhaInputDisassembler.toDomainObject(cozinhaInput);
            return cozinhaAssembler.toModel(cadastroCozinha.salvar(cozinha));
        } catch (EntidadeNaoEncontradaException e) {
            throw new NegocioException(e.getMessage());
        }

    }

    @PutMapping("/{cozinhaId}")
    public CozinhaModel atualizar(@PathVariable Long cozinhaId, @RequestBody @Valid CozinhaInput cozinhaInput) {

        try {
            Cozinha cozinhaAtual = cadastroCozinha.buscarOuFalhar(cozinhaId);
            cozinhaInputDisassembler.copyToDomainObject(cozinhaInput, cozinhaAtual);

            return cozinhaAssembler.toModel(cadastroCozinha.salvar(cozinhaAtual));
        } catch (EntidadeNaoEncontradaException e) {
            throw new NegocioException(e.getMessage());
        }


    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        cadastroCozinha.excluir(id);
    }
}
