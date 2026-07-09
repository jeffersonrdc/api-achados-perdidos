package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContatoRepository extends JpaRepository<Contato, Long> {
    List<Contato> findByFgExcluidoFalseOrderByDtContatoDesc();
    List<Contato> findByClaim_IdAndFgExcluidoFalseOrderByDtContatoDesc(Long claimId);
    List<Contato> findByItem_IdAndFgExcluidoFalseOrderByDtContatoDesc(Long itemId);
}
