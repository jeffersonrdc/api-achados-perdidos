package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "devolucao_shipping_cotacao")
public class DevolucaoShippingCotacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DevolucaoShippingCotacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Devolucao", nullable = false)
    private Devolucao devolucao;

    @Column(name = "VL_Valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal vlValor;

    @Column(name = "SG_Moeda", nullable = false, length = 3)
    private String sgMoeda = "BRL";

    @Column(name = "QT_DiasEntregaEstimados", nullable = false)
    private Integer qtDiasEntregaEstimados = 0;

    @Column(name = "QT_DiasPrazoPostagem", nullable = false)
    private Integer qtDiasPrazoPostagem = 0;

    @Column(name = "DS_InstrucoesPagamento", nullable = false, columnDefinition = "TEXT")
    private String dsInstrucoesPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Operador")
    private Usuario operador;

    @Column(name = "DT_Informada", nullable = false)
    private LocalDateTime dtInformada;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
    public BigDecimal getVlValor() { return vlValor; }
    public void setVlValor(BigDecimal vlValor) { this.vlValor = vlValor; }
    public String getSgMoeda() { return sgMoeda; }
    public void setSgMoeda(String sgMoeda) { this.sgMoeda = sgMoeda; }
    public Integer getQtDiasEntregaEstimados() { return qtDiasEntregaEstimados; }
    public void setQtDiasEntregaEstimados(Integer qtDiasEntregaEstimados) { this.qtDiasEntregaEstimados = qtDiasEntregaEstimados; }
    public Integer getQtDiasPrazoPostagem() { return qtDiasPrazoPostagem; }
    public void setQtDiasPrazoPostagem(Integer qtDiasPrazoPostagem) { this.qtDiasPrazoPostagem = qtDiasPrazoPostagem; }
    public String getDsInstrucoesPagamento() { return dsInstrucoesPagamento; }
    public void setDsInstrucoesPagamento(String dsInstrucoesPagamento) { this.dsInstrucoesPagamento = dsInstrucoesPagamento; }
    public Usuario getOperador() { return operador; }
    public void setOperador(Usuario operador) { this.operador = operador; }
    public LocalDateTime getDtInformada() { return dtInformada; }
    public void setDtInformada(LocalDateTime dtInformada) { this.dtInformada = dtInformada; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
