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
 * Critérios: mesma categoria (filtro); subcategoria/marca/modelo/cor só quando o claim informou;
 * se o claim tiver tags, exige ao menos uma em comum.
 * Persiste candidatos em {@code claim_validacao} com {@code ST_Resultado=PENDENTE}.
 */
@Service
public class MatchService {

    public static final String ST_PENDENTE = "PENDENTE";
    public static final String ST_CONFIRMADO = "CONFIRMADO";
    public static final String ST_DESCARTADO = "DESCARTADO";
    /**
     * Par claim↔item recusado pelo operador na análise do pedido. É definitivo:
     * o registro permanece visível no match e nunca volta a aceitar novo pedido.
     */
    public static final String ST_REPROVADO = "REPROVADO";

    public static final String STATUS_AGUARDANDO_MATCH = "Aguardando Match";
    public static final String STATUS_MATCH = "Match";

    /**
     * Status do item que ficam fora do match: os que ainda não passaram pela
     * triagem (não estão disponíveis no estoque) e os finais/indisponíveis.
     */
    private static final Set<String> STATUS_EXCLUIDOS = Set.of(
            "Aguardando triagem", "Em triagem", "Em Análise",
            "Aguardando retirada", "Devolvido", "Descartado", "Finalizado");

    /**
     * Resultados que mantêm o candidato visível no painel de match — e que,
     * portanto, contam para {@code hasMatch} na listagem de claims.
     */
    public static final List<String> RESULTADOS_VISIVEIS =
            List.of(ST_PENDENTE, ST_CONFIRMADO, ST_REPROVADO);
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

        // Pares já reprovados não voltam à fila — o registro original permanece.
        Set<Long> reprovados = itensReprovados(claim.getId());

        Long eventoId = claim.getEvento().getId();
        Long categoriaId = claim.getCategoria().getId();
        List<Item> candidatos = itemRepository
                .findByEvento_IdAndFgExcluidoFalseOrderByDtCadastroAsc(eventoId)
                .stream()
                .filter(i -> !Boolean.TRUE.equals(i.getFgDescartado()))
                .filter(i -> !statusExcluido(i))
                .filter(i -> i.getCategoria() != null && Objects.equals(i.getCategoria().getId(), categoriaId))
                .filter(i -> !reprovados.contains(i.getId()))
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
                || Boolean.TRUE.equals(item.getFgDescartado())) {
            return 0;
        }
        if (item.getEvento() == null || item.getCategoria() == null) return 0;
        if (statusExcluido(item)) return 0;

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

    /**
     * Candidatos do claim: o que está em pedido aberto (CONFIRMADO), os pendentes
     * e os reprovados — estes últimos mantidos à vista, mas bloqueados. Reprovados
     * vão para o fim da lista.
     */
    @Transactional(readOnly = true)
    public List<MatchCandidatoResponse> listarPorClaim(String idClaimAssinado) {
        Long claimId = idCodec.decodeClaimId(idClaimAssinado);
        return claimValidacaoRepository
                .findByClaim_IdAndStResultadoInAndFgExcluidoFalseOrderByQtSimilaridadeDesc(
                        claimId, RESULTADOS_VISIVEIS)
                .stream()
                // Item indisponível só é escondido enquanto é apenas um candidato:
                // decisões já tomadas (em pedido / reprovado) permanecem à vista.
                .filter(v -> v.getItem() != null
                        && (!ST_PENDENTE.equalsIgnoreCase(v.getStResultado()) || !statusExcluido(v.getItem())))
                .sorted(Comparator.comparing((ClaimValidacao v) -> ST_REPROVADO.equalsIgnoreCase(v.getStResultado())))
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

    /**
     * Ao abrir o pedido a partir de um match: marca o item escolhido como
     * {@link #ST_CONFIRMADO}. Os demais seguem pendentes de propósito — se este
     * pedido for reprovado, o operador precisa deles para tentar outro item.
     */
    @Transactional
    public void confirmarMatch(Long claimId, Long itemId) {
        if (claimId == null || itemId == null) return;
        LocalDateTime agora = LocalDateTime.now();
        for (ClaimValidacao v : claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_PENDENTE)) {
            if (v.getItem() != null && Objects.equals(v.getItem().getId(), itemId)) {
                v.setStResultado(ST_CONFIRMADO);
                v.setDtValidacao(agora);
                claimValidacaoRepository.save(v);
            }
        }
    }

    /** Após a aprovação o claim está resolvido: os candidatos restantes saem da fila. */
    @Transactional
    public void descartarPendentes(Long claimId, Long itemIdMantido) {
        if (claimId == null) return;
        LocalDateTime agora = LocalDateTime.now();
        for (ClaimValidacao v : claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_PENDENTE)) {
            if (v.getItem() != null && Objects.equals(v.getItem().getId(), itemIdMantido)) continue;
            v.setStResultado(ST_DESCARTADO);
            v.setDtValidacao(agora);
            claimValidacaoRepository.save(v);
        }
    }

    /**
     * Reprova o par claim↔item: o registro fica marcado como {@link #ST_REPROVADO}
     * (permanente e visível no match) e os demais candidatos daquele claim voltam a
     * ficar disponíveis, para o operador tentar outro item.
     *
     * @return true se havia candidato pendente após a reprovação.
     */
    @Transactional
    public boolean reprovarMatch(Long claimId, Long itemId) {
        if (claimId == null || itemId == null) return false;
        LocalDateTime agora = LocalDateTime.now();
        boolean sobrouPendente = false;

        for (ClaimValidacao v : claimValidacaoRepository.findByClaim_IdAndFgExcluidoFalseOrderByDtCadastroDesc(claimId)) {
            String st = v.getStResultado() != null ? v.getStResultado() : "";
            if (ST_REPROVADO.equalsIgnoreCase(st)) {
                continue;
            }
            boolean mesmoItem = v.getItem() != null && Objects.equals(v.getItem().getId(), itemId);
            if (mesmoItem) {
                v.setStResultado(ST_REPROVADO);
                v.setDtValidacao(agora);
                claimValidacaoRepository.save(v);
            } else if (ST_DESCARTADO.equalsIgnoreCase(st) || ST_CONFIRMADO.equalsIgnoreCase(st)) {
                // Descartados na abertura do pedido reprovado voltam à fila.
                v.setStResultado(ST_PENDENTE);
                v.setDtValidacao(null);
                claimValidacaoRepository.save(v);
                sobrouPendente = true;
            } else if (ST_PENDENTE.equalsIgnoreCase(st)) {
                sobrouPendente = true;
            }
        }
        return sobrouPendente;
    }

    /** True quando este par claim↔item já foi reprovado (bloqueia novo pedido). */
    @Transactional(readOnly = true)
    public boolean isReprovado(Long claimId, Long itemId) {
        if (claimId == null || itemId == null) return false;
        return claimValidacaoRepository
                .existsByClaim_IdAndItem_IdAndStResultadoAndFgExcluidoFalse(claimId, itemId, ST_REPROVADO);
    }

    /** Devolve o claim ao pool de match após uma reprovação (usado só em claims PERDA). */
    @Transactional
    public void devolverAoPoolDeMatch(Claim claim, boolean temPendente) {
        if (claim == null) return;
        claim.setStatus(statusItemService.findByNomeOrDefault(
                temPendente ? STATUS_MATCH : STATUS_AGUARDANDO_MATCH,
                temPendente ? STATUS_MATCH : STATUS_AGUARDANDO_MATCH));
        claimRepository.save(claim);
    }

    private Set<Long> itensReprovados(Long claimId) {
        return claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_REPROVADO)
                .stream()
                .map(v -> v.getItem() != null ? v.getItem().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void invalidarPendentes(Long claimId) {
        List<ClaimValidacao> pendentes = claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claimId, ST_PENDENTE);
        for (ClaimValidacao v : pendentes) {
            v.setFgExcluido(true);
            claimValidacaoRepository.save(v);
        }
    }

    private static boolean statusExcluido(Item item) {
        String st = item.getStatus() != null ? item.getStatus().getNmStatus() : "";
        if (st == null || st.isBlank()) return false;
        return STATUS_EXCLUIDOS.stream().anyMatch(s -> s.equalsIgnoreCase(st));
    }

    int calcularScore(Claim claim, Item item) {
        int camposInformados = 0;

        if (claim.getSubcategoria() != null) {
            camposInformados++;
            if (item.getSubcategoria() == null
                    || !Objects.equals(claim.getSubcategoria().getId(), item.getSubcategoria().getId())) {
                return 0;
            }
        }
        if (informado(claim.getNmMarca())) {
            camposInformados++;
            if (!equalsIgnore(claim.getNmMarca(), item.getNmMarca())) return 0;
        }
        if (informado(claim.getNmModelo())) {
            camposInformados++;
            if (!equalsIgnore(claim.getNmModelo(), item.getNmModelo())) return 0;
        }
        if (informado(claim.getNmCor())) {
            camposInformados++;
            if (!equalsIgnore(claim.getNmCor(), item.getNmCor())) return 0;
        }

        Set<String> claimTags = splitTags(claim.getDsTags());
        if (!claimTags.isEmpty() && !temAoMenosUmaTag(claimTags, item)) {
            return 0;
        }

        // Categoria já filtrada. Score base sobe se houver poucos campos informados.
        int score = camposInformados == 0
                ? 60
                : Math.max(SCORE_MINIMO, 40 + camposInformados * 10);
        if (!claimTags.isEmpty()) {
            score += 15;
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

    private static boolean informado(String v) {
        return v != null && !v.trim().isEmpty();
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
        boolean reprovado = ST_REPROVADO.equalsIgnoreCase(v.getStResultado());
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
                v.getQtSimilaridade(),
                v.getStResultado(),
                reprovado);
    }
}
