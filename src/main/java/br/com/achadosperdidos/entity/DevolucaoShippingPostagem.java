package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "devolucao_shipping_postagem")
public class DevolucaoShippingPostagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DevolucaoShippingPostagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Devolucao", nullable = false)
    private Devolucao devolucao;

    @Column(name = "DT_Postagem", nullable = false)
    private LocalDate dtPostagem;

    @Column(name = "CD_Rastreio", nullable = false, length = 40)
    private String cdRastreio;

    @Column(name = "DS_Notas", length = 500)
    private String dsNotas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Operador")
    private Usuario operador;

    @Column(name = "DT_Registro", nullable = false)
    private LocalDateTime dtRegistro;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
    public LocalDate getDtPostagem() { return dtPostagem; }
    public void setDtPostagem(LocalDate dtPostagem) { this.dtPostagem = dtPostagem; }
    public String getCdRastreio() { return cdRastreio; }
    public void setCdRastreio(String cdRastreio) { this.cdRastreio = cdRastreio; }
    public String getDsNotas() { return dsNotas; }
    public void setDsNotas(String dsNotas) { this.dsNotas = dsNotas; }
    public Usuario getOperador() { return operador; }
    public void setOperador(Usuario operador) { this.operador = operador; }
    public LocalDateTime getDtRegistro() { return dtRegistro; }
    public void setDtRegistro(LocalDateTime dtRegistro) { this.dtRegistro = dtRegistro; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
