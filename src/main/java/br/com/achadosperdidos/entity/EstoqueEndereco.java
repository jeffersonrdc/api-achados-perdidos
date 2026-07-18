package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "estoque_endereco")
public class EstoqueEndereco {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Endereco") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "IDR_Deposito", nullable = false) private Deposito deposito;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_EnderecoPai") private EstoqueEndereco enderecoPai;
    @Column(name = "TP_Nivel", nullable = false, length = 20) private String tpNivel;
    @Column(name = "NM_Endereco", nullable = false, length = 80) private String nmEndereco;
    @Column(name = "OR_Ordem", nullable = false) private Integer orOrdem = 0;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Deposito getDeposito() { return deposito; } public void setDeposito(Deposito v) { this.deposito = v; }
    public EstoqueEndereco getEnderecoPai() { return enderecoPai; } public void setEnderecoPai(EstoqueEndereco v) { this.enderecoPai = v; }
    public String getTpNivel() { return tpNivel; } public void setTpNivel(String v) { this.tpNivel = v; }
    public String getNmEndereco() { return nmEndereco; } public void setNmEndereco(String v) { this.nmEndereco = v; }
    public Integer getOrOrdem() { return orOrdem; } public void setOrOrdem(Integer v) { this.orOrdem = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
