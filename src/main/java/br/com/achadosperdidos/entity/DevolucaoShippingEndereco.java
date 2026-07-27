package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "devolucao_shipping_endereco")
public class DevolucaoShippingEndereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DevolucaoShippingEndereco")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Devolucao", nullable = false)
    private Devolucao devolucao;

    @Column(name = "NM_Destinatario", nullable = false, length = 150)
    private String nmDestinatario;

    @Column(name = "NR_Cep", nullable = false, length = 8)
    private String nrCep;

    @Column(name = "NM_Logradouro", nullable = false, length = 200)
    private String nmLogradouro;

    @Column(name = "NR_Numero", nullable = false, length = 20)
    private String nrNumero;

    @Column(name = "DS_Complemento", length = 100)
    private String dsComplemento;

    @Column(name = "NM_Bairro", nullable = false, length = 100)
    private String nmBairro;

    @Column(name = "NM_Cidade", nullable = false, length = 100)
    private String nmCidade;

    @Column(name = "SG_Uf", nullable = false, length = 2)
    private String sgUf;

    @Column(name = "NR_Telefone", nullable = false, length = 20)
    private String nrTelefone;

    @Column(name = "DT_Cadastro", nullable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "DT_Alteracao")
    private LocalDateTime dtAlteracao;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
    public String getNmDestinatario() { return nmDestinatario; }
    public void setNmDestinatario(String nmDestinatario) { this.nmDestinatario = nmDestinatario; }
    public String getNrCep() { return nrCep; }
    public void setNrCep(String nrCep) { this.nrCep = nrCep; }
    public String getNmLogradouro() { return nmLogradouro; }
    public void setNmLogradouro(String nmLogradouro) { this.nmLogradouro = nmLogradouro; }
    public String getNrNumero() { return nrNumero; }
    public void setNrNumero(String nrNumero) { this.nrNumero = nrNumero; }
    public String getDsComplemento() { return dsComplemento; }
    public void setDsComplemento(String dsComplemento) { this.dsComplemento = dsComplemento; }
    public String getNmBairro() { return nmBairro; }
    public void setNmBairro(String nmBairro) { this.nmBairro = nmBairro; }
    public String getNmCidade() { return nmCidade; }
    public void setNmCidade(String nmCidade) { this.nmCidade = nmCidade; }
    public String getSgUf() { return sgUf; }
    public void setSgUf(String sgUf) { this.sgUf = sgUf; }
    public String getNrTelefone() { return nrTelefone; }
    public void setNrTelefone(String nrTelefone) { this.nrTelefone = nrTelefone; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; }
    public void setDtAlteracao(LocalDateTime dtAlteracao) { this.dtAlteracao = dtAlteracao; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
