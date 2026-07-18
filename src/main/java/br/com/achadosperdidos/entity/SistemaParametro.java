package br.com.achadosperdidos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sistema_parametro")
public class SistemaParametro {
    @Id
    @Column(name = "NM_Chave", length = 80)
    private String nmChave;

    @Column(name = "DS_Valor", length = 500)
    private String dsValor;

    @Column(name = "DS_Descricao", length = 255)
    private String dsDescricao;

    @Column(name = "DT_Cadastro", nullable = false)
    private LocalDateTime dtCadastro;

    @Column(name = "DT_Alteracao")
    private LocalDateTime dtAlteracao;

    public String getNmChave() { return nmChave; }
    public void setNmChave(String nmChave) { this.nmChave = nmChave; }
    public String getDsValor() { return dsValor; }
    public void setDsValor(String dsValor) { this.dsValor = dsValor; }
    public String getDsDescricao() { return dsDescricao; }
    public void setDsDescricao(String dsDescricao) { this.dsDescricao = dsDescricao; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; }
    public void setDtAlteracao(LocalDateTime dtAlteracao) { this.dtAlteracao = dtAlteracao; }
}
