package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "localizacao")
public class Localizacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Localizacao") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Deposito", nullable = false) private Deposito deposito;
    @Column(name = "NM_Setor", length = 80) private String nmSetor;
    @Column(name = "NM_Corredor", length = 80) private String nmCorredor;
    @Column(name = "NM_Estante", length = 80) private String nmEstante;
    @Column(name = "NM_Prateleira", length = 80) private String nmPrateleira;
    @Column(name = "NM_Caixa", length = 80) private String nmCaixa;
    @Column(name = "NM_Posicao", length = 80) private String nmPosicao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Deposito getDeposito() { return deposito; }
    public void setDeposito(Deposito deposito) { this.deposito = deposito; }
    public String getNmSetor() { return nmSetor; }
    public void setNmSetor(String nmSetor) { this.nmSetor = nmSetor; }
    public String getNmCorredor() { return nmCorredor; }
    public void setNmCorredor(String nmCorredor) { this.nmCorredor = nmCorredor; }
    public String getNmEstante() { return nmEstante; }
    public void setNmEstante(String nmEstante) { this.nmEstante = nmEstante; }
    public String getNmPrateleira() { return nmPrateleira; }
    public void setNmPrateleira(String nmPrateleira) { this.nmPrateleira = nmPrateleira; }
    public String getNmCaixa() { return nmCaixa; }
    public void setNmCaixa(String nmCaixa) { this.nmCaixa = nmCaixa; }
    public String getNmPosicao() { return nmPosicao; }
    public void setNmPosicao(String nmPosicao) { this.nmPosicao = nmPosicao; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; }
    public void setDtAlteracao(LocalDateTime dtAlteracao) { this.dtAlteracao = dtAlteracao; }
    public Boolean getFgAtivo() { return fgAtivo; }
    public void setFgAtivo(Boolean fgAtivo) { this.fgAtivo = fgAtivo; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
