package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DepositoCreateRequest;
import br.com.achadosperdidos.controller.dto.DepositoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoCreateRequest;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoResponse;
import br.com.achadosperdidos.controller.dto.EstoqueEnderecoUpdateRequest;
import br.com.achadosperdidos.entity.Deposito;
import br.com.achadosperdidos.entity.EstoqueEndereco;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.DepositoRepository;
import br.com.achadosperdidos.repository.EstoqueEnderecoRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DepositoService {
    private static final Set<String> NIVEIS = Set.of("SETOR", "ESTANTE", "PRATELEIRA", "CAIXA", "POSICAO");
    private static final Map<String, String> PAI_ESPERADO = Map.of(
            "ESTANTE", "SETOR",
            "PRATELEIRA", "ESTANTE",
            "CAIXA", "PRATELEIRA",
            "POSICAO", "CAIXA");

    private final DepositoRepository depositoRepository;
    private final EventoRepository eventoRepository;
    private final EstoqueEnderecoRepository estoqueEnderecoRepository;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public DepositoService(DepositoRepository depositoRepository, EventoRepository eventoRepository,
                           EstoqueEnderecoRepository estoqueEnderecoRepository, SignedResourceIdCodec idCodec,
                           AuditoriaContextService auditoriaContext) {
        this.depositoRepository = depositoRepository;
        this.eventoRepository = eventoRepository;
        this.estoqueEnderecoRepository = estoqueEnderecoRepository;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    /** Opções de endereçamento (somente nomes ativos) para os selects do estoque. */
    @Transactional(readOnly = true)
    public List<String> listarEnderecos(String idDeposito, String nivel) {
        if (nivel == null || nivel.isBlank()) return List.of();
        Long depId = idCodec.decode(SignedResourceIdCodec.Kind.DEP, idDeposito);
        String tp = normalizarNivel(nivel);
        return estoqueEnderecoRepository
                .findByDeposito_IdAndTpNivelAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEnderecoAsc(depId, tp)
                .stream().map(EstoqueEndereco::getNmEndereco).toList();
    }

    /** Listagem admin (objetos) para a tela /logistica-fisica. */
    @Transactional(readOnly = true)
    public List<EstoqueEnderecoResponse> listarEnderecosAdmin(String idDeposito, String nivel,
                                                             boolean incluirInativos, String idPai) {
        Long depId = idCodec.decode(SignedResourceIdCodec.Kind.DEP, idDeposito);
        String tp = normalizarNivel(nivel);
        List<EstoqueEndereco> lista;
        if (idPai != null && !idPai.isBlank()) {
            Long paiId = idCodec.decodeEnderecoId(idPai);
            lista = incluirInativos
                    ? estoqueEnderecoRepository.findByDeposito_IdAndTpNivelAndEnderecoPai_IdAndFgExcluidoFalseOrderByOrOrdemAscNmEnderecoAsc(depId, tp, paiId)
                    : estoqueEnderecoRepository.findByDeposito_IdAndTpNivelAndEnderecoPai_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEnderecoAsc(depId, tp, paiId);
        } else {
            lista = incluirInativos
                    ? estoqueEnderecoRepository.findByDeposito_IdAndTpNivelAndFgExcluidoFalseOrderByOrOrdemAscNmEnderecoAsc(depId, tp)
                    : estoqueEnderecoRepository.findByDeposito_IdAndTpNivelAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEnderecoAsc(depId, tp);
        }
        return lista.stream().map(this::toEnderecoResponse).toList();
    }

    @Transactional
    public EstoqueEnderecoResponse criarEndereco(String idDeposito, EstoqueEnderecoCreateRequest request) {
        auditoriaContext.marcarContexto();
        Deposito dep = findEntity(idCodec.decode(SignedResourceIdCodec.Kind.DEP, idDeposito));
        String tp = normalizarNivel(request.tpNivel());
        String nome = request.nmEndereco().trim();
        if (estoqueEnderecoRepository.existsByDeposito_IdAndTpNivelAndNmEnderecoIgnoreCaseAndFgExcluidoFalse(
                dep.getId(), tp, nome)) {
            throw new IllegalArgumentException("Já existe um endereço com este nome neste nível.");
        }
        EstoqueEndereco e = new EstoqueEndereco();
        e.setDeposito(dep);
        e.setTpNivel(tp);
        e.setNmEndereco(nome);
        e.setEnderecoPai(resolvePai(dep.getId(), tp, request.idEnderecoPai()));
        e.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        e.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        e.setFgExcluido(false);
        e.setDtCadastro(LocalDateTime.now());
        return toEnderecoResponse(estoqueEnderecoRepository.save(e));
    }

    @Transactional
    public EstoqueEnderecoResponse atualizarEndereco(String idDeposito, String idEndereco,
                                                     EstoqueEnderecoUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Long depId = idCodec.decode(SignedResourceIdCodec.Kind.DEP, idDeposito);
        EstoqueEndereco e = findEndereco(idCodec.decodeEnderecoId(idEndereco));
        if (!e.getDeposito().getId().equals(depId)) {
            throw new IllegalArgumentException("Endereço não pertence a este depósito.");
        }
        if (request.nmEndereco() != null) {
            String nome = request.nmEndereco().trim();
            if (estoqueEnderecoRepository.existsByDeposito_IdAndTpNivelAndNmEnderecoIgnoreCaseAndIdNotAndFgExcluidoFalse(
                    depId, e.getTpNivel(), nome, e.getId())) {
                throw new IllegalArgumentException("Já existe um endereço com este nome neste nível.");
            }
            e.setNmEndereco(nome);
        }
        if (request.idEnderecoPai() != null) {
            if (request.idEnderecoPai().isBlank()) {
                if (!"SETOR".equals(e.getTpNivel())) {
                    throw new IllegalArgumentException("Informe o endereço pai.");
                }
                e.setEnderecoPai(null);
            } else {
                e.setEnderecoPai(resolvePai(depId, e.getTpNivel(), request.idEnderecoPai()));
            }
        }
        if (request.orOrdem() != null) e.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) e.setFgAtivo(request.fgAtivo());
        e.setDtAlteracao(LocalDateTime.now());
        return toEnderecoResponse(estoqueEnderecoRepository.save(e));
    }

    @Transactional
    public void excluirEndereco(String idDeposito, String idEndereco) {
        auditoriaContext.marcarContexto();
        Long depId = idCodec.decode(SignedResourceIdCodec.Kind.DEP, idDeposito);
        EstoqueEndereco e = findEndereco(idCodec.decodeEnderecoId(idEndereco));
        if (!e.getDeposito().getId().equals(depId)) {
            throw new IllegalArgumentException("Endereço não pertence a este depósito.");
        }
        e.setFgExcluido(true);
        e.setFgAtivo(false);
        e.setDtAlteracao(LocalDateTime.now());
        estoqueEnderecoRepository.save(e);
    }

    @Transactional
    public DepositoResponse create(DepositoCreateRequest request) {
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(request.idEvento()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Deposito d = new Deposito();
        d.setEvento(evento);
        d.setNmDeposito(request.nmDeposito().trim());
        d.setDsDeposito(request.dsDeposito());
        d.setFgPrincipal(Boolean.TRUE.equals(request.fgPrincipal()));
        d.setDtCadastro(LocalDateTime.now());
        d.setFgAtivo(true);
        d.setFgExcluido(false);
        return toResponse(depositoRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DepositoResponse> findByEvento(String idEvento) {
        return depositoRepository.findByEvento_IdAndFgExcluidoFalseOrderByNmDepositoAsc(idCodec.decodeEventoId(idEvento))
                .stream().map(this::toResponse).toList();
    }

    Deposito findEntity(Long id) {
        return depositoRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Depósito não encontrado."));
    }

    private EstoqueEndereco findEndereco(Long id) {
        return estoqueEnderecoRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
    }

    private EstoqueEndereco resolvePai(Long depositoId, String nivelFilho, String idPaiToken) {
        String nivelPai = PAI_ESPERADO.get(nivelFilho);
        if (nivelPai == null) {
            if (idPaiToken != null && !idPaiToken.isBlank()) {
                throw new IllegalArgumentException("Setor não possui endereço pai.");
            }
            return null;
        }
        if (idPaiToken == null || idPaiToken.isBlank()) {
            // Hierarquia opcional para dados legados — permite cadastrar sem pai.
            return null;
        }
        EstoqueEndereco pai = findEndereco(idCodec.decodeEnderecoId(idPaiToken));
        if (!pai.getDeposito().getId().equals(depositoId)) {
            throw new IllegalArgumentException("O endereço pai deve pertencer ao mesmo depósito.");
        }
        if (!nivelPai.equals(pai.getTpNivel())) {
            throw new IllegalArgumentException("O pai deve ser do nível " + nivelPai + ".");
        }
        return pai;
    }

    private String normalizarNivel(String nivel) {
        String tp = nivel == null ? "" : nivel.trim().toUpperCase();
        if (!NIVEIS.contains(tp)) {
            throw new IllegalArgumentException("Nível inválido. Use SETOR, ESTANTE, PRATELEIRA, CAIXA ou POSICAO.");
        }
        return tp;
    }

    private EstoqueEnderecoResponse toEnderecoResponse(EstoqueEndereco e) {
        EstoqueEndereco pai = e.getEnderecoPai();
        return new EstoqueEnderecoResponse(
                idCodec.encodeEnderecoId(e.getId()),
                idCodec.encode(SignedResourceIdCodec.Kind.DEP, e.getDeposito().getId()),
                e.getTpNivel(),
                e.getNmEndereco(),
                e.getOrOrdem(),
                e.getFgAtivo(),
                pai != null ? idCodec.encodeEnderecoId(pai.getId()) : null,
                pai != null ? pai.getNmEndereco() : null);
    }

    private DepositoResponse toResponse(Deposito d) {
        return new DepositoResponse(
                idCodec.encode(SignedResourceIdCodec.Kind.DEP, d.getId()),
                idCodec.encodeEventoId(d.getEvento().getId()),
                d.getNmDeposito(),
                d.getDsDeposito(),
                d.getFgPrincipal());
    }
}
