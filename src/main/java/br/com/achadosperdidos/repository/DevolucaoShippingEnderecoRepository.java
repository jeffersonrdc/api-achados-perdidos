package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.DevolucaoShippingEndereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DevolucaoShippingEnderecoRepository extends JpaRepository<DevolucaoShippingEndereco, Long> {
    Optional<DevolucaoShippingEndereco> findByDevolucao_IdAndFgExcluidoFalse(Long devolucaoId);
}
