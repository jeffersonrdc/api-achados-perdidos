package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "evento")
public class Evento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Evento") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Empresa", nullable = false) private Empresa empresa;
    @Column(name = "NM_Evento", nullable = false, length = 200) private String nmEvento;
    @Column(name = "DS_Evento", columnDefinition = "TEXT") private String dsEvento;
    @Column(name = "DT_Inicio", nullable = false) private LocalDateTime dtInicio;
    @Column(name = "DT_Fim", nullable = false) private LocalDateTime dtFim;
    @Column(name = "NM_Local", length = 200) private String nmLocal;
    @Column(name = "NM_Cidade", length = 100) private String nmCidade;
    @Column(name = "SG_UF", length = 2) private String sgUf;
    @Column(name = "QT_DiasRetencao", nullable = false) private Integer qtDiasRetencao = 90;
    @Column(name = "NM_UrlLogo", columnDefinition = "MEDIUMTEXT") private String nmUrlLogo;
    @Column(name = "NM_UrlHero", columnDefinition = "MEDIUMTEXT") private String nmUrlHero;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; } public void setEmpresa(Empresa v) { this.empresa = v; }
    public String getNmEvento() { return nmEvento; } public void setNmEvento(String v) { this.nmEvento = v; }
    public String getDsEvento() { return dsEvento; } public void setDsEvento(String v) { this.dsEvento = v; }
    public LocalDateTime getDtInicio() { return dtInicio; } public void setDtInicio(LocalDateTime v) { this.dtInicio = v; }
    public LocalDateTime getDtFim() { return dtFim; } public void setDtFim(LocalDateTime v) { this.dtFim = v; }
    public String getNmLocal() { return nmLocal; } public void setNmLocal(String v) { this.nmLocal = v; }
    public String getNmCidade() { return nmCidade; } public void setNmCidade(String v) { this.nmCidade = v; }
    public String getSgUf() { return sgUf; } public void setSgUf(String v) { this.sgUf = v; }
    public Integer getQtDiasRetencao() { return qtDiasRetencao; } public void setQtDiasRetencao(Integer v) { this.qtDiasRetencao = v; }
    public String getNmUrlLogo() { return nmUrlLogo; } public void setNmUrlLogo(String v) { this.nmUrlLogo = v; }
    public String getNmUrlHero() { return nmUrlHero; } public void setNmUrlHero(String v) { this.nmUrlHero = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
