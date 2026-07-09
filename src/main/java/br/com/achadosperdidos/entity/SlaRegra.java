package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "sla_regra")
public class SlaRegra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_SlaRegra") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento") private Evento evento;
    @Column(name = "TP_Processo", nullable = false, length = 50) private String tpProcesso;
    @Column(name = "QT_HorasLimite", nullable = false) private Integer qtHorasLimite;
    @Column(name = "QT_HorasAlerta") private Integer qtHorasAlerta;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_EnviarAlerta", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgEnviarAlerta = true;
    @Column(name = "DS_Observacao", length = 500) private String dsObservacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public String getTpProcesso() { return tpProcesso; } public void setTpProcesso(String v) { this.tpProcesso = v; }
    public Integer getQtHorasLimite() { return qtHorasLimite; } public void setQtHorasLimite(Integer v) { this.qtHorasLimite = v; }
    public Integer getQtHorasAlerta() { return qtHorasAlerta; } public void setQtHorasAlerta(Integer v) { this.qtHorasAlerta = v; }
    public Boolean getFgEnviarAlerta() { return fgEnviarAlerta; } public void setFgEnviarAlerta(Boolean v) { this.fgEnviarAlerta = v; }
    public String getDsObservacao() { return dsObservacao; } public void setDsObservacao(String v) { this.dsObservacao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
