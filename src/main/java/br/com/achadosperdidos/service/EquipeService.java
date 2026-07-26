package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EquipeCreateRequest;
import br.com.achadosperdidos.controller.dto.EquipeMembroRequest;
import br.com.achadosperdidos.controller.dto.EquipeMembroResponse;
import br.com.achadosperdidos.controller.dto.EquipeResponse;
import br.com.achadosperdidos.controller.dto.EquipeUpdateRequest;
import br.com.achadosperdidos.entity.Equipe;
import br.com.achadosperdidos.entity.EquipeUsuario;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.EquipeRepository;
import br.com.achadosperdidos.repository.EquipeUsuarioRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class EquipeService {
    private static final Set<String> TIPOS = Set.of(
            "COLETA", "TRIAGEM", "ESTOQUE", "ATENDIMENTO", "SUPERVISAO", "ADMINISTRACAO");

    private final EquipeRepository equipeRepository;
    private final EquipeUsuarioRepository equipeUsuarioRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LocalService localService;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public EquipeService(EquipeRepository equipeRepository, EquipeUsuarioRepository equipeUsuarioRepository,
                         EventoRepository eventoRepository, UsuarioRepository usuarioRepository,
                         LocalService localService, SignedResourceIdCodec idCodec,
                         AuditoriaContextService auditoriaContext) {
        this.equipeRepository = equipeRepository;
        this.equipeUsuarioRepository = equipeUsuarioRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.localService = localService;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    @Transactional
    public EquipeResponse create(EquipeCreateRequest request) {
        auditoriaContext.marcarContexto();
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(request.idEvento()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Equipe e = new Equipe();
        e.setEvento(evento);
        e.setNmEquipe(request.nmEquipe().trim());
        e.setTpEquipe(validarTipo(request.tpEquipe()));
        if (request.idLocal() != null && !request.idLocal().isBlank()) {
            e.setLocal(localService.findEntity(idCodec.decodeLocalId(request.idLocal())));
        }
        e.setDsResponsabilidade(request.dsResponsabilidade());
        e.setDtCadastro(LocalDateTime.now());
        e.setFgAtivo(true);
        e.setFgExcluido(false);
        return toResponse(equipeRepository.save(e));
    }

    @Transactional(readOnly = true)
    public ApiPage<EquipeResponse> findByEvento(String idEvento, Integer page, Integer limit,
                                                String q, String tpEquipe) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Specification<Equipe> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("evento").get("id"), eventoId));
            if (tpEquipe != null && !tpEquipe.isBlank()) {
                ps.add(cb.equal(root.get("tpEquipe"), tpEquipe.trim().toUpperCase()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmEquipe")), like),
                        cb.like(cb.lower(root.get("dsResponsabilidade")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Equipe> result = equipeRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "nmEquipe")));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public EquipeResponse findById(String id) {
        return toResponse(findEntity(idCodec.decodeEquipeId(id)));
    }

    @Transactional
    public EquipeResponse update(String id, EquipeUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Equipe e = findEntity(idCodec.decodeEquipeId(id));
        if (request.nmEquipe() != null) e.setNmEquipe(request.nmEquipe().trim());
        if (request.tpEquipe() != null) e.setTpEquipe(validarTipo(request.tpEquipe()));
        if (request.idLocal() != null) {
            e.setLocal(request.idLocal().isBlank() ? null
                    : localService.findEntity(idCodec.decodeLocalId(request.idLocal())));
        }
        if (request.dsResponsabilidade() != null) e.setDsResponsabilidade(request.dsResponsabilidade());
        if (request.fgAtivo() != null) e.setFgAtivo(request.fgAtivo());
        e.setDtAlteracao(LocalDateTime.now());
        return toResponse(equipeRepository.save(e));
    }

    @Transactional
    public void softDelete(String id) {
        auditoriaContext.marcarContexto();
        Equipe e = findEntity(idCodec.decodeEquipeId(id));
        e.setFgExcluido(true);
        e.setFgAtivo(false);
        e.setDtAlteracao(LocalDateTime.now());
        equipeRepository.save(e);
    }

    @Transactional
    public EquipeMembroResponse adicionarMembro(String idEquipe, EquipeMembroRequest request) {
        auditoriaContext.marcarContexto();
        Equipe equipe = findEntity(idCodec.decodeEquipeId(idEquipe));
        Usuario usuario = usuarioRepository.findById(idCodec.decodeUsuarioId(request.idUsuario()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        // Reaproveita o vinculo existente (inclusive se estiver soft-deleted) para
        // respeitar a UNIQUE(IDR_Equipe, IDR_Usuario) ao re-adicionar um membro.
        EquipeUsuario vinculo = equipeUsuarioRepository
                .findByEquipe_IdAndUsuario_Id(equipe.getId(), usuario.getId())
                .orElseGet(EquipeUsuario::new);
        if (vinculo.getId() == null) {
            vinculo.setEquipe(equipe);
            vinculo.setUsuario(usuario);
            vinculo.setDtCadastro(LocalDateTime.now());
        }
        vinculo.setFgAtivo(true);
        vinculo.setFgExcluido(false);
        vinculo.setDtAlteracao(LocalDateTime.now());
        return toMembroResponse(equipeUsuarioRepository.save(vinculo));
    }

    @Transactional
    public void removerMembro(String idEquipe, String idUsuario) {
        auditoriaContext.marcarContexto();
        Long equipeId = idCodec.decodeEquipeId(idEquipe);
        Long usuarioId = idCodec.decodeUsuarioId(idUsuario);
        EquipeUsuario vinculo = equipeUsuarioRepository
                .findByEquipe_IdAndUsuario_IdAndFgExcluidoFalse(equipeId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo de equipe não encontrado."));
        vinculo.setFgExcluido(true);
        vinculo.setFgAtivo(false);
        vinculo.setDtAlteracao(LocalDateTime.now());
        equipeUsuarioRepository.save(vinculo);
    }

    Equipe findEntity(Long id) {
        return equipeRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe não encontrada."));
    }

    private String validarTipo(String tipo) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!TIPOS.contains(t)) {
            throw new IllegalArgumentException("Tipo de equipe inválido: " + tipo + ". Use " + String.join(", ", TIPOS) + ".");
        }
        return t;
    }

    private EquipeResponse toResponse(Equipe e) {
        List<EquipeMembroResponse> membros = equipeUsuarioRepository
                .findByEquipe_IdAndFgExcluidoFalseOrderByUsuario_NmUsuarioAsc(e.getId())
                .stream().map(this::toMembroResponse).toList();
        return new EquipeResponse(
                idCodec.encodeEquipeId(e.getId()),
                idCodec.encodeEventoId(e.getEvento().getId()),
                e.getNmEquipe(),
                e.getTpEquipe(),
                e.getLocal() != null ? idCodec.encodeLocalId(e.getLocal().getId()) : null,
                e.getLocal() != null ? e.getLocal().getNmLocal() : null,
                e.getDsResponsabilidade(),
                e.getFgAtivo(),
                membros);
    }

    private EquipeMembroResponse toMembroResponse(EquipeUsuario eu) {
        return new EquipeMembroResponse(
                idCodec.encodeEquipeUsuarioId(eu.getId()),
                idCodec.encodeUsuarioId(eu.getUsuario().getId()),
                eu.getUsuario().getNmUsuario(),
                eu.getUsuario().getNmEmail());
    }
}
