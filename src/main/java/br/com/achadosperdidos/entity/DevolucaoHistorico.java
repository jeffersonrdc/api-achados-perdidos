package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "devolucao_historico")
public class DevolucaoHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DevolucaoHistorico")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Devolucao", nullable = false)
    private Devolucao devolucao;

    @Column(name = "TP_Evento", nullable = false, length = 40)
    private String tpEvento;

    @Column(name = "NM_Titulo", nullable = false, length = 200)
    private String nmTitulo;

    @Column(name = "DS_Descricao", columnDefinition = "TEXT")
    private String dsDescricao;

    @Column(name = "TP_Ator", length = 20)
    private String tpAtor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Operador")
    private Usuario operador;

    @Column(name = "NM_Ator", length = 150)
    private String nmAtor;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_EmailEnviado", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgEmailEnviado = false;

    @Column(name = "DS_EmailErro", length = 500)
    private String dsEmailErro;

    @Column(name = "JS_Metadata", columnDefinition = "JSON")
    private String jsMetadata;

    @Column(name = "DT_Evento", nullable = false)
    private LocalDateTime dtEvento;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
    public String getTpEvento() { return tpEvento; }
    public void setTpEvento(String tpEvento) { this.tpEvento = tpEvento; }
    public String getNmTitulo() { return nmTitulo; }
    public void setNmTitulo(String nmTitulo) { this.nmTitulo = nmTitulo; }
    public String getDsDescricao() { return dsDescricao; }
    public void setDsDescricao(String dsDescricao) { this.dsDescricao = dsDescricao; }
    public String getTpAtor() { return tpAtor; }
    public void setTpAtor(String tpAtor) { this.tpAtor = tpAtor; }
    public Usuario getOperador() { return operador; }
    public void setOperador(Usuario operador) { this.operador = operador; }
    public String getNmAtor() { return nmAtor; }
    public void setNmAtor(String nmAtor) { this.nmAtor = nmAtor; }
    public Boolean getFgEmailEnviado() { return fgEmailEnviado; }
    public void setFgEmailEnviado(Boolean fgEmailEnviado) { this.fgEmailEnviado = fgEmailEnviado; }
    public String getDsEmailErro() { return dsEmailErro; }
    public void setDsEmailErro(String dsEmailErro) { this.dsEmailErro = dsEmailErro; }
    public String getJsMetadata() { return jsMetadata; }
    public void setJsMetadata(String jsMetadata) { this.jsMetadata = jsMetadata; }
    public LocalDateTime getDtEvento() { return dtEvento; }
    public void setDtEvento(LocalDateTime dtEvento) { this.dtEvento = dtEvento; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
