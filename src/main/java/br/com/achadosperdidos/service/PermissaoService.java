package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.PermissaoResponse;
import br.com.achadosperdidos.entity.Permissao;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.PermissaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissaoService {
    private final PermissaoRepository permissaoRepository;

    public PermissaoService(PermissaoRepository permissaoRepository) {
        this.permissaoRepository = permissaoRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissaoResponse> listarCatalogo() {
        return permissaoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByNmModuloAscNmAcaoAsc()
                .stream().map(this::toResponse).toList();
    }

    Permissao findByNome(String nmPermissao) {
        return permissaoRepository.findByNmPermissao(nmPermissao.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Permissão não encontrada: " + nmPermissao));
    }

    PermissaoResponse toResponse(Permissao p) {
        return new PermissaoResponse(p.getNmPermissao(), p.getNmModulo(), p.getNmAcao(), p.getDsPermissao());
    }
}
