package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.AuthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuthEventRepository extends JpaRepository<AuthEvent, Long> {

    @Query("""
            SELECT e FROM AuthEvent e
            LEFT JOIN e.usuario u
            WHERE e.fgExcluido = false
              AND (:tpEvento IS NULL OR :tpEvento = '' OR e.tpEvento = :tpEvento)
              AND (:tpResultado IS NULL OR :tpResultado = '' OR e.tpResultado = :tpResultado)
              AND (:idUsuario IS NULL OR u.id = :idUsuario)
              AND (:nrIp IS NULL OR :nrIp = '' OR e.nrIp = :nrIp)
              AND (:de IS NULL OR e.dtEvento >= :de)
              AND (:ate IS NULL OR e.dtEvento <= :ate)
            ORDER BY e.dtEvento DESC
            """)
    Page<AuthEvent> buscarFiltrado(@Param("tpEvento") String tpEvento,
                                   @Param("tpResultado") String tpResultado,
                                   @Param("idUsuario") Long idUsuario,
                                   @Param("nrIp") String nrIp,
                                   @Param("de") LocalDateTime de,
                                   @Param("ate") LocalDateTime ate,
                                   Pageable pageable);
}
