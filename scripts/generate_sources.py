#!/usr/bin/env python3
"""Gera classes Java restantes do projeto api-achados-perdidos."""
import os

BASE = os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "br", "com", "achadosperdidos")

def w(rel_path: str, content: str):
    full = os.path.join(BASE, *rel_path.split("/"))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        f.write(content.strip() + "\n")
    print("OK", rel_path)

# --- ENTITIES ---
w("entity/PerfilCodigo.java", """
package br.com.achadosperdidos.entity;

public enum PerfilCodigo {
    ADMINISTRADOR, OPERADOR, ATENDENTE, CONSULTA;

    public String roleName() { return "ROLE_" + name(); }

    public static PerfilCodigo fromNmPerfil(String nmPerfil) {
        if (nmPerfil == null || nmPerfil.isBlank()) throw new IllegalArgumentException("Perfil inválido.");
        return PerfilCodigo.valueOf(nmPerfil.trim().toUpperCase().replace(' ', '_'));
    }
}
""")

w("entity/Empresa.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "empresa")
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Empresa") private Long id;
    @Column(name = "NM_RazaoSocial", nullable = false, length = 200) private String nmRazaoSocial;
    @Column(name = "NM_Fantasia", length = 200) private String nmFantasia;
    @Column(name = "NR_CNPJ", nullable = false, length = 14) private String nrCnpj;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmRazaoSocial() { return nmRazaoSocial; } public void setNmRazaoSocial(String v) { this.nmRazaoSocial = v; }
    public String getNmFantasia() { return nmFantasia; } public void setNmFantasia(String v) { this.nmFantasia = v; }
    public String getNrCnpj() { return nrCnpj; } public void setNrCnpj(String v) { this.nrCnpj = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNrTelefone() { return nrTelefone; } public void setNrTelefone(String v) { this.nrTelefone = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/Perfil.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "perfil")
public class Perfil {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Perfil") private Long id;
    @Column(name = "NM_Perfil", nullable = false, length = 100) private String nmPerfil;
    @Column(name = "DS_Perfil", length = 500) private String dsPerfil;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmPerfil() { return nmPerfil; } public void setNmPerfil(String v) { this.nmPerfil = v; }
    public String getDsPerfil() { return dsPerfil; } public void setDsPerfil(String v) { this.dsPerfil = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/Usuario.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Usuario") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Empresa", nullable = false) private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Perfil", nullable = false) private Perfil perfil;
    @Column(name = "NM_Usuario", nullable = false, length = 150) private String nmUsuario;
    @Column(name = "NM_Login", nullable = false, length = 80) private String nmLogin;
    @Column(name = "NM_Email", nullable = false, length = 150) private String nmEmail;
    @Column(name = "NM_Senha", nullable = false, length = 255) private String nmSenha;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; } public void setEmpresa(Empresa v) { this.empresa = v; }
    public Perfil getPerfil() { return perfil; } public void setPerfil(Perfil v) { this.perfil = v; }
    public String getNmUsuario() { return nmUsuario; } public void setNmUsuario(String v) { this.nmUsuario = v; }
    public String getNmLogin() { return nmLogin; } public void setNmLogin(String v) { this.nmLogin = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNmSenha() { return nmSenha; } public void setNmSenha(String v) { this.nmSenha = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/StatusItem.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "status_item")
public class StatusItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Status") private Long id;
    @Column(name = "NM_Status", nullable = false, length = 80) private String nmStatus;
    @Column(name = "DS_Status", length = 500) private String dsStatus;
    @Column(name = "OR_Ordem", nullable = false) private Integer orOrdem = 0;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Final", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgFinal = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmStatus() { return nmStatus; } public void setNmStatus(String v) { this.nmStatus = v; }
    public String getDsStatus() { return dsStatus; } public void setDsStatus(String v) { this.dsStatus = v; }
    public Integer getOrOrdem() { return orOrdem; } public void setOrOrdem(Integer v) { this.orOrdem = v; }
    public Boolean getFgFinal() { return fgFinal; } public void setFgFinal(Boolean v) { this.fgFinal = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/Categoria.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "categoria")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Categoria") private Long id;
    @Column(name = "NM_Categoria", nullable = false, length = 120) private String nmCategoria;
    @Column(name = "DS_Categoria", length = 500) private String dsCategoria;
    @Column(name = "IC_Icone", length = 80) private String icIcone;
    @Column(name = "OR_Ordem", nullable = false) private Integer orOrdem = 0;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmCategoria() { return nmCategoria; } public void setNmCategoria(String v) { this.nmCategoria = v; }
    public String getDsCategoria() { return dsCategoria; } public void setDsCategoria(String v) { this.dsCategoria = v; }
    public String getIcIcone() { return icIcone; } public void setIcIcone(String v) { this.icIcone = v; }
    public Integer getOrOrdem() { return orOrdem; } public void setOrOrdem(Integer v) { this.orOrdem = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/Evento.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "evento")
public class Evento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Evento") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Empresa", nullable = false) private Empresa empresa;
    @Column(name = "NM_Evento", nullable = false, length = 200) private String nmEvento;
    @Column(name = "DS_Evento", columnDefinition = "TEXT") private String dsEvento;
    @Column(name = "DT_Inicio", nullable = false) private LocalDateTime dtInicio;
    @Column(name = "DT_Fim", nullable = false) private LocalDateTime dtFim;
    @Column(name = "NM_Local", length = 200) private String nmLocal;
    @Column(name = "NM_Cidade", length = 100) private String nmCidade;
    @Column(name = "SG_UF", length = 2) private String sgUf;
    @Column(name = "QT_DiasRetencao", nullable = false) private Integer qtDiasRetencao = 90;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; } public void setEmpresa(Empresa v) { this.empresa = v; }
    public String getNmEvento() { return nmEvento; } public void setNmEvento(String v) { this.nmEvento = v; }
    public String getDsEvento() { return dsEvento; } public void setDsEvento(String v) { this.dsEvento = v; }
    public LocalDateTime getDtInicio() { return dtInicio; } public void setDtInicio(LocalDateTime v) { this.dtInicio = v; }
    public LocalDateTime getDtFim() { return dtFim; } public void setDtFim(LocalDateTime v) { this.dtFim = v; }
    public String getNmLocal() { return nmLocal; } public void setNmLocal(String v) { this.nmLocal = v; }
    public String getNmCidade() { return nmCidade; } public void setNmCidade(String v) { this.nmCidade = v; }
    public String getSgUf() { return sgUf; } public void setSgUf(String v) { this.sgUf = v; }
    public Integer getQtDiasRetencao() { return qtDiasRetencao; } public void setQtDiasRetencao(Integer v) { this.qtDiasRetencao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/Item.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "item")
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Item") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Categoria", nullable = false) private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Status", nullable = false) private StatusItem status;
    @Column(name = "CD_Item", nullable = false, length = 50) private String cdItem;
    @Column(name = "NM_Titulo", nullable = false, length = 200) private String nmTitulo;
    @Column(name = "DS_Item", columnDefinition = "TEXT") private String dsItem;
    @Column(name = "NM_Marca", length = 100) private String nmMarca;
    @Column(name = "NM_Modelo", length = 100) private String nmModelo;
    @Column(name = "NM_Cor", length = 60) private String nmCor;
    @Column(name = "DT_Encontrado", nullable = false) private LocalDate dtEncontrado;
    @Column(name = "HR_Encontrado") private LocalTime hrEncontrado;
    @Column(name = "NM_LocalEncontrado", length = 200) private String nmLocalEncontrado;
    @Column(name = "VL_Estimado", precision = 12, scale = 2) private BigDecimal vlEstimado;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Entregue", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgEntregue = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Descartado", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgDescartado = false;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria v) { this.categoria = v; }
    public StatusItem getStatus() { return status; } public void setStatus(StatusItem v) { this.status = v; }
    public String getCdItem() { return cdItem; } public void setCdItem(String v) { this.cdItem = v; }
    public String getNmTitulo() { return nmTitulo; } public void setNmTitulo(String v) { this.nmTitulo = v; }
    public String getDsItem() { return dsItem; } public void setDsItem(String v) { this.dsItem = v; }
    public String getNmMarca() { return nmMarca; } public void setNmMarca(String v) { this.nmMarca = v; }
    public String getNmModelo() { return nmModelo; } public void setNmModelo(String v) { this.nmModelo = v; }
    public String getNmCor() { return nmCor; } public void setNmCor(String v) { this.nmCor = v; }
    public LocalDate getDtEncontrado() { return dtEncontrado; } public void setDtEncontrado(LocalDate v) { this.dtEncontrado = v; }
    public LocalTime getHrEncontrado() { return hrEncontrado; } public void setHrEncontrado(LocalTime v) { this.hrEncontrado = v; }
    public String getNmLocalEncontrado() { return nmLocalEncontrado; } public void setNmLocalEncontrado(String v) { this.nmLocalEncontrado = v; }
    public BigDecimal getVlEstimado() { return vlEstimado; } public void setVlEstimado(BigDecimal v) { this.vlEstimado = v; }
    public Boolean getFgEntregue() { return fgEntregue; } public void setFgEntregue(Boolean v) { this.fgEntregue = v; }
    public Boolean getFgDescartado() { return fgDescartado; } public void setFgDescartado(Boolean v) { this.fgDescartado = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

w("entity/Claim.java", """
package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "claim")
public class Claim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Claim") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Categoria", nullable = false) private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Status", nullable = false) private StatusItem status;
    @Column(name = "NM_Nome", nullable = false, length = 150) private String nmNome;
    @Column(name = "NR_CPF", length = 11) private String nrCpf;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "NM_Objeto", nullable = false, length = 200) private String nmObjeto;
    @Column(name = "DS_Objeto", columnDefinition = "TEXT") private String dsObjeto;
    @Column(name = "NM_Marca", length = 100) private String nmMarca;
    @Column(name = "NM_Modelo", length = 100) private String nmModelo;
    @Column(name = "NM_Cor", length = 60) private String nmCor;
    @Column(name = "DT_Perdeu") private LocalDate dtPerdeu;
    @Column(name = "HR_Perdeu") private LocalTime hrPerdeu;
    @Column(name = "NM_Local", length = 200) private String nmLocal;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria v) { this.categoria = v; }
    public StatusItem getStatus() { return status; } public void setStatus(StatusItem v) { this.status = v; }
    public String getNmNome() { return nmNome; } public void setNmNome(String v) { this.nmNome = v; }
    public String getNrCpf() { return nrCpf; } public void setNrCpf(String v) { this.nrCpf = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNrTelefone() { return nrTelefone; } public void setNrTelefone(String v) { this.nrTelefone = v; }
    public String getNmObjeto() { return nmObjeto; } public void setNmObjeto(String v) { this.nmObjeto = v; }
    public String getDsObjeto() { return dsObjeto; } public void setDsObjeto(String v) { this.dsObjeto = v; }
    public String getNmMarca() { return nmMarca; } public void setNmMarca(String v) { this.nmMarca = v; }
    public String getNmModelo() { return nmModelo; } public void setNmModelo(String v) { this.nmModelo = v; }
    public String getNmCor() { return nmCor; } public void setNmCor(String v) { this.nmCor = v; }
    public LocalDate getDtPerdeu() { return dtPerdeu; } public void setDtPerdeu(LocalDate v) { this.dtPerdeu = v; }
    public LocalTime getHrPerdeu() { return hrPerdeu; } public void setHrPerdeu(LocalTime v) { this.hrPerdeu = v; }
    public String getNmLocal() { return nmLocal; } public void setNmLocal(String v) { this.nmLocal = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
""")

# --- REPOSITORIES ---
repos = {
"repository/UsuarioRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @EntityGraph(attributePaths = {"perfil", "empresa"})
    Optional<Usuario> findWithPerfilByNmEmail(String nmEmail);
    @EntityGraph(attributePaths = {"perfil", "empresa"})
    Optional<Usuario> findWithPerfilByNmLogin(String nmLogin);
    Optional<Usuario> findByNmEmail(String nmEmail);
}
""",
"repository/EventoRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByFgExcluidoFalseOrderByDtInicioDesc();
    List<Evento> findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc();
}
""",
"repository/CategoriaRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
}
""",
"repository/StatusItemRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.StatusItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatusItemRepository extends JpaRepository<StatusItem, Long> {
    List<StatusItem> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    Optional<StatusItem> findByNmStatus(String nmStatus);
}
""",
"repository/ItemRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByFgExcluidoFalse(Pageable pageable);
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);
}
""",
"repository/ClaimRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Claim> findByFgExcluidoFalse(Pageable pageable);
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Claim> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);
}
""",
"repository/EmpresaRepository.java": """
package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {}
""",
}
for path, content in repos.items():
    w(path, content)

# --- DTOs ---
w("controller/dto/LoginRequest.java", """
package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login (email ou login)")
public record LoginRequest(
        @NotBlank @Schema(description = "NM_Email ou NM_Login") String identificador,
        @NotBlank @Schema(description = "Senha") String senha
) {}
""")

w("controller/dto/LoginResponse.java", """
package br.com.achadosperdidos.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação")
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tipoToken,
        UsuarioResumoResponse usuario
) {
    public static LoginResponse of(String access, String refresh, UsuarioResumoResponse usuario) {
        return new LoginResponse(access, refresh, "Bearer", usuario);
    }
}
""")

w("controller/dto/RefreshRequest.java", """
package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
""")

w("controller/dto/RefreshResponse.java", """
package br.com.achadosperdidos.controller.dto;

public record RefreshResponse(String accessToken, String refreshToken, String tipoToken) {
    public static RefreshResponse of(String access, String refresh) {
        return new RefreshResponse(access, refresh, "Bearer");
    }
}
""")

w("controller/dto/UsuarioResumoResponse.java", """
package br.com.achadosperdidos.controller.dto;

public record UsuarioResumoResponse(String id, String nmUsuario, String nmEmail, String nmLogin, String nmPerfil) {}
""")

w("controller/dto/EventoCreateRequest.java", """
package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventoCreateRequest(
        @NotBlank String idEmpresa,
        @NotBlank String nmEvento,
        String dsEvento,
        @NotNull LocalDateTime dtInicio,
        @NotNull LocalDateTime dtFim,
        String nmLocal,
        String nmCidade,
        String sgUf,
        Integer qtDiasRetencao
) {}
""")

w("controller/dto/EventoResponse.java", """
package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record EventoResponse(
        String id, String nmEvento, String dsEvento, LocalDateTime dtInicio, LocalDateTime dtFim,
        String nmLocal, String nmCidade, String sgUf, Integer qtDiasRetencao, Boolean fgAtivo
) {}
""")

w("controller/dto/CategoriaResponse.java", """
package br.com.achadosperdidos.controller.dto;

public record CategoriaResponse(String id, String nmCategoria, String dsCategoria, String icIcone, Integer orOrdem, Boolean fgAtivo) {}
""")

w("controller/dto/StatusItemResponse.java", """
package br.com.achadosperdidos.controller.dto;

public record StatusItemResponse(String id, String nmStatus, String dsStatus, Integer orOrdem, Boolean fgFinal) {}
""")

w("controller/dto/ItemCreateRequest.java", """
package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ItemCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String idCategoria,
        String idStatus,
        @NotBlank String nmTitulo,
        String dsItem,
        String nmMarca,
        String nmModelo,
        String nmCor,
        @NotNull LocalDate dtEncontrado,
        LocalTime hrEncontrado,
        String nmLocalEncontrado,
        BigDecimal vlEstimado
) {}
""")

w("controller/dto/ItemResponse.java", """
package br.com.achadosperdidos.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemResponse(
        String id, String cdItem, String nmTitulo, String dsItem, String nmMarca, String nmModelo, String nmCor,
        LocalDate dtEncontrado, BigDecimal vlEstimado, String nmStatus, String nmCategoria, String nmEvento,
        Boolean fgEntregue, Boolean fgDescartado, LocalDateTime dtCadastro
) {}
""")

w("controller/dto/ClaimCreateRequest.java", """
package br.com.achadosperdidos.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClaimCreateRequest(
        @NotBlank String idEvento,
        @NotBlank String idCategoria,
        String idStatus,
        @NotBlank String nmNome,
        String nrCpf,
        String nmEmail,
        String nrTelefone,
        @NotBlank String nmObjeto,
        String dsObjeto,
        String nmMarca,
        String nmModelo,
        String nmCor,
        LocalDate dtPerdeu,
        LocalTime hrPerdeu,
        String nmLocal
) {}
""")

w("controller/dto/ClaimResponse.java", """
package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimResponse(
        String id, String nmNome, String nmObjeto, String nmMarca, String nmModelo, String nmCor,
        LocalDate dtPerdeu, String nmStatus, String nmCategoria, String nmEvento, LocalDateTime dtCadastro
) {}
""")

w("controller/dto/DashboardEventoResponse.java", """
package br.com.achadosperdidos.controller.dto;

public record DashboardEventoResponse(
        String idEvento, String nmEvento, Long qtItensTotal, Long qtItensPendentes,
        Long qtItensDevolvidos, Long qtClaimsTotal
) {}
""")

# --- SERVICES ---
w("service/CustomUserDetailsService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.PerfilCodigo;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(username)
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        String role = PerfilCodigo.fromNmPerfil(usuario.getPerfil().getNmPerfil()).roleName();
        return User.builder()
                .username(usuario.getNmEmail())
                .password(usuario.getNmSenha())
                .disabled(!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido()))
                .authorities(role)
                .build();
    }
}
""")

w("service/AuthService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record AuthTokens(String accessToken, String refreshToken) {}

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public AuthTokens authenticate(String identificador, String senha) {
        Usuario usuario = usuarioRepository.findWithPerfilByNmEmail(identificador.trim())
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador.trim()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!passwordEncoder.matches(senha, usuario.getNmSenha())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (!Boolean.TRUE.equals(usuario.getFgAtivo()) || Boolean.TRUE.equals(usuario.getFgExcluido())) {
            throw new BadCredentialsException("Usuário inativo");
        }
        String subject = usuario.getNmEmail();
        return new AuthTokens(jwtUtil.generateAccessToken(subject), jwtUtil.generateRefreshToken(subject));
    }

    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        String subject = jwtUtil.getSubjectFromToken(refreshToken);
        if (subject == null || usuarioRepository.findByNmEmail(subject).isEmpty()) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        return new AuthTokens(jwtUtil.generateAccessToken(subject), jwtUtil.generateRefreshToken(subject));
    }
}
""")

w("service/UsuarioContextService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioContextService {
    private final UsuarioRepository usuarioRepository;
    public UsuarioContextService(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Transactional(readOnly = true)
    public Usuario requireUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new IllegalStateException("Usuário não autenticado.");
        }
        return usuarioRepository.findWithPerfilByNmEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public Long requireUsuarioLogadoId() { return requireUsuarioLogado().getId(); }
}
""")

w("service/EventoService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.entity.Empresa;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EmpresaRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventoService {
    private final EventoRepository eventoRepository;
    private final EmpresaRepository empresaRepository;
    private final SignedResourceIdCodec idCodec;
    private final UsuarioContextService usuarioContextService;

    public EventoService(EventoRepository eventoRepository, EmpresaRepository empresaRepository,
                         SignedResourceIdCodec idCodec, UsuarioContextService usuarioContextService) {
        this.eventoRepository = eventoRepository;
        this.empresaRepository = empresaRepository;
        this.idCodec = idCodec;
        this.usuarioContextService = usuarioContextService;
    }

    @Transactional
    public EventoResponse create(EventoCreateRequest request) {
        Long empresaId = idCodec.decodeEmpresaId(request.idEmpresa());
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
        Evento evento = new Evento();
        evento.setEmpresa(empresa);
        evento.setNmEvento(request.nmEvento().trim());
        evento.setDsEvento(request.dsEvento());
        evento.setDtInicio(request.dtInicio());
        evento.setDtFim(request.dtFim());
        evento.setNmLocal(request.nmLocal());
        evento.setNmCidade(request.nmCidade());
        evento.setSgUf(request.sgUf());
        evento.setQtDiasRetencao(request.qtDiasRetencao() != null ? request.qtDiasRetencao() : 90);
        evento.setDtCadastro(LocalDateTime.now());
        evento.setFgAtivo(true);
        evento.setFgExcluido(false);
        return toResponse(eventoRepository.save(evento));
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> findAll(boolean incluirInativos) {
        List<Evento> list = incluirInativos
                ? eventoRepository.findByFgExcluidoFalseOrderByDtInicioDesc()
                : eventoRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByDtInicioDesc();
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeEventoId(idToken)));
    }

    @Transactional
    public void softDelete(String idToken) {
        Evento evento = findEntity(idCodec.decodeEventoId(idToken));
        evento.setFgExcluido(true);
        evento.setFgAtivo(false);
        evento.setDtAlteracao(LocalDateTime.now());
        eventoRepository.save(evento);
    }

    private Evento findEntity(Long id) {
        return eventoRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
    }

    private EventoResponse toResponse(Evento e) {
        return new EventoResponse(
                idCodec.encodeEventoId(e.getId()), e.getNmEvento(), e.getDsEvento(), e.getDtInicio(), e.getDtFim(),
                e.getNmLocal(), e.getNmCidade(), e.getSgUf(), e.getQtDiasRetencao(), e.getFgAtivo());
    }
}
""")

w("service/CategoriaService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.CategoriaRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final SignedResourceIdCodec idCodec;
    public CategoriaService(CategoriaRepository categoriaRepository, SignedResourceIdCodec idCodec) {
        this.categoriaRepository = categoriaRepository; this.idCodec = idCodec;
    }
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAll() {
        return categoriaRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc().stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public CategoriaResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeCategoriaId(idToken)));
    }
    Categoria findEntity(Long id) {
        return categoriaRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }
    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(idCodec.encodeCategoriaId(c.getId()), c.getNmCategoria(), c.getDsCategoria(), c.getIcIcone(), c.getOrOrdem(), c.getFgAtivo());
    }
}
""")

w("service/StatusItemService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.StatusItemResponse;
import br.com.achadosperdidos.entity.StatusItem;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.StatusItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StatusItemService {
    private final StatusItemRepository statusItemRepository;
    private final SignedResourceIdCodec idCodec;
    public StatusItemService(StatusItemRepository statusItemRepository, SignedResourceIdCodec idCodec) {
        this.statusItemRepository = statusItemRepository; this.idCodec = idCodec;
    }
    @Transactional(readOnly = true)
    public List<StatusItemResponse> findAll() {
        return statusItemRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc().stream().map(this::toResponse).toList();
    }
    StatusItem findEntity(Long id) {
        return statusItemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status não encontrado."));
    }
    StatusItem findByNomeOrDefault(String nmStatus, String defaultName) {
        if (nmStatus != null && !nmStatus.isBlank()) {
            return statusItemRepository.findByNmStatus(nmStatus.trim())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status não encontrado: " + nmStatus));
        }
        return statusItemRepository.findByNmStatus(defaultName)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status padrão não encontrado: " + defaultName));
    }
    private StatusItemResponse toResponse(StatusItem s) {
        return new StatusItemResponse(idCodec.encodeStatusId(s.getId()), s.getNmStatus(), s.getDsStatus(), s.getOrOrdem(), s.getFgFinal());
    }
}
""")

w("service/ItemService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ItemCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemResponse;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final EventoRepository eventoRepository;
    private final CategoriaService categoriaService;
    private final StatusItemService statusItemService;
    private final SignedResourceIdCodec idCodec;
    private final UsuarioContextService usuarioContextService;

    public ItemService(ItemRepository itemRepository, EventoRepository eventoRepository,
                       CategoriaService categoriaService, StatusItemService statusItemService,
                       SignedResourceIdCodec idCodec, UsuarioContextService usuarioContextService) {
        this.itemRepository = itemRepository; this.eventoRepository = eventoRepository;
        this.categoriaService = categoriaService; this.statusItemService = statusItemService;
        this.idCodec = idCodec; this.usuarioContextService = usuarioContextService;
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        Long eventoId = idCodec.decodeEventoId(request.idEvento());
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Item item = new Item();
        item.setEvento(evento);
        item.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        item.setStatus(request.idStatus() != null && !request.idStatus().isBlank()
                ? statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus()))
                : statusItemService.findByNomeOrDefault(null, "Recebido"));
        item.setCdItem(gerarCodigoItem());
        item.setNmTitulo(request.nmTitulo().trim());
        item.setDsItem(request.dsItem());
        item.setNmMarca(request.nmMarca());
        item.setNmModelo(request.nmModelo());
        item.setNmCor(request.nmCor());
        item.setDtEncontrado(request.dtEncontrado());
        item.setHrEncontrado(request.hrEncontrado());
        item.setNmLocalEncontrado(request.nmLocalEncontrado());
        item.setVlEstimado(request.vlEstimado());
        item.setDtCadastro(LocalDateTime.now());
        item.setFgAtivo(true);
        item.setFgExcluido(false);
        return toResponse(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public ApiPage<ItemResponse> findAll(Integer page, Integer limit, String idEvento) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Item> result = (idEvento != null && !idEvento.isBlank())
                ? itemRepository.findByEvento_IdAndFgExcluidoFalse(idCodec.decodeEventoId(idEvento), PageRequest.of(p - 1, l))
                : itemRepository.findByFgExcluidoFalse(PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        var meta = new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages());
        return ApiPage.paged(content, meta);
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeItemId(idToken)));
    }

    @Transactional
    public void softDelete(String idToken) {
        Item item = findEntity(idCodec.decodeItemId(idToken));
        item.setFgExcluido(true);
        item.setFgAtivo(false);
        item.setDtAlteracao(LocalDateTime.now());
        itemRepository.save(item);
    }

    private Item findEntity(Long id) {
        return itemRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
    }

    private String gerarCodigoItem() {
        return "ITM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String decodeStatusNome(String idStatusToken) {
        return statusItemService.findEntity(idCodec.decodeStatusId(idStatusToken)).getNmStatus();
    }

    private ItemResponse toResponse(Item i) {
        return new ItemResponse(
                idCodec.encodeItemId(i.getId()), i.getCdItem(), i.getNmTitulo(), i.getDsItem(),
                i.getNmMarca(), i.getNmModelo(), i.getNmCor(), i.getDtEncontrado(), i.getVlEstimado(),
                i.getStatus().getNmStatus(), i.getCategoria().getNmCategoria(), i.getEvento().getNmEvento(),
                i.getFgEntregue(), i.getFgDescartado(), i.getDtCadastro());
    }
}
""")

w("service/ClaimService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ClaimCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.Evento;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.pagination.PaginationMeta;
import br.com.achadosperdidos.pagination.PaginationParams;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.EventoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final EventoRepository eventoRepository;
    private final CategoriaService categoriaService;
    private final StatusItemService statusItemService;
    private final SignedResourceIdCodec idCodec;

    public ClaimService(ClaimRepository claimRepository, EventoRepository eventoRepository,
                        CategoriaService categoriaService, StatusItemService statusItemService,
                        SignedResourceIdCodec idCodec) {
        this.claimRepository = claimRepository; this.eventoRepository = eventoRepository;
        this.categoriaService = categoriaService; this.statusItemService = statusItemService; this.idCodec = idCodec;
    }

    @Transactional
    public ClaimResponse create(ClaimCreateRequest request) {
        Long eventoId = idCodec.decodeEventoId(request.idEvento());
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));
        Claim claim = new Claim();
        claim.setEvento(evento);
        claim.setCategoria(categoriaService.findEntity(idCodec.decodeCategoriaId(request.idCategoria())));
        claim.setStatus(request.idStatus() != null && !request.idStatus().isBlank()
                ? statusItemService.findEntity(idCodec.decodeStatusId(request.idStatus()))
                : statusItemService.findByNomeOrDefault(null, "Claim Aberto"));
        claim.setNmNome(request.nmNome().trim());
        claim.setNrCpf(request.nrCpf());
        claim.setNmEmail(request.nmEmail());
        claim.setNrTelefone(request.nrTelefone());
        claim.setNmObjeto(request.nmObjeto().trim());
        claim.setDsObjeto(request.dsObjeto());
        claim.setNmMarca(request.nmMarca());
        claim.setNmModelo(request.nmModelo());
        claim.setNmCor(request.nmCor());
        claim.setDtPerdeu(request.dtPerdeu());
        claim.setHrPerdeu(request.hrPerdeu());
        claim.setNmLocal(request.nmLocal());
        claim.setDtCadastro(LocalDateTime.now());
        claim.setFgAtivo(true);
        claim.setFgExcluido(false);
        return toResponse(claimRepository.save(claim));
    }

    @Transactional(readOnly = true)
    public ApiPage<ClaimResponse> findAll(Integer page, Integer limit, String idEvento) {
        int p = PaginationParams.resolvePage(page);
        int l = PaginationParams.resolveLimit(limit);
        Page<Claim> result = (idEvento != null && !idEvento.isBlank())
                ? claimRepository.findByEvento_IdAndFgExcluidoFalse(idCodec.decodeEventoId(idEvento), PageRequest.of(p - 1, l))
                : claimRepository.findByFgExcluidoFalse(PageRequest.of(p - 1, l));
        var content = result.getContent().stream().map(this::toResponse).toList();
        return ApiPage.paged(content, new PaginationMeta(p, l, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ClaimResponse findById(String idToken) {
        return toResponse(findEntity(idCodec.decodeClaimId(idToken)));
    }

    @Transactional
    public void softDelete(String idToken) {
        Claim claim = findEntity(idCodec.decodeClaimId(idToken));
        claim.setFgExcluido(true);
        claim.setFgAtivo(false);
        claim.setDtAlteracao(LocalDateTime.now());
        claimRepository.save(claim);
    }

    private Claim findEntity(Long id) {
        return claimRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Claim não encontrado."));
    }

    private ClaimResponse toResponse(Claim c) {
        return new ClaimResponse(
                idCodec.encodeClaimId(c.getId()), c.getNmNome(), c.getNmObjeto(), c.getNmMarca(), c.getNmModelo(), c.getNmCor(),
                c.getDtPerdeu(), c.getStatus().getNmStatus(), c.getCategoria().getNmCategoria(), c.getEvento().getNmEvento(), c.getDtCadastro());
    }
}
""")

w("service/DashboardService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.DashboardEventoResponse;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DashboardService {
    @PersistenceContext private EntityManager em;
    private final SignedResourceIdCodec idCodec;
    public DashboardService(SignedResourceIdCodec idCodec) { this.idCodec = idCodec; }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<DashboardEventoResponse> listarResumoEventos() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ID_Evento, NM_Evento, QT_ItensTotal, QT_ItensPendentes, QT_ItensDevolvidos, QT_ClaimsTotal FROM VW_Dashboard_Evento"
        ).getResultList();
        return rows.stream().map(r -> new DashboardEventoResponse(
                idCodec.encodeEventoId(((Number) r[0]).longValue()),
                (String) r[1],
                r[2] != null ? ((Number) r[2]).longValue() : 0L,
                r[3] != null ? ((Number) r[3]).longValue() : 0L,
                r[4] != null ? ((Number) r[4]).longValue() : 0L,
                r[5] != null ? ((Number) r[5]).longValue() : 0L
        )).toList();
    }
}
""")

w("service/UsuarioService.java", """
package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.UsuarioResumoResponse;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final SignedResourceIdCodec idCodec;
    public UsuarioService(UsuarioRepository usuarioRepository, SignedResourceIdCodec idCodec) {
        this.usuarioRepository = usuarioRepository; this.idCodec = idCodec;
    }
    @Transactional(readOnly = true)
    public UsuarioResumoResponse toResumo(Usuario usuario) {
        return new UsuarioResumoResponse(
                idCodec.encodeUsuarioId(usuario.getId()),
                usuario.getNmUsuario(),
                usuario.getNmEmail(),
                usuario.getNmLogin(),
                usuario.getPerfil().getNmPerfil());
    }
    @Transactional(readOnly = true)
    public UsuarioResumoResponse findResumoByEmail(String email) {
        return usuarioRepository.findWithPerfilByNmEmail(email).map(this::toResumo)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
    @Transactional(readOnly = true)
    public UsuarioResumoResponse findResumoByIdentificador(String identificador) {
        return usuarioRepository.findWithPerfilByNmEmail(identificador)
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador))
                .map(this::toResumo)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
}
""")

print("Services done")

# --- CONTROLLERS ---
w("controller/AuthController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.service.AuthService;
import br.com.achadosperdidos.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {
    private final AuthService authService;
    private final UsuarioService usuarioService;
    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService; this.usuarioService = usuarioService;
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authService.authenticate(request.identificador(), request.senha());
        var usuario = usuarioService.findResumoByIdentificador(request.identificador());
        return ResponseEntity.ok(LoginResponse.of(tokens.accessToken(), tokens.refreshToken(), usuario));
    }
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(RefreshResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }
}
""")

# fix UsuarioService findResumoByIdentificador - add to service block via separate write after gen

w("controller/EventoController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EventoCreateRequest;
import br.com.achadosperdidos.controller.dto.EventoResponse;
import br.com.achadosperdidos.service.EventoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/eventos")
@Tag(name = "Eventos")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {
    private final EventoService eventoService;
    public EventoController(EventoService eventoService) { this.eventoService = eventoService; }

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoResponse> create(@Valid @RequestBody EventoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.create(request));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<EventoResponse> findAll(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return eventoService.findAll(incluirInativos);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public EventoResponse findById(@PathVariable String id) { return eventoService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) { eventoService.softDelete(id); return ResponseEntity.noContent().build(); }
}
""")

w("controller/CategoriaController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.CategoriaResponse;
import br.com.achadosperdidos.service.CategoriaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {
    private final CategoriaService categoriaService;
    public CategoriaController(CategoriaService categoriaService) { this.categoriaService = categoriaService; }
    @GetMapping @PreAuthorize("isAuthenticated()")
    public List<CategoriaResponse> findAll() { return categoriaService.findAll(); }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()")
    public CategoriaResponse findById(@PathVariable String id) { return categoriaService.findById(id); }
}
""")

w("controller/StatusItemController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.StatusItemResponse;
import br.com.achadosperdidos.service.StatusItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/status-itens")
@Tag(name = "Status")
@SecurityRequirement(name = "bearerAuth")
public class StatusItemController {
    private final StatusItemService statusItemService;
    public StatusItemController(StatusItemService statusItemService) { this.statusItemService = statusItemService; }
    @GetMapping
    public List<StatusItemResponse> findAll() { return statusItemService.findAll(); }
}
""")

w("controller/ItemController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ItemCreateRequest;
import br.com.achadosperdidos.controller.dto.ItemResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/itens")
@Tag(name = "Itens")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {
    private final ItemService itemService;
    public ItemController(ItemService itemService) { this.itemService = itemService; }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public ApiPage<ItemResponse> findAll(@RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) String idEvento) {
        return itemService.findAll(page, limit, idEvento);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public ItemResponse findById(@PathVariable String id) { return itemService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<Void> delete(@PathVariable String id) { itemService.softDelete(id); return ResponseEntity.noContent().build(); }
}
""")

w("controller/ClaimController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.ClaimCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.ClaimService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
@Tag(name = "Claims")
@SecurityRequirement(name = "bearerAuth")
public class ClaimController {
    private final ClaimService claimService;
    public ClaimController(ClaimService claimService) { this.claimService = claimService; }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(request));
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONSULTA')")
    public ApiPage<ClaimResponse> findAll(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer limit,
                                          @RequestParam(required = false) String idEvento) {
        return claimService.findAll(page, limit, idEvento);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONSULTA')")
    public ClaimResponse findById(@PathVariable String id) { return claimService.findById(id); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<Void> delete(@PathVariable String id) { claimService.softDelete(id); return ResponseEntity.noContent().build(); }
}
""")

w("controller/DashboardController.java", """
package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.DashboardEventoResponse;
import br.com.achadosperdidos.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }
    @GetMapping("/eventos") @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','ATENDENTE','CONSULTA')")
    public List<DashboardEventoResponse> resumoEventos() { return dashboardService.listarResumoEventos(); }
}
""")

print("Controllers done")
