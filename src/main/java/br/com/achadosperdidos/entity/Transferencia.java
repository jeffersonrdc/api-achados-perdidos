package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "transferencia")
public class Transferencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Transferencia") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_LocalOrigem") private Local localOrigem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_LocalDestino", nullable = false) private Local localDestino;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_UsuarioResponsavel") private Usuario responsavel;
    @Column(name = "NM_Receptor", length = 150) private String nmReceptor;
    @Column(name = "DS_Motivo", length = 500) private String dsMotivo;
    @Column(name = "TP_Status", nullable = false, length = 30) private String tpStatus = "CONCLUIDA";
    @Column(name = "DT_Transferencia", nullable = false) private LocalDateTime dtTransferencia;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_UsuarioCadastro") private Usuario usuarioCadastro;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_UsuarioAlteracao") private Usuario usuarioAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public Local getLocalOrigem() { return localOrigem; } public void setLocalOrigem(Local v) { this.localOrigem = v; }
    public Local getLocalDestino() { return localDestino; } public void setLocalDestino(Local v) { this.localDestino = v; }
    public Usuario getResponsavel() { return responsavel; } public void setResponsavel(Usuario v) { this.responsavel = v; }
    public String getNmReceptor() { return nmReceptor; } public void setNmReceptor(String v) { this.nmReceptor = v; }
    public String getDsMotivo() { return dsMotivo; } public void setDsMotivo(String v) { this.dsMotivo = v; }
    public String getTpStatus() { return tpStatus; } public void setTpStatus(String v) { this.tpStatus = v; }
    public LocalDateTime getDtTransferencia() { return dtTransferencia; } public void setDtTransferencia(LocalDateTime v) { this.dtTransferencia = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Usuario getUsuarioCadastro() { return usuarioCadastro; } public void setUsuarioCadastro(Usuario v) { this.usuarioCadastro = v; }
    public Usuario getUsuarioAlteracao() { return usuarioAlteracao; } public void setUsuarioAlteracao(Usuario v) { this.usuarioAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
