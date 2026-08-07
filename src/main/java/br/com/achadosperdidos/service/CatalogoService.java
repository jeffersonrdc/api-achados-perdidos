package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.entity.Cor;
import br.com.achadosperdidos.entity.Marca;
import br.com.achadosperdidos.entity.Modelo;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.CorRepository;
import br.com.achadosperdidos.repository.MarcaRepository;
import br.com.achadosperdidos.repository.ModeloRepository;
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

/**
 * Catálogos globais (cor, marca, modelo):
 * - listagens por nome para selects da coleta;
 * - CRUD completo para a tela /caracteristicas.
 */
@Service
public class CatalogoService {
    private final CorRepository corRepository;
    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;
    private final SignedResourceIdCodec idCodec;
    private final AuditoriaContextService auditoriaContext;

    public CatalogoService(CorRepository corRepository, MarcaRepository marcaRepository,
                           ModeloRepository modeloRepository, SignedResourceIdCodec idCodec,
                           AuditoriaContextService auditoriaContext) {
        this.corRepository = corRepository;
        this.marcaRepository = marcaRepository;
        this.modeloRepository = modeloRepository;
        this.idCodec = idCodec;
        this.auditoriaContext = auditoriaContext;
    }

    // ---- Selects (somente nomes ativos) ----

    @Transactional(readOnly = true)
    public List<String> listarCores() {
        return corRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByNmCorAsc()
                .stream().map(Cor::getNmCor).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarMarcas() {
        return marcaRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByNmMarcaAsc()
                .stream().map(Marca::getNmMarca).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarModelos(String nmMarca) {
        if (nmMarca == null || nmMarca.isBlank()) return List.of();
        return modeloRepository
                .findByMarca_NmMarcaAndFgExcluidoFalseAndFgAtivoTrueOrderByNmModeloAsc(nmMarca.trim())
                .stream().map(Modelo::getNmModelo).toList();
    }

    // ---- Marcas (CRUD) ----

    @Transactional(readOnly = true)
    public ApiPage<MarcaResponse> listarMarcasAdmin(boolean incluirInativos, Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Specification<Marca> spec = catalogoSpec(incluirInativos, q, "nmMarca");
        Page<Marca> result = marcaRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "nmMarca")));
        var content = result.getContent().stream().map(this::toMarca).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public MarcaResponse criarMarca(MarcaCreateRequest request) {
        auditoriaContext.marcarContexto();
        String nome = request.nmMarca().trim();
        if (marcaRepository.existsByNmMarcaIgnoreCaseAndFgExcluidoFalse(nome)) {
            throw new IllegalArgumentException("Já existe uma marca com este nome.");
        }
        Marca m = new Marca();
        m.setNmMarca(nome);
        m.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        m.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        m.setFgExcluido(false);
        m.setDtCadastro(LocalDateTime.now());
        return toMarca(marcaRepository.save(m));
    }

    @Transactional
    public MarcaResponse atualizarMarca(String idToken, MarcaUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Marca m = findMarca(idCodec.decodeMarcaId(idToken));
        if (request.nmMarca() != null) {
            String nome = request.nmMarca().trim();
            if (marcaRepository.existsByNmMarcaIgnoreCaseAndIdNotAndFgExcluidoFalse(nome, m.getId())) {
                throw new IllegalArgumentException("Já existe uma marca com este nome.");
            }
            m.setNmMarca(nome);
        }
        if (request.orOrdem() != null) m.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) m.setFgAtivo(request.fgAtivo());
        m.setDtAlteracao(LocalDateTime.now());
        return toMarca(marcaRepository.save(m));
    }

    @Transactional
    public void excluirMarca(String idToken) {
        auditoriaContext.marcarContexto();
        Marca m = findMarca(idCodec.decodeMarcaId(idToken));
        m.setFgExcluido(true);
        m.setFgAtivo(false);
        m.setDtAlteracao(LocalDateTime.now());
        marcaRepository.save(m);
    }

    // ---- Modelos (CRUD) ----

    @Transactional(readOnly = true)
    public ApiPage<ModeloResponse> listarModelosAdmin(boolean incluirInativos, String idMarca,
                                                      Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Long marcaId = (idMarca != null && !idMarca.isBlank()) ? idCodec.decodeMarcaId(idMarca) : null;
        Specification<Modelo> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (!incluirInativos) ps.add(cb.isTrue(root.get("fgAtivo")));
            if (marcaId != null) ps.add(cb.equal(root.get("marca").get("id"), marcaId));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.like(cb.lower(root.get("nmModelo")), like));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Modelo> result = modeloRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "nmModelo")));
        var content = result.getContent().stream().map(this::toModelo).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public ModeloResponse criarModelo(ModeloCreateRequest request) {
        auditoriaContext.marcarContexto();
        Marca marca = findMarca(idCodec.decodeMarcaId(request.idMarca()));
        String nome = request.nmModelo().trim();
        if (modeloRepository.existsByNmModeloIgnoreCaseAndMarca_IdAndFgExcluidoFalse(nome, marca.getId())) {
            throw new IllegalArgumentException("Já existe um modelo com este nome nesta marca.");
        }
        Modelo m = new Modelo();
        m.setMarca(marca);
        m.setNmModelo(nome);
        m.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        m.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        m.setFgExcluido(false);
        m.setDtCadastro(LocalDateTime.now());
        return toModelo(modeloRepository.save(m));
    }

    @Transactional
    public ModeloResponse atualizarModelo(String idToken, ModeloUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Modelo m = findModelo(idCodec.decodeModeloId(idToken));
        if (request.idMarca() != null && !request.idMarca().isBlank()) {
            m.setMarca(findMarca(idCodec.decodeMarcaId(request.idMarca())));
        }
        if (request.nmModelo() != null) {
            String nome = request.nmModelo().trim();
            if (modeloRepository.existsByNmModeloIgnoreCaseAndMarca_IdAndIdNotAndFgExcluidoFalse(
                    nome, m.getMarca().getId(), m.getId())) {
                throw new IllegalArgumentException("Já existe um modelo com este nome nesta marca.");
            }
            m.setNmModelo(nome);
        }
        if (request.orOrdem() != null) m.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) m.setFgAtivo(request.fgAtivo());
        m.setDtAlteracao(LocalDateTime.now());
        return toModelo(modeloRepository.save(m));
    }

    @Transactional
    public void excluirModelo(String idToken) {
        auditoriaContext.marcarContexto();
        Modelo m = findModelo(idCodec.decodeModeloId(idToken));
        m.setFgExcluido(true);
        m.setFgAtivo(false);
        m.setDtAlteracao(LocalDateTime.now());
        modeloRepository.save(m);
    }

    // ---- Cores (CRUD) ----

    @Transactional(readOnly = true)
    public ApiPage<CorResponse> listarCoresAdmin(boolean incluirInativos, Integer page, Integer limit, String q) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Specification<Cor> spec = catalogoSpec(incluirInativos, q, "nmCor");
        Page<Cor> result = corRepository.findAll(spec,
                PageRequest.of(p - 1, l, Sort.by(Sort.Direction.ASC, "nmCor")));
        var content = result.getContent().stream().map(this::toCor).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public CorResponse criarCor(CorCreateRequest request) {
        auditoriaContext.marcarContexto();
        String nome = request.nmCor().trim();
        if (corRepository.existsByNmCorIgnoreCaseAndFgExcluidoFalse(nome)) {
            throw new IllegalArgumentException("Já existe uma cor com este nome.");
        }
        Cor c = new Cor();
        c.setNmCor(nome);
        c.setCdHex(blankToNull(request.cdHex()));
        c.setOrOrdem(request.orOrdem() != null ? request.orOrdem() : 0);
        c.setFgAtivo(request.fgAtivo() == null || request.fgAtivo());
        c.setFgExcluido(false);
        c.setDtCadastro(LocalDateTime.now());
        return toCor(corRepository.save(c));
    }

    @Transactional
    public CorResponse atualizarCor(String idToken, CorUpdateRequest request) {
        auditoriaContext.marcarContexto();
        Cor c = findCor(idCodec.decodeCorId(idToken));
        if (request.nmCor() != null) {
            String nome = request.nmCor().trim();
            if (corRepository.existsByNmCorIgnoreCaseAndIdNotAndFgExcluidoFalse(nome, c.getId())) {
                throw new IllegalArgumentException("Já existe uma cor com este nome.");
            }
            c.setNmCor(nome);
        }
        if (request.cdHex() != null) c.setCdHex(blankToNull(request.cdHex()));
        if (request.orOrdem() != null) c.setOrOrdem(request.orOrdem());
        if (request.fgAtivo() != null) c.setFgAtivo(request.fgAtivo());
        c.setDtAlteracao(LocalDateTime.now());
        return toCor(corRepository.save(c));
    }

    @Transactional
    public void excluirCor(String idToken) {
        auditoriaContext.marcarContexto();
        Cor c = findCor(idCodec.decodeCorId(idToken));
        c.setFgExcluido(true);
        c.setFgAtivo(false);
        c.setDtAlteracao(LocalDateTime.now());
        corRepository.save(c);
    }

    private <T> Specification<T> catalogoSpec(boolean incluirInativos, String q, String nomeField) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("fgExcluido")));
            if (!incluirInativos) ps.add(cb.isTrue(root.get("fgAtivo")));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.like(cb.lower(root.get(nomeField)), like));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private Marca findMarca(Long id) {
        return marcaRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Marca não encontrada."));
    }

    private Modelo findModelo(Long id) {
        return modeloRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo não encontrado."));
    }

    private Cor findCor(Long id) {
        return corRepository.findById(id)
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cor não encontrada."));
    }

    private MarcaResponse toMarca(Marca m) {
        return new MarcaResponse(idCodec.encodeMarcaId(m.getId()), m.getNmMarca(), m.getOrOrdem(), m.getFgAtivo());
    }

    private ModeloResponse toModelo(Modelo m) {
        return new ModeloResponse(
                idCodec.encodeModeloId(m.getId()),
                m.getNmModelo(),
                m.getOrOrdem(),
                m.getFgAtivo(),
                idCodec.encodeMarcaId(m.getMarca().getId()),
                m.getMarca().getNmMarca());
    }

    private CorResponse toCor(Cor c) {
        return new CorResponse(idCodec.encodeCorId(c.getId()), c.getNmCor(), c.getCdHex(), c.getOrOrdem(), c.getFgAtivo());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
