package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.controller.dto.PortalCategoriaCapaResponse;
import br.com.achadosperdidos.entity.Arquivo;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.PortalCategoriaCapa;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.PortalCategoriaCapaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PortalCategoriaCapaService {
    public static final String TP_ARQUIVO = "PORTAL_CAPA";

    private final PortalCategoriaCapaRepository repository;
    private final CategoriaService categoriaService;
    private final EventoRepository eventoRepository;
    private final ArquivoService arquivoService;
    private final SignedResourceIdCodec idCodec;

    public PortalCategoriaCapaService(PortalCategoriaCapaRepository repository,
                                      CategoriaService categoriaService,
                                      EventoRepository eventoRepository,
                                      ArquivoService arquivoService,
                                      SignedResourceIdCodec idCodec) {
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.eventoRepository = eventoRepository;
        this.arquivoService = arquivoService;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<PortalCategoriaCapaResponse> listar() {
        return repository.findByFgExcluidoFalseOrderByDtCadastroDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PortalCategoriaCapaResponse salvar(String idCategoria, String idEvento, MultipartFile file) {
        Categoria categoria = exigirCategoriaRaiz(idCodec.decodeCategoriaId(idCategoria));
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(idEvento))
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        PortalCategoriaCapa existente = repository.findByCategoria_Id(categoria.getId()).orElse(null);
        Arquivo anterior = existente != null && !Boolean.TRUE.equals(existente.getFgExcluido())
                ? existente.getArquivo()
                : null;

        ArquivoResponse uploaded = arquivoService.uploadComEvento(
                "CATEGORIA", categoria.getId(), TP_ARQUIVO, file, evento, true);

        Arquivo novo = arquivoService.findArquivoAtivo(uploaded.id());
        LocalDateTime agora = LocalDateTime.now();
        PortalCategoriaCapa capa = existente != null ? existente : new PortalCategoriaCapa();
        if (capa.getDtCadastro() == null) {
            capa.setDtCadastro(agora);
        }
        capa.setCategoria(categoria);
        capa.setEvento(evento);
        capa.setArquivo(novo);
        capa.setFgAtivo(true);
        capa.setFgExcluido(false);
        capa.setDtAlteracao(agora);
        repository.save(capa);

        if (anterior != null && !Objects.equals(anterior.getId(), novo.getId())) {
            arquivoService.excluir(idCodec.encodeArquivoId(anterior.getId()));
        }
        return toResponse(capa);
    }

    @Transactional
    public void remover(String idCategoria) {
        Long catId = idCodec.decodeCategoriaId(idCategoria);
        PortalCategoriaCapa capa = repository.findByCategoria_Id(catId)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não há imagem vinculada a esta categoria."));
        capa.setFgExcluido(true);
        capa.setFgAtivo(false);
        capa.setDtAlteracao(LocalDateTime.now());
        repository.save(capa);
        if (capa.getArquivo() != null) {
            arquivoService.excluir(idCodec.encodeArquivoId(capa.getArquivo().getId()));
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Arquivo> capasAtivasPorCategorias(Collection<Long> idsCategoria) {
        if (idsCategoria == null || idsCategoria.isEmpty()) return Map.of();
        List<Long> ids = idsCategoria.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Arquivo> out = new HashMap<>();
        for (PortalCategoriaCapa c : repository.findByCategoria_IdInAndFgExcluidoFalse(ids)) {
            if (c.getArquivo() != null && !Boolean.TRUE.equals(c.getArquivo().getFgExcluido())) {
                out.put(c.getCategoria().getId(), c.getArquivo());
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public boolean categoriaTemCapa(Long idCategoria) {
        return idCategoria != null && repository.existsByCategoria_IdAndFgExcluidoFalse(idCategoria);
    }

    @Transactional(readOnly = true)
    public boolean arquivoEhCapaAtiva(Long idArquivo) {
        return idArquivo != null && repository.existsByArquivo_IdAndFgExcluidoFalse(idArquivo);
    }

    public static Long idCategoriaRaiz(Item item) {
        if (item == null) return null;
        Categoria c = item.getCategoria();
        if (c == null) return null;
        while (c.getCategoriaPai() != null) {
            c = c.getCategoriaPai();
        }
        return c.getId();
    }

    private Categoria exigirCategoriaRaiz(Long id) {
        Categoria c = categoriaService.findEntity(id);
        if (c.getCategoriaPai() != null) {
            throw new IllegalArgumentException(
                    "Vincule a imagem à categoria principal, não à subcategoria.");
        }
        return c;
    }

    private PortalCategoriaCapaResponse toResponse(PortalCategoriaCapa c) {
        Arquivo a = c.getArquivo();
        return new PortalCategoriaCapaResponse(
                idCodec.encodeCategoriaId(c.getCategoria().getId()),
                c.getCategoria().getNmCategoria(),
                a != null ? idCodec.encodeArquivoId(a.getId()) : null,
                a != null ? a.getNmArquivo() : null);
    }
}
