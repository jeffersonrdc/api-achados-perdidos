package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "item_campo")
public class ItemCampo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_ItemCampo") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_CategoriaCampo", nullable = false) private CategoriaCampo categoriaCampo;
    @Column(name = "VL_Texto", length = 500) private String vlTexto;
    @Column(name = "VL_Numero", precision = 18, scale = 4) private BigDecimal vlNumero;
    @Column(name = "VL_Data") private LocalDate vlData;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "VL_Boolean", columnDefinition = "TINYINT(1)") private Boolean vlBoolean;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public CategoriaCampo getCategoriaCampo() { return categoriaCampo; } public void setCategoriaCampo(CategoriaCampo v) { this.categoriaCampo = v; }
    public String getVlTexto() { return vlTexto; } public void setVlTexto(String v) { this.vlTexto = v; }
    public BigDecimal getVlNumero() { return vlNumero; } public void setVlNumero(BigDecimal v) { this.vlNumero = v; }
    public LocalDate getVlData() { return vlData; } public void setVlData(LocalDate v) { this.vlData = v; }
    public Boolean getVlBoolean() { return vlBoolean; } public void setVlBoolean(Boolean v) { this.vlBoolean = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
