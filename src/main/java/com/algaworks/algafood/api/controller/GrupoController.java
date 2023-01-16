package com.algaworks.algafood.api.controller;

import com.algaworks.algafood.api.assembler.GrupoAssembler;
import com.algaworks.algafood.api.disassembler.GrupoDisassembler;
import com.algaworks.algafood.api.model.input.GrupoInput;
import com.algaworks.algafood.api.model.input.GrupoModel;
import com.algaworks.algafood.domain.model.Grupo;
import com.algaworks.algafood.domain.service.CadastroGrupoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/grupos")
public class GrupoController {

    @Autowired
    private CadastroGrupoService cadastroGrupoService;

    @Autowired
    private GrupoDisassembler grupoDisassembler;

    @Autowired
    private GrupoAssembler grupoAssembler;

    @GetMapping
    public List<GrupoModel> listar() {
        return grupoAssembler.toModelList(cadastroGrupoService.listar());
    }

    @GetMapping("/{grupoId}")
    public GrupoModel buscar(@PathVariable @Valid Long grupoId) {
        return grupoAssembler.toModel(cadastroGrupoService.buscarOuFalhar(grupoId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoModel salvar(@RequestBody @Valid GrupoInput grupoInput) {
        Grupo grupo = grupoDisassembler.toDomainObject(grupoInput);
        cadastroGrupoService.salvar(grupo);
        return grupoAssembler.toModel(grupo);
    }

    @DeleteMapping("/{grupoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable @Valid Long grupoId){
        cadastroGrupoService.excluir(grupoId);
    }

    @PutMapping("/{grupoId}")
    public GrupoModel atualizar(@PathVariable @Valid Long grupoId, @RequestBody @Valid GrupoInput grupoInput) {
        Grupo grupo = cadastroGrupoService.buscarOuFalhar(grupoId);
        grupoDisassembler.copyToDomainObject(grupoInput,grupo);
        cadastroGrupoService.salvar(grupo);
        return grupoAssembler.toModel(grupo);

    }

}

