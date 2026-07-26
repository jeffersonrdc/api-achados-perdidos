package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemDisponivelResponse;
import br.com.achadosperdidos.controller.dto.TransferenciaCreateRequest;
import br.com.achadosperdidos.controller.dto.TransferenciaResponse;
import br.com.achadosperdidos.controller.dto.TransferenciaResumoResponse;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.Local;
import br.com.achadosperdidos.entity.Transferencia;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.repository.TransferenciaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransferenciaService {

    private static final String STATUS_CONCLUIDA = "CONCLUIDA";

    private final TransferenciaRepository transferenciaRepository;
    private final ItemRepository itemRepository;
    private final LocalService localService;
    private final UsuarioContextService usuarioContextService;
    private final AuditoriaContextService auditoriaContext;
    private final SignedResourceIdCodec idCodec;

    public TransferenciaService(TransferenciaRepository transferenciaRepository, ItemRepository itemRepository,
                                LocalService localService, UsuarioContextService usuarioContextService,
                                AuditoriaContextService auditoriaContext, SignedResourceIdCodec idCodec) {
        this.transferenciaRepository = transferenciaRepository;
        this.itemRepository = itemRepository;
        this.localService = localService;
        this.usuarioContextService = usuarioContextService;
        this.auditoriaContext = auditoriaContext;
        this.idCodec = idCodec;
    }

    /** Registra a transferência dos itens: origem -> destino. Persiste, audita e atualiza o local atual do item. */
    @Transactional
    public List<TransferenciaResponse> criar(TransferenciaCreateRequest request) {
        auditoriaContext.marcarContexto();
        Local destino = localService.findEntity(idCodec.decodeLocalId(request.idLocalDestino()));
        Local origemPadrao = (request.idLocalOrigem() != null && !request.idLocalOrigem().isBlank())
                ? localService.findEntity(idCodec.decodeLocalId(request.idLocalOrigem()))
                : null;
        Usuario responsavel = usuarioLogadoOuNulo();
        LocalDateTime agora = LocalDateTime.now();

        List<TransferenciaResponse> resultado = new ArrayList<>();
        for (String idItem : request.idsItens()) {
            Item item = itemRepository.findById(idCodec.decodeItemId(idItem))
                    .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
            Local origem = origemPadrao != null ? origemPadrao : item.getLocalAtual();

            Transferencia t = new Transferencia();
            t.setEvento(destino.getEvento());
            t.setItem(item);
            t.setLocalOrigem(origem);
            t.setLocalDestino(destino);
            t.setResponsavel(responsavel);
            t.setNmReceptor(request.nmReceptor());
            t.setDsMotivo(request.dsMotivo());
            t.setTpStatus(STATUS_CONCLUIDA);
            t.setDtTransferencia(agora);
            t.setDtCadastro(agora);
            t.setUsuarioCadastro(responsavel);
            t.setFgAtivo(true);
            t.setFgExcluido(false);
            Transferencia salva = transferenciaRepository.save(t);

            // O item passa a estar no local de destino.
            item.setLocalAtual(destino);
            item.setUsuarioAlteracao(responsavel);
            item.setDtAlteracao(agora);
            itemRepository.save(item);

            resultado.add(toResponse(salva));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public ApiPage<TransferenciaResponse> listar(String idEvento, Integer page, Integer limit,
                                                 String q, String idLocalDestino, String tpStatus, String data) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long eventoId = idCodec.decodeEventoId(idEvento);
        Long localDestinoId = (idLocalDestino != null && !idLocalDestino.isBlank())
                ? idCodec.decodeLocalId(idLocalDestino) : null;
        LocalDate dataT = parseData(data);

        Specification<Transferencia> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("evento").get("id"), eventoId));
            if (localDestinoId != null) ps.add(cb.equal(root.get("localDestino").get("id"), localDestinoId));
            if (tpStatus != null && !tpStatus.isBlank()) ps.add(cb.equal(root.get("tpStatus"), tpStatus));
            if (dataT != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("dtTransferencia"), dataT.atStartOfDay()));
                ps.add(cb.lessThan(root.get("dtTransferencia"), dataT.plusDays(1).atStartOfDay()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("item").get("nmTitulo")), like),
                        cb.like(cb.lower(root.get("item").get("cdItem")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Transferencia> result = transferenciaRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "dtTransferencia")));
        var content = result.getContent().stream().map(this::toResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    @Transactional(readOnly = true)
    public TransferenciaResumoResponse resumo(String idEvento, String data) {
        Long ev = idCodec.decodeEventoId(idEvento);
        LocalDate dia = parseData(data);
        LocalDateTime inicio = dia != null ? dia.atStartOfDay() : null;
        LocalDateTime fim = dia != null ? dia.plusDays(1).atStartOfDay() : null;

        List<TransferenciaResumoResponse.DestinoQt> porDestino = transferenciaRepository
                .contagemPorDestino(ev, inicio, fim).stream()
                .map(r -> new TransferenciaResumoResponse.DestinoQt(
                        r[0] != null ? r[0].toString() : "Sem origem", ((Number) r[1]).longValue()))
                .toList();

        Specification<Transferencia> base = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            ps.add(cb.equal(root.get("evento").get("id"), ev));
            if (inicio != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("dtTransferencia"), inicio));
                ps.add(cb.lessThan(root.get("dtTransferencia"), fim));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        long total = transferenciaRepository.count(base);
        long concluidas = transferenciaRepository.count(base.and((root, query, cb) ->
                cb.equal(root.get("tpStatus"), STATUS_CONCLUIDA)));
        long locaisDestino = porDestino.size();
        return new TransferenciaResumoResponse(total, concluidas, total, locaisDestino, porDestino);
    }

    @Transactional(readOnly = true)
    public List<ItemDisponivelResponse> itensDisponiveis(String idEvento, String idLocalOrigem) {
        Long ev = idCodec.decodeEventoId(idEvento);
        List<Item> itens = (idLocalOrigem != null && !idLocalOrigem.isBlank())
                ? itemRepository.findByEvento_IdAndLocalAtual_IdAndFgExcluidoFalseAndFgEntregueFalseAndFgDescartadoFalseOrderByNmTituloAsc(
                        ev, idCodec.decodeLocalId(idLocalOrigem))
                : itemRepository.findByEvento_IdAndFgExcluidoFalseAndFgEntregueFalseAndFgDescartadoFalseAndLocalAtualIsNotNullOrderByNmTituloAsc(ev);
        return itens.stream().map(i -> new ItemDisponivelResponse(
                idCodec.encodeItemId(i.getId()), i.getCdItem(), i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getTpPrioridade(), i.getFgSensivel(),
                i.getLocalAtual() != null ? i.getLocalAtual().getNmLocal() : null)).toList();
    }

    // ------------------------------------------------------------------

    private Usuario usuarioLogadoOuNulo() {
        try {
            return usuarioContextService.requireUsuarioLogado();
        } catch (RuntimeException ex) {
            return null;
        }
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

    private TransferenciaResponse toResponse(Transferencia t) {
        Item i = t.getItem();
        return new TransferenciaResponse(
                idCodec.encodeTransferenciaId(t.getId()),
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getFgSensivel(),
                t.getLocalOrigem() != null ? t.getLocalOrigem().getNmLocal() : null,
                t.getLocalDestino() != null ? t.getLocalDestino().getNmLocal() : null,
                t.getResponsavel() != null ? t.getResponsavel().getNmUsuario() : null,
                t.getNmReceptor(),
                t.getDsMotivo(),
                t.getTpStatus(),
                t.getDtTransferencia());
    }
}
