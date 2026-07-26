package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long>, JpaSpecificationExecutor<Transferencia> {

    long countByEvento_IdAndFgExcluidoFalse(Long eventoId);
    long countByEvento_IdAndFgExcluidoFalseAndTpStatus(Long eventoId, String tpStatus);

    @Query("""
            SELECT COALESCE(l.nmLocal, 'Sem origem') AS nome, COUNT(t) AS qt
              FROM Transferencia t LEFT JOIN t.localDestino l
             WHERE t.evento.id = :eventoId AND t.fgExcluido = false
               AND (:inicio IS NULL OR (t.dtTransferencia >= :inicio AND t.dtTransferencia < :fim))
             GROUP BY l.nmLocal ORDER BY qt DESC""")
    List<Object[]> contagemPorDestino(@Param("eventoId") Long eventoId,
                                      @Param("inicio") java.time.LocalDateTime inicio,
                                      @Param("fim") java.time.LocalDateTime fim);
}
