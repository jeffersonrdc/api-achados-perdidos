package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.MatchCandidatoResponse;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.ClaimValidacao;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ClaimValidacaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor de match claim PERDA ↔ itens da coleta/estoque.
 * Critérios: mesma categoria (filtro), subcategoria + marca + modelo + cor iguais;
 * se o claim tiver tags, exige ao menos uma em comum (senão, os demais bastam).
 * Persiste candidatos em {@code claim_validacao} com {@code ST_Resultado=PENDENTE}.
 */
@Service
public class MatchService {

    public static final String ST_PENDENTE = "PENDENTE";
    public static final String ST_CONFIRMADO = "CONFIRMADO";
    public static final String ST_DESCARTADO = "DESCARTADO";

    public static final String STATUS_AGUARDANDO_MATCH = "Aguardando Match";
    public static final String STATUS_MATCH = "Match";

    private static final List<String> STATUS_CANDIDATOS = List.of(
            "Em estoque", "Com pedido de devolucao", "Aguardando retirada");
    /** Status que o motor de match pode alterar automaticamente. */
    private static final Set<String> STATUS_GERENCIADOS = Set.of(
            "Claim Aberto", STATUS_AGUARDANDO_MATCH, STATUS_MATCH);
    private static final int SCORE_MINIMO = 55;
    private static final int MAX_CANDIDATOS = 10;

    private final ItemRepository itemRepository;
    private final ClaimValidacaoRepository claimValidacaoRepository;
    private final ClaimRepository claimRepository;
    private final StatusItemService statusItemService;
    private final SignedResourceIdCodec idCodec;

    public MatchService(ItemRepository itemRepository,
                        ClaimValidacaoRepository claimValidacaoRepository,
                        ClaimRepository claimRepository,
                        StatusItemService statusItemService,
                        SignedResourceIdCodec idCodec) {
        this.itemRepository = itemRepository;
        this.claimValidacaoRepository = claimValidacaoRepository;
        this.claimRepository = claimRepository;
        this.statusItemService = statusItemService;
        this.idCodec = idCodec;
    }

    /** Recalcula matches do claim (só faz sentido para PERDA fora de Rascunho). Pode gerar vários candidatos. */
    @Transactional
    public int recalcularMatches(Claim claim) {
        if (claim == null || claim.getId() == null) return 0;
        if (!ClaimService.TIPO_PERDA.equalsIgnoreCase(claim.getTpClaim())) return 0;
        String status = claim.getStatus() != null ? claim.getStatus().getNmStatus() : "";
        if ("Rascunho".equalsIgnoreCase(status)) return 0;
        if (claim.getCategoria() == null) return 0;

        invalidarPendentes(claim.getId());

        Long eventoId = claim.getEvento().getId();
        Long categoriaId = claim.getCategoria().getId();
        List<Item> candidatos = itemRepository
                .findByEvento_IdAndStatus_NmStatusInAndFgExcluidoFalseOrderByDtCadastroAsc(
                        eventoId, STATUS_CANDIDATOS)
                .stream()
                .filter(i -> !Boolean.TRUE.equals(i.getFgEntregue()))
                .filter(i -> !Boolean.TRUE.equals(i.getFgDescartado()))
                .filter(i -> i.getCategoria() != null && Objects.equals(i.getCategoria().getId(), categoriaId))
                .toList();

        record Scored(Item item, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (Item item : candidatos) {
            int score = calcularScore(claim, item);
            if (score >= SCORE_MINIMO) {
                scored.add(new Scored(item, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        if (scored.size() > MAX_CANDIDATOS) {
            scored = scored.subList(0, MAX_CANDIDATOS);
        }

        LocalDateTime agora = LocalDateTime.now();
        for (Scored s : scored) {
            ClaimValidacao v = new ClaimValidacao();
            v.setEvento(claim.getEvento());
            v.setClaim(claim);
            v.setItem(s.item());
            v.setQtSimilaridade(BigDecimal.valueOf(s.score()).setScale(2, RoundingMode.HALF_UP));
            v.setStResultado(ST_PENDENTE);
            v.setDtCadastro(agora);
            v.setFgExcluido(false);
            claimValidacaoRepository.save(v);
        }
        atualizarStatusMatch(claim, !scored.isEmpty());
        return scored.size();
    }

    /**
     * Quando um item da coleta entra/atualiza: recalcula todos os claims PERDA do mesmo evento/categoria
     * para incluir (ou remover) este item entre os candidatos.
     */
    @Transactional
    public int recalcularMatchesPorItem(Item item) {
        if (item == null || item.getId() == null) return 0;
        if (Boolean.TRUE.equals(item.getFgExcluido())
                || Boolean.TRUE.equals(item.getFgEntregue())
                || Boolean.TRUE.equals(item.getFgDescartado())) {
            return 0;
        }
        if (item.getEvento() == null || item.getCategoria() == null) return 0;
        String st = item.getStatus() != null ? item.getStatus().getNmStatus() : "";
        boolean candidato = STATUS_CANDIDATOS.stream().anyMatch(s -> s.equalsIgnoreCase(st));
        if (!candidato) return 0;

        List<Claim> claims = claimRepository.findPerdasParaMatch(
                item.getEvento().getId(), ClaimService.TIPO_PERDA, item.getCategoria().getId());
        int total = 0;
        for (Claim claim : claims) {
            total += recalcularMatches(claim);
        }
        return total;
    }

    /** Recalcula a partir do id assinado do claim. */
    @Transactional
    public Claim recalcularPorClaimId(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new br.com.achadosperdidos.exception.RecursoNaoEncontradoException("Claim não encontrado."));
        recalcularMatches(claim);
        return claimRepository.findById(claimId).orElse(claim);
    }

    private void atualizarStatusMatch(Claim claim, boolean temMatch) {
        String atual = claim.getStatus() != null ? claim.getStatus().getNmStatus() : "";
        if ("Rascunho".equalsIgnoreCase(atual)) return;
        boolean gerenciavel = atual.isBlank()
                || STATUS_GERENCIADOS.stream().anyMatch(s -> s.equalsIgnoreCase(atual));
        if (!gerenciavel) return;
        String novo = temMatch ? STATUS_MATCH : STATUS_AGUARDANDO_MATCH;
        if (novo.equalsIgnoreCase(atual)) return;
        claim.setStatus(statusItemService.findByNomeOrDefault(novo, novo));
        claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public List<MatchCandidatoResponse> listarPorClaim(String idClaimAssinado) {
        Long claimId = idCodec.decodeClaimId(idClaimAssinado);
        return claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_PENDENTE)
                .stream()
                .map(this::toCandidato)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasMatchPendente(Long claimId) {
        if (claimId == null) return false;
        return claimValidacaoRepository
                .countByClaim_IdAndStResultadoAndFgExcluidoFalse(claimId, ST_PENDENTE) > 0;
    }

    @Transactional(readOnly = true)
    public long contarPendentes(Long claimId) {
        if (claimId == null) return 0;
        return claimValidacaoRepository.countByClaim_IdAndStResultadoAndFgExcluidoFalse(claimId, ST_PENDENTE);
    }

    /** Ao iniciar devolução a partir de um match: confirma o item e descarta os demais pendentes. */
    @Transactional
    public void confirmarMatch(Long claimId, Long itemId) {
        if (claimId == null || itemId == null) return;
        LocalDateTime agora = LocalDateTime.now();
        List<ClaimValidacao> pendentes = claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_PENDENTE);
        for (ClaimValidacao v : pendentes) {
            if (v.getItem() != null && Objects.equals(v.getItem().getId(), itemId)) {
                v.setStResultado(ST_CONFIRMADO);
                v.setDtValidacao(agora);
            } else {
                v.setStResultado(ST_DESCARTADO);
                v.setDtValidacao(agora);
            }
            claimValidacaoRepository.save(v);
        }
    }

    private void invalidarPendentes(Long claimId) {
        List<ClaimValidacao> pendentes = claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_PENDENTE);
        for (ClaimValidacao v : pendentes) {
            v.setFgExcluido(true);
            claimValidacaoRepository.save(v);
        }
    }

    int calcularScore(Claim claim, Item item) {
        // Critérios obrigatórios: subcategoria, marca, modelo e cor.
        if (claim.getSubcategoria() == null || item.getSubcategoria() == null
                || !Objects.equals(claim.getSubcategoria().getId(), item.getSubcategoria().getId())) {
            return 0;
        }
        if (!equalsIgnore(claim.getNmMarca(), item.getNmMarca())) return 0;
        if (!equalsIgnore(claim.getNmModelo(), item.getNmModelo())) return 0;
        if (!equalsIgnore(claim.getNmCor(), item.getNmCor())) return 0;

        Set<String> claimTags = splitTags(claim.getDsTags());
        // Se o claim informou tags, exige ao menos uma em comum com o item.
        // Se não houver tags no claim, os demais critérios bastam.
        if (!claimTags.isEmpty() && !temAoMenosUmaTag(claimTags, item)) {
            return 0;
        }

        int score = 70; // subcategoria + marca + modelo + cor
        if (!claimTags.isEmpty()) {
            score += 15; // ao menos uma tag bateu
        }
        return Math.min(100, score);
    }

    /** True se alguma tag do claim aparece nas tags do item ou no texto (título/descrição/obs). */
    private static boolean temAoMenosUmaTag(Set<String> claimTags, Item item) {
        Set<String> itemTags = splitTags(item.getDsTags());
        String hay = ((item.getNmTitulo() == null ? "" : item.getNmTitulo()) + " "
                + (item.getDsItem() == null ? "" : item.getDsItem()) + " "
                + (item.getDsObservacoes() == null ? "" : item.getDsObservacoes()) + " "
                + (item.getDsTags() == null ? "" : item.getDsTags()))
                .toLowerCase(Locale.ROOT);
        for (String tag : claimTags) {
            if (itemTags.contains(tag) || hay.contains(tag)) return true;
        }
        return false;
    }

    private static Set<String> splitTags(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split("[,;|]"))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> s.length() >= 2)
                .collect(Collectors.toSet());
    }

    private static boolean equalsIgnore(String a, String b) {
        if (a == null || b == null) return false;
        String aa = a.trim();
        String bb = b.trim();
        if (aa.isEmpty() || bb.isEmpty()) return false;
        return aa.equalsIgnoreCase(bb);
    }

    private MatchCandidatoResponse toCandidato(ClaimValidacao v) {
        Item i = v.getItem();
        return new MatchCandidatoResponse(
                idCodec.encodeItemId(i.getId()),
                i.getCdItem(),
                i.getNmTitulo(),
                i.getCategoria() != null ? i.getCategoria().getNmCategoria() : null,
                i.getSubcategoria() != null ? i.getSubcategoria().getNmCategoria() : null,
                i.getNmMarca(),
                i.getNmModelo(),
                i.getNmCor(),
                i.getNmEstado(),
                i.getNmLocalEncontrado(),
                i.getDtEncontrado(),
                i.getHrEncontrado(),
                i.getTpPrioridade(),
                i.getStatus() != null ? i.getStatus().getNmStatus() : null,
                v.getQtSimilaridade());
    }
}
