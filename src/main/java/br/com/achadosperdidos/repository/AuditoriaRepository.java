package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByFgExcluidoFalseOrderByDtAuditoriaDesc(Pageable pageable);
    Page<Auditoria> findByNmTabelaAndIdRegistroAndFgExcluidoFalseOrderByDtAuditoriaDesc(String nmTabela, Long idRegistro, Pageable pageable);
}
