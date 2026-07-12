package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatorios operacionais (secao 12 da especificacao). Reaproveita as views
 * ja existentes no banco. Recebe idEvento assinado; retorna linhas como mapas
 * coluna->valor (superficie somente-leitura para paineis/exportacao).
 */
@Service
public class RelatorioService {

    @PersistenceContext
    private EntityManager em;

    private final EventoRepository eventoRepository;
    private final SignedResourceIdCodec idCodec;

    public RelatorioService(EventoRepository eventoRepository, SignedResourceIdCodec idCodec) {
        this.eventoRepository = eventoRepository;
        this.idCodec = idCodec;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensPorCategoria(String idEvento) {
        return porEvento("SELECT * FROM VW_Itens_Categoria WHERE ID_Evento = :ev ORDER BY QT_Itens DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensPendentes(String idEvento) {
        return porEvento("SELECT * FROM VW_Itens_Pendentes WHERE ID_Evento = :ev ORDER BY QT_DiasArmazenado DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tempoDevolucao(String idEvento) {
        return porEvento("SELECT * FROM VW_Tempo_Devolucao WHERE ID_Evento = :ev", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> auditoria(String idEvento) {
        return porEvento("SELECT * FROM VW_Auditoria_Evento WHERE ID_Evento = :ev ORDER BY DT_Auditoria DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensDevolvidos(String idEvento) {
        return porNomeEvento("SELECT * FROM VW_Itens_Devolvidos WHERE NM_Evento = :ev ORDER BY DT_Devolucao DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> claimsAbertos(String idEvento) {
        return porNomeEvento("SELECT * FROM VW_Claims_Abertos WHERE NM_Evento = :ev ORDER BY QT_DiasAberto DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> slaEstourado(String idEvento) {
        return porNomeEvento("SELECT * FROM VW_Sla_Estourado WHERE NM_Evento = :ev ORDER BY QT_HorasEstouradas DESC", idEvento);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> itensPorLocalizacao() {
        return executar(em.createNativeQuery(
                "SELECT * FROM VW_Itens_Localizacao ORDER BY NM_Deposito, NM_Setor", Tuple.class).getResultList());
    }

    // ------------------------------------------------------------------

    private List<Map<String, Object>> porEvento(String sql, String idEvento) {
        Long ev = idCodec.decodeEventoId(idEvento);
        return executar(em.createNativeQuery(sql, Tuple.class).setParameter("ev", ev).getResultList());
    }

    private List<Map<String, Object>> porNomeEvento(String sql, String idEvento) {
        Evento evento = eventoRepository.findById(idCodec.decodeEventoId(idEvento))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        return executar(em.createNativeQuery(sql, Tuple.class).setParameter("ev", evento.getNmEvento()).getResultList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> executar(List<?> rows) {
        return ((List<Tuple>) rows).stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(Tuple t) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (TupleElement<?> e : t.getElements()) {
            m.put(e.getAlias(), t.get(e.getAlias()));
        }
        return m;
    }
}
