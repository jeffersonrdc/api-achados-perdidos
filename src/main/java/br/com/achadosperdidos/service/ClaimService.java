package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ClaimCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.controller.dto.ClaimUpdateRequest;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Local;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.entity.ClaimValidacao;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.repository.ClaimMensagemRepository;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ClaimValidacaoRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ClaimService {
    public static final String TIPO_PERDA = "PERDA";
    public static final String TIPO_RETIRADA = "RETIRADA";
    private static final Set<String> TIPOS = Set.of(TIPO_PERDA, TIPO_RETIRADA);

    private final ClaimRepository claimRepository;
    private final EventoRepository eventoRepository;
    private final CategoriaService categoriaService;
    private final LocalService localService;
    private final StatusItemService statusItemService;
    private final ClaimValidacaoRepository claimValidacaoRepository;
    private final ClaimMensagemRepository claimMensagemRepository;
    private final MatchService matchService;
    private final SignedResourceIdCodec idCodec;

    public ClaimService(ClaimRepository claimRepository, EventoRepository eventoRepository,
                        CategoriaService categoriaService, LocalService localService,
                        StatusItemService statusItemService,
                        ClaimValidacaoRepository claimValidacaoRepository,
                        ClaimMensagemRepository claimMensagemRepository,
                        MatchService matchService,
                        SignedResourceIdCodec idCodec) {
        this.claimRepository = claimRepository;
        this.eventoRepository = eventoRepository;
        this.categoriaService = categoriaService;
        this.localService = localService;
        this.statusItemService = statusItemService;
        this.claimValidacaoRepository = claimValidacaoRepository;
        this.claimMensagemRepository = claimMensagemRepository;
        this.matchService = matchService;
        this.idCodec = idCodec;
    }

    @Transactional
    public ClaimResponse create(ClaimCreateRequest request) {
        Long eventoId = idCodec.decodeEventoId(request.idEvento());
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Claim claim = new Claim();
        claim.setEvento(evento);
        claim.setTpClaim(normalizarTipo(request.tpClaim()));
        aplicarDadosBasicos(claim,
                request.idCategoria(), request.idSubcategoria(), request.idStatus(), request.nmStatus(),
                request.nmNome(), request.nrCpf(), request.nmEmail(), request.nrTelefone(),
                request.nmObjeto(), request.dsObjeto(), request.nmMarca(), request.nmModelo(),
                request.nmCor(), request.nmEstado(), request.dsTags(), request.tpPrioridade(),
                request.fgSensivel(), request.dtPerdeu(), request.hrPerdeu(),
                request.idLocal(), request.nmLocal());
        claim.setNmContatoConfianca(blankToNull(request.nmContatoConfianca()));
        claim.setNrTelefoneConfianca(blankToNull(request.nrTelefoneConfianca()));
        claim.setDsRelacaoContatoConfianca(blankToNull(request.dsRelacaoContatoConfianca()));
        claim.setNmOperador(blankToNull(request.nmOperador()));
        claim.setDsObservacao(blankToNull(request.dsObservacao()));
        claim.setDsWallpaper(blankToNull(request.dsWallpaper()));
        claim.setDtCadastro(LocalDateTime.now());
        claim.setFgAtivo(true);
        claim.setFgExcluido(false);
        claim = claimRepository.save(claim);
        claim.setCdClaim(gerarProtocolo(claim.getId(), claim.getDtCadastro()));
        claim = claimRepository.save(claim);
        matchService.recalcularMatches(claim);
        return toResponse(claimRepository.findById(claim.getId()).orElse(claim));
    }

    @Transactional
    public ClaimResponse update(String idToken, ClaimUpdateRequest request) {
        Claim claim = findEntity(idCodec.decodeClaimId(idToken));
        if (request.idCategoria() != null && !request.idCategoria().isBlank()) {
            claim.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        }
        if (request.idSubcategoria() != null) {
            claim.setSubcategoria(request.idSubcategoria().isBlank() ? null
                    : resolverSubcategoria(claim.getCategoria(), request.idSubcategoria()));
        }
        if (request.idStatus() != null && !request.idStatus().isBlank()) {
            claim.setStatus(statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus())));
        } else if (request.nmStatus() != null && !request.nmStatus().isBlank()) {
            claim.setStatus(statusItemService.findByNomeOrDefault(request.nmStatus(), "Claim Aberto"));
        }
        if (request.nmNome() != null) claim.setNmNome(request.nmNome().trim());
        if (request.nrCpf() != null) claim.setNrCpf(blankToNull(request.nrCpf()));
        if (request.nmEmail() != null) claim.setNmEmail(blankToNull(request.nmEmail()));
        if (request.nrTelefone() != null) claim.setNrTelefone(blankToNull(request.nrTelefone()));
        if (request.nmContatoConfianca() != null) claim.setNmContatoConfianca(blankToNull(request.nmContatoConfianca()));
        if (request.nrTelefoneConfianca() != null) claim.setNrTelefoneConfianca(blankToNull(request.nrTelefoneConfianca()));
        if (request.dsRelacaoContatoConfianca() != null) {
            claim.setDsRelacaoContatoConfianca(blankToNull(request.dsRelacaoContatoConfianca()));
        }
        if (request.nmObjeto() != null) claim.setNmObjeto(request.nmObjeto().trim());
        if (request.dsObjeto() != null) claim.setDsObjeto(blankToNull(request.dsObjeto()));
        if (request.dsWallpaper() != null) claim.setDsWallpaper(blankToNull(request.dsWallpaper()));
        if (request.nmMarca() != null) claim.setNmMarca(blankToNull(request.nmMarca()));
        if (request.nmModelo() != null) claim.setNmModelo(blankToNull(request.nmModelo()));
        if (request.nmCor() != null) claim.setNmCor(blankToNull(request.nmCor()));
        if (request.nmEstado() != null) claim.setNmEstado(blankToNull(request.nmEstado()));
        if (request.dsTags() != null) claim.setDsTags(blankToNull(request.dsTags()));
        if (request.tpPrioridade() != null) claim.setTpPrioridade(blankToNull(request.tpPrioridade()));
        if (request.fgSensivel() != null) claim.setFgSensivel(request.fgSensivel());
        if (request.dtPerdeu() != null) claim.setDtPerdeu(request.dtPerdeu());
        if (request.hrPerdeu() != null) claim.setHrPerdeu(request.hrPerdeu());
        if (request.idLocal() != null || request.nmLocal() != null) {
            aplicarLocal(claim, request.idLocal(), request.nmLocal());
        }
        if (request.nmOperador() != null) claim.setNmOperador(blankToNull(request.nmOperador()));
        if (request.dsObservacao() != null) claim.setDsObservacao(blankToNull(request.dsObservacao()));
        if (request.fgAtivo() != null) claim.setFgAtivo(request.fgAtivo());
        claim.setDtAlteracao(LocalDateTime.now());
        claim = claimRepository.save(claim);
        matchService.recalcularMatches(claim);
        return toResponse(claimRepository.findById(claim.getId()).orElse(claim));
    }

    /** Recalcula candidatos da coleta para o claim PERDA (novos itens podem ter entrado). */
    @Transactional
    public ClaimResponse recalcularMatches(String idToken) {
        Claim claim = findEntity(idCodec.decodeClaimId(idToken));
        matchService.recalcularMatches(claim);
        return toResponse(claimRepository.findById(claim.getId()).orElse(claim));
    }

    @Transactional(readOnly = true)
    public ApiPage<ClaimResponse> findAll(Integer page, Integer limit, String idEvento,
                                          String q, String idCategoria, String local,
                                          String status, String data, String tipo) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = (idEvento != null && !idEvento.isBlank()) ? idCodec.decodeEventoId(idEvento) : null;
        Long categoriaId = (idCategoria != null && !idCategoria.isBlank()) ? idCodec.decodeCategoriaId(idCategoria) : null;
        LocalDate dataCadastro = parseData(data);
        String tipoNorm = (tipo != null && !tipo.isBlank()) ? normalizarTipo(tipo) : null;
        boolean priorizarMatch = TIPO_PERDA.equals(tipoNorm);

        Specification<Claim> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (eventoId != null) ps.add(cb.equal(root.get("evento").get("id"), eventoId));
            if (categoriaId != null) ps.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            if (tipoNorm != null) ps.add(cb.equal(root.get("tpClaim"), tipoNorm));
            if (local != null && !local.isBlank()) ps.add(cb.equal(root.get("nmLocal"), local));
            if (status != null && !status.isBlank()) ps.add(cb.equal(root.get("status").get("nmStatus"), status));
            if (dataCadastro != null) {
                ps.add(cb.between(root.get("dtCadastro"),
                        dataCadastro.atStartOfDay(), dataCadastro.atTime(LocalTime.MAX)));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("nmNome")), like),
                        cb.like(cb.lower(root.get("nmObjeto")), like),
                        cb.like(cb.lower(root.get("nmLocal")), like),
                        cb.like(cb.lower(root.get("cdClaim")), like)));
            }
            if (priorizarMatch && query != null && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                Expression<Integer> prioridade = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("status").get("nmStatus"), MatchService.STATUS_MATCH), 0)
                        .when(cb.equal(root.get("status").get("nmStatus"), MatchService.STATUS_AGUARDANDO_MATCH), 1)
                        .when(cb.equal(root.get("status").get("nmStatus"), "Rascunho"), 2)
                        .otherwise(3);
                query.orderBy(cb.asc(prioridade), cb.desc(root.get("dtCadastro")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        PageRequest pageable = priorizarMatch
                ? PageRequest.of(p - 1, l)
                : PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "dtCadastro"));
        Page<Claim> result = claimRepository.findAll(spec, pageable);
        Map<Long, Long> naoLidas = contarNaoLidas(result.getContent().stream().map(Claim::getId).toList());
        var content = result.getContent().stream()
                .map(c -> toResponse(c, naoLidas.getOrDefault(c.getId(), 0L)))
                .toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse filtros(String idEvento, String tipo) {
        Long ev = idCodec.decodeEventoId(idEvento);
        String tipoNorm = (tipo != null && !tipo.isBlank()) ? normalizarTipo(tipo) : null;
        var categorias = claimRepository.findDistinctCategorias(ev, tipoNorm).stream()
                .map(cat -> new br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse.CategoriaArvore(
                        idCodec.encodeCategoriaId(cat.getId()), cat.getNmCategoria(), List.of()))
                .toList();
        var status = claimRepository.findDistinctStatus(ev, tipoNorm).stream()
                .map(s -> new br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse.Opcao(s.getNmStatus(), s.getNmStatus()))
                .toList();
        var locais = claimRepository.findDistinctLocais(ev, tipoNorm);
        return new br.com.achadosperdidos.controller.dto.ColetaFiltrosResponse(categorias, status, locais, List.of());
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return null;
        String v = data.trim();
        try {
            if (v.contains("/")) return LocalDate.parse(v, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return LocalDate.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public ClaimResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeClaimId(idToken)));
    }

    @Transactional(readOnly = true)
    public br.com.achadosperdidos.controller.dto.ClaimResumoResponse resumo(String idEvento, String tipo) {
        Long ev = idCodec.decodeEventoId(idEvento);
        String tipoNorm = (tipo != null && !tipo.isBlank()) ? normalizarTipo(tipo) : null;
        long total = claimRepository.countByEventoAndTipo(ev, tipoNorm);
        long abertos = claimRepository.countByEventoTipoAndStatus(
                ev, tipoNorm, List.of("Claim Aberto", MatchService.STATUS_AGUARDANDO_MATCH));
        long emAnalise = claimRepository.countByEventoTipoAndStatus(
                ev, tipoNorm, List.of("Claim em Análise", "Claim Aguardando Info", MatchService.STATUS_MATCH));
        long aprovados = claimRepository.countByEventoTipoAndStatus(ev, tipoNorm, List.of("Claim Aprovado"));
        long rejeitados = claimRepository.countByEventoTipoAndStatus(
                ev, tipoNorm, List.of("Claim Rejeitado", "Claim Cancelado"));
        return new br.com.achadosperdidos.controller.dto.ClaimResumoResponse(total, abertos, emAnalise, aprovados, rejeitados);
    }

    @Transactional
    public void softDelete(String idToken) {
        Claim claim = findEntity(idCodec.decodeClaimId(idToken));
        claim.setFgExcluido(true);
        claim.setFgAtivo(false);
        claim.setDtAlteracao(LocalDateTime.now());
        claimRepository.save(claim);
    }

    Claim findEntity(Long id) {
        return claimRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado."));
    }

    /** Preenche campos comuns (create portal/admin). */
    void aplicarDadosBasicos(Claim claim,
                             String idCategoria, String idSubcategoria, String idStatus,
                             String nmNome, String nrCpf, String nmEmail, String nrTelefone,
                             String nmObjeto, String dsObjeto, String nmMarca, String nmModelo,
                             String nmCor, String nmEstado, String dsTags, String tpPrioridade,
                             Boolean fgSensivel, LocalDate dtPerdeu, LocalTime hrPerdeu,
                             String idLocal, String nmLocal) {
        aplicarDadosBasicos(claim, idCategoria, idSubcategoria, idStatus, null,
                nmNome, nrCpf, nmEmail, nrTelefone, nmObjeto, dsObjeto, nmMarca, nmModelo,
                nmCor, nmEstado, dsTags, tpPrioridade, fgSensivel, dtPerdeu, hrPerdeu, idLocal, nmLocal);
    }

    void aplicarDadosBasicos(Claim claim,
                             String idCategoria, String idSubcategoria, String idStatus, String nmStatus,
                             String nmNome, String nrCpf, String nmEmail, String nrTelefone,
                             String nmObjeto, String dsObjeto, String nmMarca, String nmModelo,
                             String nmCor, String nmEstado, String dsTags, String tpPrioridade,
                             Boolean fgSensivel, LocalDate dtPerdeu, LocalTime hrPerdeu,
                             String idLocal, String nmLocal) {
        Categoria categoria = categoriaService.findEntity(idCodec.decodeCategoriaId(idCategoria));
        claim.setCategoria(categoria);
        claim.setSubcategoria(idSubcategoria != null && !idSubcategoria.isBlank()
                ? resolverSubcategoria(categoria, idSubcategoria) : null);
        if (idStatus != null && !idStatus.isBlank()) {
            claim.setStatus(statusItemService.findEntity(idCodec.decodeStatusId(idStatus)));
        } else {
            claim.setStatus(statusItemService.findByNomeOrDefault(nmStatus, "Claim Aberto"));
        }
        claim.setNmNome(nmNome.trim());
        claim.setNrCpf(blankToNull(nrCpf));
        claim.setNmEmail(nmEmail != null ? nmEmail.trim().toLowerCase(Locale.ROOT) : null);
        claim.setNrTelefone(blankToNull(nrTelefone));
        claim.setNmObjeto(nmObjeto.trim());
        claim.setDsObjeto(blankToNull(dsObjeto));
        claim.setNmMarca(blankToNull(nmMarca));
        claim.setNmModelo(blankToNull(nmModelo));
        claim.setNmCor(blankToNull(nmCor));
        claim.setNmEstado(blankToNull(nmEstado));
        claim.setDsTags(blankToNull(dsTags));
        claim.setTpPrioridade(blankToNull(tpPrioridade));
        claim.setFgSensivel(Boolean.TRUE.equals(fgSensivel));
        claim.setDtPerdeu(dtPerdeu);
        claim.setHrPerdeu(hrPerdeu);
        aplicarLocal(claim, idLocal, nmLocal);
    }

    void aplicarLocal(Claim claim, String idLocal, String nmLocal) {
        if (idLocal != null && !idLocal.isBlank()) {
            Local local = localService.findEntity(idCodec.decodeLocalId(idLocal));
            if (!local.getEvento().getId().equals(claim.getEvento().getId())) {
                throw new IllegalArgumentException("Local não pertence ao evento do claim.");
            }
            claim.setLocal(local);
            claim.setNmLocal(local.getNmLocal());
        } else {
            claim.setLocal(null);
            claim.setNmLocal(blankToNull(nmLocal));
        }
    }

    Categoria resolverSubcategoria(Categoria categoria, String idSubcategoria) {
        Categoria sub = categoriaService.findEntity(idCodec.decodeCategoriaId(idSubcategoria));
        if (sub.getCategoriaPai() == null || !sub.getCategoriaPai().getId().equals(categoria.getId())) {
            throw new IllegalArgumentException("Subcategoria não pertence à categoria informada.");
        }
        return sub;
    }

    String normalizarTipo(String tipo) {
        String t = tipo == null || tipo.isBlank() ? TIPO_PERDA : tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(t)) {
            throw new IllegalArgumentException("Tipo de claim inválido: " + tipo + ". Use PERDA ou RETIRADA.");
        }
        return t;
    }

    String gerarProtocolo(Long id, LocalDateTime dt) {
        int ano = (dt != null ? dt : LocalDateTime.now()).getYear();
        return "CLM-" + ano + "-" + String.format("%05d", id);
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    ClaimResponse toResponse(Claim c) {
        long naoLidas = claimMensagemRepository
                .countByClaim_IdAndTpAutorAndFgLidaOperadorFalseAndFgExcluidoFalse(c.getId(), "SOLICITANTE");
        return toResponse(c, naoLidas);
    }

    private ClaimResponse toResponse(Claim c, long qtMensagensNaoLidas) {
        List<ClaimValidacao> validacoes = claimValidacaoRepository
                .findByClaim_IdAndFgExcluidoFalseOrderByDtCadastroDesc(c.getId());
        Item itemVinculado = validacoes.stream()
                .filter(v -> MatchService.ST_CONFIRMADO.equalsIgnoreCase(v.getStResultado()))
                .map(ClaimValidacao::getItem)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseGet(() -> ClaimService.TIPO_RETIRADA.equalsIgnoreCase(c.getTpClaim())
                        ? validacoes.stream().map(ClaimValidacao::getItem)
                        .filter(java.util.Objects::nonNull).findFirst().orElse(null)
                        : null);
        long qtMatches = validacoes.stream()
                .filter(v -> MatchService.ST_PENDENTE.equalsIgnoreCase(v.getStResultado()))
                .count();
        return new ClaimResponse(
                idCodec.encodeClaimId(c.getId()),
                c.getTpClaim(),
                c.getCdClaim(),
                c.getNmNome(),
                c.getNmObjeto(),
                c.getNmMarca(),
                c.getNmModelo(),
                c.getNmCor(),
                c.getNmEstado(),
                c.getDsTags(),
                c.getTpPrioridade(),
                c.getFgSensivel(),
                c.getDtPerdeu(),
                c.getHrPerdeu(),
                c.getStatus().getNmStatus(),
                idCodec.encodeCategoriaId(c.getCategoria().getId()),
                c.getCategoria().getNmCategoria(),
                c.getSubcategoria() != null ? idCodec.encodeCategoriaId(c.getSubcategoria().getId()) : null,
                c.getSubcategoria() != null ? c.getSubcategoria().getNmCategoria() : null,
                c.getEvento().getNmEvento(),
                c.getDtCadastro(),
                c.getNrCpf(),
                c.getNmEmail(),
                c.getNrTelefone(),
                c.getNmContatoConfianca(),
                c.getNrTelefoneConfianca(),
                c.getDsRelacaoContatoConfianca(),
                c.getLocal() != null ? idCodec.encodeLocalId(c.getLocal().getId()) : null,
                c.getNmLocal(),
                c.getDsObjeto(),
                c.getDsWallpaper(),
                c.getDsDetalhesOcultos(),
                c.getNmOperador(),
                c.getDsObservacao(),
                itemVinculado != null ? idCodec.encodeItemId(itemVinculado.getId()) : null,
                itemVinculado != null ? itemVinculado.getCdItem() : null,
                c.getDsJustificativaAprovacao(),
                c.getDsJustificativaReprovacao(),
                qtMensagensNaoLidas,
                qtMatches > 0,
                qtMatches);
    }

    private Map<Long, Long> contarNaoLidas(List<Long> claimIds) {
        if (claimIds == null || claimIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : claimMensagemRepository.contarNaoLidasPorClaims(claimIds)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
