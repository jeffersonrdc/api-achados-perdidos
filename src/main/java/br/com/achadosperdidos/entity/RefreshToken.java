package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Refresh token emitido, base para revogação/rotação (A07). */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_RefreshToken") private Long id;
    @Column(name = "NM_Jti", nullable = false, length = 64, unique = true) private String jti;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Usuario", nullable = false) private Usuario usuario;
    @Column(name = "DT_Expiracao", nullable = false) private LocalDateTime dtExpiracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Revogado", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgRevogado = false;
    @Column(name = "DT_Revogacao") private LocalDateTime dtRevogacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getJti() { return jti; } public void setJti(String v) { this.jti = v; }
    public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario v) { this.usuario = v; }
    public LocalDateTime getDtExpiracao() { return dtExpiracao; } public void setDtExpiracao(LocalDateTime v) { this.dtExpiracao = v; }
    public Boolean getFgRevogado() { return fgRevogado; } public void setFgRevogado(Boolean v) { this.fgRevogado = v; }
    public LocalDateTime getDtRevogacao() { return dtRevogacao; } public void setDtRevogacao(LocalDateTime v) { this.dtRevogacao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
}
