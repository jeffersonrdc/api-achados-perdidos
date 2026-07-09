package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_movimentacao")
public class ItemMovimentacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ItemMovimentacao") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_LocalizacaoOrigem") private Localizacao localizacaoOrigem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_LocalizacaoDestino", nullable = false) private Localizacao localizacaoDestino;
    @Column(name = "TP_Movimento", nullable = false, length = 30) private String tpMovimento;
    @Column(name = "DS_Motivo", length = 500) private String dsMotivo;
    @Column(name = "DT_Movimento", nullable = false) private LocalDateTime dtMovimento;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Item getItem(){return item;} public void setItem(Item item){this.item=item;}
    public Localizacao getLocalizacaoOrigem(){return localizacaoOrigem;} public void setLocalizacaoOrigem(Localizacao localizacaoOrigem){this.localizacaoOrigem=localizacaoOrigem;}
    public Localizacao getLocalizacaoDestino(){return localizacaoDestino;} public void setLocalizacaoDestino(Localizacao localizacaoDestino){this.localizacaoDestino=localizacaoDestino;}
    public String getTpMovimento(){return tpMovimento;} public void setTpMovimento(String tpMovimento){this.tpMovimento=tpMovimento;}
    public String getDsMotivo(){return dsMotivo;} public void setDsMotivo(String dsMotivo){this.dsMotivo=dsMotivo;}
    public LocalDateTime getDtMovimento(){return dtMovimento;} public void setDtMovimento(LocalDateTime dtMovimento){this.dtMovimento=dtMovimento;}
    public LocalDateTime getDtCadastro(){return dtCadastro;} public void setDtCadastro(LocalDateTime dtCadastro){this.dtCadastro=dtCadastro;}
    public LocalDateTime getDtAlteracao(){return dtAlteracao;} public void setDtAlteracao(LocalDateTime dtAlteracao){this.dtAlteracao=dtAlteracao;}
    public Boolean getFgAtivo(){return fgAtivo;} public void setFgAtivo(Boolean fgAtivo){this.fgAtivo=fgAtivo;}
    public Boolean getFgExcluido(){return fgExcluido;} public void setFgExcluido(Boolean fgExcluido){this.fgExcluido=fgExcluido;}
}
