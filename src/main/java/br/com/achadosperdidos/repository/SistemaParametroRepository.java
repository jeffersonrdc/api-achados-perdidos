package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.SistemaParametro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SistemaParametroRepository extends JpaRepository<SistemaParametro, String> {
}
