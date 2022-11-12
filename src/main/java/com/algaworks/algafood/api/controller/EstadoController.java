package com.algaworks.algafood.api.controller;

import com.algaworks.algafood.api.assembler.EstadoAssembler;
import com.algaworks.algafood.api.disassembler.EstadoDisassembler;
import com.algaworks.algafood.api.model.EstadoModel;
import com.algaworks.algafood.api.model.input.EstadoInput;
import com.algaworks.algafood.domain.exception.EntidadeEmUsoException;
import com.algaworks.algafood.domain.exception.EntidadeNaoEncontradaException;
import com.algaworks.algafood.domain.exception.NegocioException;
import com.algaworks.algafood.domain.model.Estado;
import com.algaworks.algafood.domain.service.CadastroEstadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/estados")
public class EstadoController {

    @Autowired
    private CadastroEstadoService cadastroEstadoService;

    @Autowired
    private EstadoAssembler estadoAssembler;

    @Autowired
    private EstadoDisassembler estadoDisassembler;

    @GetMapping
    public List<EstadoModel> listar() {
        return estadoAssembler.toCollectionModel(cadastroEstadoService.listar());
    }

    @GetMapping("/{id}")
    public EstadoModel buscar(@PathVariable Long id) {
        Estado estado = cadastroEstadoService.buscarOuFalhar(id);
        return estadoAssembler.toModel(estado);

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoModel adicionar(@RequestBody @Valid EstadoInput estadoInput) {
        try {
        Estado estado = estadoDisassembler.toDomainObject(estadoInput);
        return estadoAssembler.toModel(cadastroEstadoService.salvar(estado));
    } catch (EntidadeNaoEncontradaException e) {
        throw new NegocioException(e.getMessage());
    }
}

    @PutMapping("/{id}")
    public EstadoModel atualizar(@PathVariable Long id, @RequestBody @Valid EstadoInput estadoInput) {
        try {
            Estado estadoAtual = cadastroEstadoService.buscarOuFalhar(id);
            estadoDisassembler.copyToDomainObject(estadoInput,estadoAtual);

            return estadoAssembler.toModel(cadastroEstadoService.salvar(estadoAtual));
        } catch (EntidadeNaoEncontradaException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remover(@PathVariable Long id) {
        try {
            cadastroEstadoService.excluir(id);
            return ResponseEntity.noContent().build();

        } catch (EntidadeNaoEncontradaException e) {
            return ResponseEntity.notFound().build();

        } catch (EntidadeEmUsoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

}
