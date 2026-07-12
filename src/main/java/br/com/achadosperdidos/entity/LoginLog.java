package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_log")
public class LoginLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Login") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Usuario", nullable = false) private Usuario usuario;
    @Column(name = "DT_Login", nullable = false) private LocalDateTime dtLogin;
    @Column(name = "DT_Logout") private LocalDateTime dtLogout;
    @Column(name = "NR_IP", length = 45) private String nrIp;
    @Column(name = "NM_Dispositivo", length = 150) private String nmDispositivo;
    @Column(name = "NM_Navegador", length = 150) private String nmNavegador;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario v) { this.usuario = v; }
    public LocalDateTime getDtLogin() { return dtLogin; } public void setDtLogin(LocalDateTime v) { this.dtLogin = v; }
    public LocalDateTime getDtLogout() { return dtLogout; } public void setDtLogout(LocalDateTime v) { this.dtLogout = v; }
    public String getNrIp() { return nrIp; } public void setNrIp(String v) { this.nrIp = v; }
    public String getNmDispositivo() { return nmDispositivo; } public void setNmDispositivo(String v) { this.nmDispositivo = v; }
    public String getNmNavegador() { return nmNavegador; } public void setNmNavegador(String v) { this.nmNavegador = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
