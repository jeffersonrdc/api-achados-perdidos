package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "etiqueta_impressao")
public class EtiquetaImpressao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EtiquetaImpressao") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Operador") private Usuario operador;
    @Column(name = "TP_Impressao", nullable = false, length = 20) private String tpImpressao = "IMPRESSAO";
    @Column(name = "NM_Impressora", length = 120) private String nmImpressora;
    @Column(name = "NR_Identificador", length = 60) private String nrIdentificador;
    @Column(name = "DS_Motivo", length = 300) private String dsMotivo;
    @Column(name = "DT_Impressao", nullable = false) private LocalDateTime dtImpressao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public Usuario getOperador() { return operador; } public void setOperador(Usuario v) { this.operador = v; }
    public String getTpImpressao() { return tpImpressao; } public void setTpImpressao(String v) { this.tpImpressao = v; }
    public String getNmImpressora() { return nmImpressora; } public void setNmImpressora(String v) { this.nmImpressora = v; }
    public String getNrIdentificador() { return nrIdentificador; } public void setNrIdentificador(String v) { this.nrIdentificador = v; }
    public String getDsMotivo() { return dsMotivo; } public void setDsMotivo(String v) { this.dsMotivo = v; }
    public LocalDateTime getDtImpressao() { return dtImpressao; } public void setDtImpressao(LocalDateTime v) { this.dtImpressao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
