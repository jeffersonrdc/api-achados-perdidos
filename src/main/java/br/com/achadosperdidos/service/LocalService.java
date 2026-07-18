package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.LocalCreateRequest;
import br.com.achadosperdidos.controller.dto.LocalResponse;
import br.com.achadosperdidos.controller.dto.LocalUpdateRequest;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Local;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.LocalRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class LocalService {
    private static final Set<String> TIPOS = Set.of("ACHADO", "COLETA", "DEPOSITO", "ATENDIMENTO", "OPERACIONAL");

    private final LocalRepository localRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public LocalService(LocalRepository localRepository, EventoRepository eventoRepository,
                        UsuarioRepository usuarioRepository, SignedResourceIdCodec idCodec,
                        AuditoriaContextService auditoriaContext) {
        this.localRepository = localRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    @Transactional
    public LocalResponse create(LocalCreateRequest request) {
        auditoriaContext.marcarContexto();
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(request.idEvento()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Local l = new Local();
        l.setEvento(evento);
        l.setNmLocal(request.nmLocal().trim());
        l.setTpLocal(validarTipo(request.tpLocal()));
        if (request.idResponsavel() != null && !request.idResponsavel().isBlank()) {
            l.setResponsavel(usuarioRepository.findById(idCodec.decodeUsuarioId(request.idResponsavel()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Responsável não encontrado.")));
        }
        l.setVlLatitude(request.vlLatitude());
        l.setVlLongitude(request.vlLongitude());
        l.setNmHorario(request.nmHorario());
        l.setDsObservacao(request.dsObservacao());
        l.setDtCadastro(LocalDateTime.now());
        l.setFgAtivo(request.fgAtivo() == null || Boolean.TRUE.equals(request.fgAtivo()));
        l.setFgExcluido(false);
        return toResponse(localRepository.save(l));
    }

    @Transactional(readOnly = true)
    public List<LocalResponse> findByEvento(String idEvento) {
        return localRepository.findByEvento_IdAndFgExcluidoFalseOrderByNmLocalAsc(idCodec.decodeEventoId(idEvento))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LocalResponse findById(String id) {
        return toResponse(findEntity(idCodec.decodeLocalId(id)));
    }

    @Transactional
    public LocalResponse update(String id, LocalUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Local l = findEntity(idCodec.decodeLocalId(id));
        if (request.nmLocal() != null) l.setNmLocal(request.nmLocal().trim());
        if (request.tpLocal() != null) l.setTpLocal(validarTipo(request.tpLocal()));
        if (request.idResponsavel() != null) {
            l.setResponsavel(request.idResponsavel().isBlank() ? null
                    : usuarioRepository.findById(idCodec.decodeUsuarioId(request.idResponsavel()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Responsável não encontrado.")));
        }
        if (request.vlLatitude() != null) l.setVlLatitude(request.vlLatitude());
        if (request.vlLongitude() != null) l.setVlLongitude(request.vlLongitude());
        if (request.nmHorario() != null) l.setNmHorario(request.nmHorario());
        if (request.dsObservacao() != null) l.setDsObservacao(request.dsObservacao());
        if (request.fgAtivo() != null) l.setFgAtivo(request.fgAtivo());
        l.setDtAlteracao(LocalDateTime.now());
        return toResponse(localRepository.save(l));
    }

    @Transactional
    public void softDelete(String id) {
        auditoriaContext.marcarContexto();
        Local l = findEntity(idCodec.decodeLocalId(id));
        l.setFgExcluido(true);
        l.setFgAtivo(false);
        l.setDtAlteracao(LocalDateTime.now());
        localRepository.save(l);
    }

    Local findEntity(Long id) {
        return localRepository.findById(id)
                .filter(l -> !Boolean.TRUE.equals(l.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
    }

    private String validarTipo(String tipo) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!TIPOS.contains(t)) {
            throw new IllegalArgumentException("Tipo de local inválido: " + tipo + ". Use " + String.join(", ", TIPOS) + ".");
        }
        return t;
    }

    private LocalResponse toResponse(Local l) {
        return new LocalResponse(
                idCodec.encodeLocalId(l.getId()),
                idCodec.encodeEventoId(l.getEvento().getId()),
                l.getNmLocal(),
                l.getTpLocal(),
                l.getResponsavel() != null ? idCodec.encodeUsuarioId(l.getResponsavel().getId()) : null,
                l.getResponsavel() != null ? l.getResponsavel().getNmUsuario() : null,
                l.getVlLatitude(),
                l.getVlLongitude(),
                l.getNmHorario(),
                l.getDsObservacao(),
                l.getFgAtivo());
    }
}
