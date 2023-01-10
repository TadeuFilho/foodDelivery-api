package com.algaworks.algafood.api.controller;

import com.algaworks.algafood.api.assembler.FormaPagamentoAssembler;
import com.algaworks.algafood.api.disassembler.FormaPagamentoDisassembler;
import com.algaworks.algafood.api.model.FormaPagamentoModel;
import com.algaworks.algafood.api.model.input.FormaPagamentoInput;
import com.algaworks.algafood.domain.exception.EntidadeNaoEncontradaException;
import com.algaworks.algafood.domain.exception.NegocioException;
import com.algaworks.algafood.domain.model.FormaPagamento;
import com.algaworks.algafood.domain.service.CadastroFormaPagamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/formapagamento")
public class FormaPagamentoController {

    @Autowired
    private CadastroFormaPagamentoService cadastroFormaPagamentoService;

    @Autowired
    private FormaPagamentoAssembler formaPagamentoAssembler;

    @Autowired
    private FormaPagamentoDisassembler formaPagamentoDisassembler;

    @GetMapping("/{formaPagamentoId}")
    public ResponseEntity<FormaPagamentoModel> buscar(@PathVariable Long formaPagamentoId) {
        return ResponseEntity.ok(
                formaPagamentoAssembler.toModel(
                        (cadastroFormaPagamentoService.buscarOuFalhar(formaPagamentoId))));
    }

    @GetMapping()
    public ResponseEntity<List<FormaPagamentoModel>> listar() {
        return ResponseEntity.ok(formaPagamentoAssembler.toCollectionModel(cadastroFormaPagamentoService.listar()));
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public FormaPagamentoModel adicionar (@RequestBody @Valid FormaPagamentoInput formaPagamentoInput){
        try{
            FormaPagamento formaPagamento = formaPagamentoDisassembler.toDomainObject(formaPagamentoInput);
            return formaPagamentoAssembler.toModel(cadastroFormaPagamentoService.salvar(formaPagamento));
        } catch (EntidadeNaoEncontradaException e){
            throw new NegocioException(e.getMessage());
        }

    }

    @DeleteMapping("/{formaPagamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long formaPagamentoId) {
        cadastroFormaPagamentoService.excluir(formaPagamentoId);
    }

    @PutMapping("/{formaPagamentoId}")
    public FormaPagamentoModel atualizar(@PathVariable Long formaPagamentoId,
                                         @RequestBody @Valid FormaPagamentoInput formaPagamentoInput){
        try {
            FormaPagamento formaPagamentoAtual = cadastroFormaPagamentoService.buscarOuFalhar(formaPagamentoId);
            formaPagamentoDisassembler.copyToDomainObject(formaPagamentoInput,formaPagamentoAtual);
            return formaPagamentoAssembler.toModel(cadastroFormaPagamentoService.salvar(formaPagamentoAtual));
        } catch (EntidadeNaoEncontradaException e) {
            throw new NegocioException(e.getMessage());
        }
    }
}
