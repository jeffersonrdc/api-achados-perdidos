package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "email_config")
public class EmailConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_EmailConfig") private Long id;
    @Column(name = "NM_Config", nullable = false, length = 120) private String nmConfig;
    @Column(name = "NM_Host", length = 150) private String nmHost;
    @Column(name = "NR_Porta") private Integer nrPorta;
    @Column(name = "NM_Usuario", length = 200) private String nmUsuario;
    @Column(name = "NM_Senha", length = 255) private String nmSenha;
    @Column(name = "NM_Remetente", length = 200) private String nmRemetente;
    @Column(name = "NM_RemetenteNome", length = 150) private String nmRemetenteNome;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Tls", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgTls = true;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmConfig() { return nmConfig; } public void setNmConfig(String v) { this.nmConfig = v; }
    public String getNmHost() { return nmHost; } public void setNmHost(String v) { this.nmHost = v; }
    public Integer getNrPorta() { return nrPorta; } public void setNrPorta(Integer v) { this.nrPorta = v; }
    public String getNmUsuario() { return nmUsuario; } public void setNmUsuario(String v) { this.nmUsuario = v; }
    public String getNmSenha() { return nmSenha; } public void setNmSenha(String v) { this.nmSenha = v; }
    public String getNmRemetente() { return nmRemetente; } public void setNmRemetente(String v) { this.nmRemetente = v; }
    public String getNmRemetenteNome() { return nmRemetenteNome; } public void setNmRemetenteNome(String v) { this.nmRemetenteNome = v; }
    public Boolean getFgTls() { return fgTls; } public void setFgTls(Boolean v) { this.fgTls = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
