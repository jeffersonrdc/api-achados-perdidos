package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.StatusItemResponse;
import br.com.achadosperdidos.entity.StatusItem;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.StatusItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StatusItemService {
    private final StatusItemRepository statusItemRepository;
    private final SignedResourceIdCodec idCodec;
    public StatusItemService(StatusItemRepository statusItemRepository, SignedResourceIdCodec idCodec) {
        this.statusItemRepository = statusItemRepository; this.idCodec = idCodec;
    }
    @Transactional(readOnly = true)
    public List<StatusItemResponse> findAll() {
        return statusItemRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc().stream().map(this::toResponse).toList();
    }
    StatusItem findEntity(Long id) {
        return statusItemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status não encontrado."));
    }
    StatusItem findByNomeOrDefault(String nmStatus, String defaultName) {
        if (nmStatus != null && !nmStatus.isBlank()) {
            return statusItemRepository.findByNmStatus(nmStatus.trim())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status não encontrado: " + nmStatus));
        }
        return statusItemRepository.findByNmStatus(defaultName)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status padrão não encontrado: " + defaultName));
    }
    private StatusItemResponse toResponse(StatusItem s) {
        return new StatusItemResponse(idCodec.encodeStatusId(s.getId()), s.getNmStatus(), s.getDsStatus(), s.getOrOrdem(), s.getFgFinal());
    }
}
