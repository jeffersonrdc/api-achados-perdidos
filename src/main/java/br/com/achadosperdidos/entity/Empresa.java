package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "empresa")
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Empresa") private Long id;
    @Column(name = "NM_RazaoSocial", nullable = false, length = 200) private String nmRazaoSocial;
    @Column(name = "NM_Fantasia", length = 200) private String nmFantasia;
    @Column(name = "NR_CNPJ", nullable = false, length = 14) private String nrCnpj;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmRazaoSocial() { return nmRazaoSocial; } public void setNmRazaoSocial(String v) { this.nmRazaoSocial = v; }
    public String getNmFantasia() { return nmFantasia; } public void setNmFantasia(String v) { this.nmFantasia = v; }
    public String getNrCnpj() { return nrCnpj; } public void setNrCnpj(String v) { this.nrCnpj = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNrTelefone() { return nrTelefone; } public void setNrTelefone(String v) { this.nrTelefone = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
