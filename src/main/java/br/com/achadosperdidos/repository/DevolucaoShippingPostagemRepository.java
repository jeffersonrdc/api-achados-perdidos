package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.DevolucaoShippingPostagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DevolucaoShippingPostagemRepository extends JpaRepository<DevolucaoShippingPostagem, Long> {
    Optional<DevolucaoShippingPostagem> findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtRegistroDesc(Long devolucaoId);
}
