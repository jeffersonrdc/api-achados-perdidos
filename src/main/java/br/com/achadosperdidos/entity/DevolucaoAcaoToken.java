package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "devolucao_acao_token")
public class DevolucaoAcaoToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DevolucaoAcaoToken")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Devolucao", nullable = false)
    private Devolucao devolucao;

    @Column(name = "TP_Acao", nullable = false, length = 40)
    private String tpAcao;

    @Column(name = "CD_Token", nullable = false, length = 64, unique = true)
    private String cdToken;

    @Column(name = "DT_Expiracao", nullable = false)
    private LocalDateTime dtExpiracao;

    @Column(name = "DT_Usado")
    private LocalDateTime dtUsado;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgAtivo = true;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_MultiUso", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgMultiUso = false;

    @Column(name = "DT_Cadastro", nullable = false)
    private LocalDateTime dtCadastro;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
    public String getTpAcao() { return tpAcao; }
    public void setTpAcao(String tpAcao) { this.tpAcao = tpAcao; }
    public String getCdToken() { return cdToken; }
    public void setCdToken(String cdToken) { this.cdToken = cdToken; }
    public LocalDateTime getDtExpiracao() { return dtExpiracao; }
    public void setDtExpiracao(LocalDateTime dtExpiracao) { this.dtExpiracao = dtExpiracao; }
    public LocalDateTime getDtUsado() { return dtUsado; }
    public void setDtUsado(LocalDateTime dtUsado) { this.dtUsado = dtUsado; }
    public Boolean getFgAtivo() { return fgAtivo; }
    public void setFgAtivo(Boolean fgAtivo) { this.fgAtivo = fgAtivo; }
    public Boolean getFgMultiUso() { return fgMultiUso; }
    public void setFgMultiUso(Boolean fgMultiUso) { this.fgMultiUso = fgMultiUso; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
