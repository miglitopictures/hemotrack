package com.hemotrack.backend.model.instituicao.dto;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;

public record InstituicaoResponse(Long id, String razaoSocial, String cnpj,
                                  TipoInstituicao tipo, StatusInstituicao status,
                                  String endereco, String municipio, String telefone) {
    

    public static InstituicaoResponse de(Instituicao i) {
        InstituicaoResponse convertedFromInstituicao = new InstituicaoResponse(i.getId(), i.getRazaoSocial(), i.getCnpj(), i.getTipo(), i.getStatus(), i.getEndereco(), i.getMunicipio(), i.getTelefone());
        return convertedFromInstituicao;
    }


}
