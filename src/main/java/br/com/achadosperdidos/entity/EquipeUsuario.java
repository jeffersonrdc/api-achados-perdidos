package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipe_usuario")
public class EquipeUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EquipeUsuario") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Equipe", nullable = false) private Equipe equipe;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Usuario", nullable = false) private Usuario usuario;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Equipe getEquipe() { return equipe; } public void setEquipe(Equipe v) { this.equipe = v; }
    public Usuario getUsuario() { return usuario; } public void setUsuario(Usuario v) { this.usuario = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
