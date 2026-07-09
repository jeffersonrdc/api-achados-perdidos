package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "status_item")
public class StatusItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Status") private Long id;
    @Column(name = "NM_Status", nullable = false, length = 80) private String nmStatus;
    @Column(name = "DS_Status", length = 500) private String dsStatus;
    @Column(name = "OR_Ordem", nullable = false) private Integer orOrdem = 0;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Final", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgFinal = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmStatus() { return nmStatus; } public void setNmStatus(String v) { this.nmStatus = v; }
    public String getDsStatus() { return dsStatus; } public void setDsStatus(String v) { this.dsStatus = v; }
    public Integer getOrOrdem() { return orOrdem; } public void setOrOrdem(Integer v) { this.orOrdem = v; }
    public Boolean getFgFinal() { return fgFinal; } public void setFgFinal(Boolean v) { this.fgFinal = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
