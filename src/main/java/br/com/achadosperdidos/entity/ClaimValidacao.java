package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "claim_validacao")
public class ClaimValidacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_ClaimValidacao") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Claim", nullable = false) private Claim claim;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @Column(name = "QT_Similaridade", precision = 5, scale = 2) private BigDecimal qtSimilaridade;
    @Column(name = "ST_Resultado", nullable = false, length = 30) private String stResultado = "PENDENTE";
    @Column(name = "DT_Validacao") private LocalDateTime dtValidacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Claim getClaim() { return claim; } public void setClaim(Claim v) { this.claim = v; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public BigDecimal getQtSimilaridade() { return qtSimilaridade; } public void setQtSimilaridade(BigDecimal v) { this.qtSimilaridade = v; }
    public String getStResultado() { return stResultado; } public void setStResultado(String v) { this.stResultado = v; }
    public LocalDateTime getDtValidacao() { return dtValidacao; } public void setDtValidacao(LocalDateTime v) { this.dtValidacao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
