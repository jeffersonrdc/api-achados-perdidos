package br.com.achadosperdidos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class SignedResourceIdCodec {

    private static final String TOKEN_PREFIX = "s2";
    private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    private final Mac macPrototype;

    /** Valores de exemplo/documentação que jamais podem ir a produção (A02/A05). */
    private static final java.util.Set<String> SEGREDOS_PROIBIDOS = java.util.Set.of(
            "7mP9!qL2#vN8@xD4$kR6^tY1&hJ5*eF3!uW0@pS8#zQ2",
            "troque-por-outro-segredo-com-pelo-menos-32-caracteres");

    public SignedResourceIdCodec(@Value("${app.resource-id.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("Defina app.resource-id.secret com pelo menos 32 caracteres.");
        }
        if (SEGREDOS_PROIBIDOS.contains(secret)) {
            throw new IllegalStateException(
                    "app.resource-id.secret está usando um valor de exemplo. Defina RESOURCE_ID_SECRET com um segredo próprio.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            this.macPrototype = mac;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao inicializar HMAC.", e);
        }
    }

    public String encode(Kind kind, long id) { return sign(kind, id); }
    public long decode(Kind kind, String token) { return verify(kind, token, true); }

    /**
     * Decodifica exigindo token {@code s2.*} assinado.
     * Rejeita ID numérico puro — uso obrigatório em endpoints públicos (anti-enumeração).
     */
    public long decodeAssinado(Kind kind, String token) { return verify(kind, token, false); }

    public String encodeEventoId(long id) { return encode(Kind.EVT, id); }
    public long decodeEventoId(String token) { return decode(Kind.EVT, token); }
    public long decodeEventoIdAssinado(String token) { return decodeAssinado(Kind.EVT, token); }
    public String encodeItemId(long id) { return encode(Kind.ITM, id); }
    public long decodeItemId(String token) { return decode(Kind.ITM, token); }
    public long decodeItemIdAssinado(String token) { return decodeAssinado(Kind.ITM, token); }
    public String encodeClaimId(long id) { return encode(Kind.CLM, id); }
    public long decodeClaimId(String token) { return decode(Kind.CLM, token); }
    public long decodeClaimIdAssinado(String token) { return decodeAssinado(Kind.CLM, token); }
    public String encodeCategoriaId(long id) { return encode(Kind.CAT, id); }
    public long decodeCategoriaId(String token) { return decode(Kind.CAT, token); }
    public String encodeUsuarioId(long id) { return encode(Kind.USR, id); }
    public long decodeUsuarioId(String token) { return decode(Kind.USR, token); }
    public String encodeStatusId(long id) { return encode(Kind.STA, id); }
    public long decodeStatusId(String token) { return decode(Kind.STA, token); }
    public String encodeLocalizacaoId(long id) { return encode(Kind.LOC, id); }
    public long decodeLocalizacaoId(String token) { return decode(Kind.LOC, token); }
    public String encodeDevolucaoId(long id) { return encode(Kind.DEV, id); }
    public long decodeDevolucaoId(String token) { return decode(Kind.DEV, token); }
    public String encodeCriancaId(long id) { return encode(Kind.CRI, id); }
    public long decodeCriancaId(String token) { return decode(Kind.CRI, token); }
    public String encodeCriancaResponsavelId(long id) { return encode(Kind.CRR, id); }
    public long decodeCriancaResponsavelId(String token) { return decode(Kind.CRR, token); }
    public String encodeArquivoId(long id) { return encode(Kind.ARQ, id); }
    public long decodeArquivoId(String token) { return decode(Kind.ARQ, token); }
    public long decodeArquivoIdAssinado(String token) { return decodeAssinado(Kind.ARQ, token); }
    public String encodeMovimentacaoId(long id) { return encode(Kind.MOV, id); }
    public long decodeMovimentacaoId(String token) { return decode(Kind.MOV, token); }
    public String encodeSlaId(long id) { return encode(Kind.SLA, id); }
    public long decodeSlaId(String token) { return decode(Kind.SLA, token); }
    public String encodeCategoriaCampoId(long id) { return encode(Kind.CAC, id); }
    public long decodeCategoriaCampoId(String token) { return decode(Kind.CAC, token); }
    public String encodeItemCampoId(long id) { return encode(Kind.ICC, id); }
    public long decodeItemCampoId(String token) { return decode(Kind.ICC, token); }
    public String encodeAuditoriaId(long id) { return encode(Kind.AUD, id); }
    public long decodeAuditoriaId(String token) { return decode(Kind.AUD, token); }
    public String encodeClaimValidacaoId(long id) { return encode(Kind.CLV, id); }
    public long decodeClaimValidacaoId(String token) { return decode(Kind.CLV, token); }
    public String encodeContatoId(long id) { return encode(Kind.CTO, id); }
    public long decodeContatoId(String token) { return decode(Kind.CTO, token); }
    public String encodeLacreId(long id) { return encode(Kind.LCR, id); }
    public long decodeLacreId(String token) { return decode(Kind.LCR, token); }
    public String encodeSlaRegraId(long id) { return encode(Kind.SLG, id); }
    public long decodeSlaRegraId(String token) { return decode(Kind.SLG, token); }
    public String encodeItemHistoricoId(long id) { return encode(Kind.IHI, id); }
    public long decodeItemHistoricoId(String token) { return decode(Kind.IHI, token); }
    public String encodeLocalId(long id) { return encode(Kind.LCL, id); }
    public long decodeLocalId(String token) { return decode(Kind.LCL, token); }
    public String encodeEquipeId(long id) { return encode(Kind.EQP, id); }
    public long decodeEquipeId(String token) { return decode(Kind.EQP, token); }
    public String encodeEquipeUsuarioId(long id) { return encode(Kind.EQM, id); }
    public long decodeEquipeUsuarioId(String token) { return decode(Kind.EQM, token); }
    public String encodeTriagemId(long id) { return encode(Kind.TRG, id); }
    public long decodeTriagemId(String token) { return decode(Kind.TRG, token); }
    public String encodeEtiquetaId(long id) { return encode(Kind.ETQ, id); }
    public long decodeEtiquetaId(String token) { return decode(Kind.ETQ, token); }
    public String encodePerfilId(long id) { return encode(Kind.PRF, id); }
    public long decodePerfilId(String token) { return decode(Kind.PRF, token); }

    public long decodeEntidadeId(String tpEntidade, String token) {
        return switch (tpEntidade.trim().toUpperCase()) {
            case "ITEM" -> decodeItemId(token);
            case "CLAIM" -> decodeClaimId(token);
            case "CLAIM_MENSAGEM" -> decodeClaimMensagemId(token);
            case "DEVOLUCAO" -> decodeDevolucaoId(token);
            case "CRIANCA" -> decodeCriancaId(token);
            case "EVENTO" -> decodeEventoId(token);
            case "CONTATO" -> decodeContatoId(token);
            case "CATEGORIA" -> decodeCategoriaId(token);
            default -> decodeNumeric(token);
        };
    }

    public String encodeEntidadeId(String tpEntidade, long id) {
        return switch (tpEntidade.trim().toUpperCase()) {
            case "ITEM" -> encodeItemId(id);
            case "CLAIM" -> encodeClaimId(id);
            case "CLAIM_MENSAGEM" -> encodeClaimMensagemId(id);
            case "DEVOLUCAO" -> encodeDevolucaoId(id);
            case "CRIANCA" -> encodeCriancaId(id);
            case "EVENTO" -> encodeEventoId(id);
            case "CONTATO" -> encodeContatoId(id);
            case "CATEGORIA" -> encodeCategoriaId(id);
            default -> String.valueOf(id);
        };
    }

    private long decodeNumeric(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Token de ID não informado.");
        String value = token.trim();
        if (value.chars().allMatch(Character::isDigit)) return Long.parseLong(value);
        throw new IllegalArgumentException("Token de ID inválido.");
    }

    private String sign(Kind kind, long id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido: " + id);
        String payload = "1|" + kind.name() + "|" + id;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] sig = cloneMac().doFinal(payloadBytes);
        return TOKEN_PREFIX + "." + B64_ENC.encodeToString(payloadBytes) + "." + B64_ENC.encodeToString(sig);
    }

    private long verify(Kind expectedKind, String token, boolean allowPlainNumeric) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Token de ID não informado.");
        String value = token.trim();
        if (value.chars().allMatch(Character::isDigit)) {
            if (!allowPlainNumeric) {
                throw new IllegalArgumentException("Token de ID inválido.");
            }
            return Long.parseLong(value);
        }
        String[] parts = value.split("\\.");
        if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Token de ID inválido.");
        }
        byte[] payloadBytes;
        byte[] signature;
        try {
            payloadBytes = B64_DEC.decode(parts[1]);
            signature = B64_DEC.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Token de ID inválido (Base64).");
        }
        byte[] expectedSig = cloneMac().doFinal(payloadBytes);
        if (!MessageDigest.isEqual(expectedSig, signature)) {
            throw new IllegalArgumentException("Token de ID adulterado.");
        }
        String[] segs = new String(payloadBytes, StandardCharsets.UTF_8).split("\\|", 3);
        if (segs.length != 3 || !"1".equals(segs[0])) throw new IllegalArgumentException("Payload inválido.");
        Kind kind = Kind.valueOf(segs[1]);
        if (kind != expectedKind) throw new IllegalArgumentException("Tipo de token incompatível.");
        long id = Long.parseLong(segs[2]);
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        return id;
    }

    private Mac cloneMac() {
        try {
            return (Mac) macPrototype.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Mac não clonável.", e);
        }
    }

    public String encodeTransferenciaId(long id) { return encode(Kind.TSF, id); }
    public long decodeTransferenciaId(String token) { return decode(Kind.TSF, token); }

    public String encodeMarcaId(long id) { return encode(Kind.MRC, id); }
    public long decodeMarcaId(String token) { return decode(Kind.MRC, token); }
    public String encodeModeloId(long id) { return encode(Kind.MOD, id); }
    public long decodeModeloId(String token) { return decode(Kind.MOD, token); }
    public String encodeCorId(long id) { return encode(Kind.COR, id); }
    public long decodeCorId(String token) { return decode(Kind.COR, token); }
    public String encodeTagId(long id) { return encode(Kind.TAG, id); }
    public long decodeTagId(String token) { return decode(Kind.TAG, token); }
    public String encodeEstadoId(long id) { return encode(Kind.ESD, id); }
    public long decodeEstadoId(String token) { return decode(Kind.ESD, token); }
    public String encodeEnderecoId(long id) { return encode(Kind.EDR, id); }
    public long decodeEnderecoId(String token) { return decode(Kind.EDR, token); }
    public String encodeClaimMensagemId(long id) { return encode(Kind.CMS, id); }
    public long decodeClaimMensagemId(String token) { return decode(Kind.CMS, token); }
    public String encodeAuthEventId(long id) { return encode(Kind.AEV, id); }
    public long decodeAuthEventId(String token) { return decode(Kind.AEV, token); }

    public String encodeDevolucaoHistoricoId(long id) { return encode(Kind.DEVHIST, id); }
    public long decodeDevolucaoHistoricoId(String token) { return decode(Kind.DEVHIST, token); }
    public String encodeDevolucaoPickupOpcaoId(long id) { return encode(Kind.DEVPICKUP, id); }
    public long decodeDevolucaoPickupOpcaoId(String token) { return decode(Kind.DEVPICKUP, token); }

    public enum Kind {
        EMP, USR, EVT, CAT, ITM, CLM, DEP, STA, LOC, DEV, CRI, CRR, ARQ, MOV, SLA, CAC, ICC, AUD, CLV, CTO, LCR, SLG,
        IHI, LCL, EQP, EQM, TRG, ETQ, PRF, TSF, MRC, MOD, COR, TAG, ESD, EDR, CMS, AEV, DEVHIST, DEVPICKUP
    }
}
