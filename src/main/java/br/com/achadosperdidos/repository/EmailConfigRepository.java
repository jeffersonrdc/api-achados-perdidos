package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {
    List<EmailConfig> findByFgExcluidoFalseOrderByNmConfigAsc();
}
