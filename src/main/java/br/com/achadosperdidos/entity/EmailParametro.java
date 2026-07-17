package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "email_parametro")
public class EmailParametro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_EmailParametro") private Long id;
    @Column(name = "TP_Evento", nullable = false, length = 40) private String tpEvento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_EmailConfig") private EmailConfig emailConfig;
    @Column(name = "NM_Template", nullable = false, length = 120) private String nmTemplate;
    @Column(name = "NM_Assunto", length = 200) private String nmAssunto;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTpEvento() { return tpEvento; } public void setTpEvento(String v) { this.tpEvento = v; }
    public EmailConfig getEmailConfig() { return emailConfig; } public void setEmailConfig(EmailConfig v) { this.emailConfig = v; }
    public String getNmTemplate() { return nmTemplate; } public void setNmTemplate(String v) { this.nmTemplate = v; }
    public String getNmAssunto() { return nmAssunto; } public void setNmAssunto(String v) { this.nmAssunto = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
}
