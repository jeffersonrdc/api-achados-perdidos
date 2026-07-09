package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.CriancaResponsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriancaResponsavelRepository extends JpaRepository<CriancaResponsavel, Long> {
    List<CriancaResponsavel> findByCrianca_IdAndFgExcluidoFalseOrderByFgPrincipalDesc(Long criancaId);
}
