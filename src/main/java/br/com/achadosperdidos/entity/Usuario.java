package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Usuario") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Perfil", nullable = false) private Perfil perfil;
    @Column(name = "NM_Usuario", nullable = false, length = 150) private String nmUsuario;
    @Column(name = "NM_Login", nullable = false, length = 80) private String nmLogin;
    @Column(name = "NM_Email", nullable = false, length = 150) private String nmEmail;
    @Column(name = "NM_Senha", nullable = false, length = 255) private String nmSenha;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Perfil getPerfil() { return perfil; } public void setPerfil(Perfil v) { this.perfil = v; }
    public String getNmUsuario() { return nmUsuario; } public void setNmUsuario(String v) { this.nmUsuario = v; }
    public String getNmLogin() { return nmLogin; } public void setNmLogin(String v) { this.nmLogin = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNmSenha() { return nmSenha; } public void setNmSenha(String v) { this.nmSenha = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
