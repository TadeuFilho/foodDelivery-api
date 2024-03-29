package com.algaworks.algafood.api.disassembler;

import com.algaworks.algafood.api.model.input.UsuarioComSenhaInput;
import com.algaworks.algafood.api.model.input.UsuarioInput;
import com.algaworks.algafood.domain.model.Usuario;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsuarioDisassembler {

    @Autowired
    private ModelMapper modelMapper;

    public Usuario toDomainObject(UsuarioInput usuarioInput){
       return modelMapper.map(usuarioInput,Usuario.class);
    }

    public Usuario toDomainObjectWithPassword(UsuarioComSenhaInput usuarioComSenhaInput) {
        return modelMapper.map(usuarioComSenhaInput, Usuario.class);
    }

    public void copyToDomainObject(UsuarioInput usuarioInput, Usuario usuario) {
        modelMapper.map(usuarioInput,usuario);
    }

}
