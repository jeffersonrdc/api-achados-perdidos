package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "portal_categoria_capa")
public class PortalCategoriaCapa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PortalCategoriaCapa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Arquivo", nullable = false)
    private Arquivo arquivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Evento", nullable = false)
    private Evento evento;

    @Column(name = "DT_Cadastro", nullable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "DT_Alteracao")
    private LocalDateTime dtAlteracao;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgAtivo = true;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Arquivo getArquivo() { return arquivo; }
    public void setArquivo(Arquivo arquivo) { this.arquivo = arquivo; }
    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; }
    public void setDtAlteracao(LocalDateTime dtAlteracao) { this.dtAlteracao = dtAlteracao; }
    public Boolean getFgAtivo() { return fgAtivo; }
    public void setFgAtivo(Boolean fgAtivo) { this.fgAtivo = fgAtivo; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
