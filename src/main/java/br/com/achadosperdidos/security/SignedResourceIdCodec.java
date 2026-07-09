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

    public SignedResourceIdCodec(@Value("${app.resource-id.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("Defina app.resource-id.secret com pelo menos 32 caracteres.");
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
    public long decode(Kind kind, String token) { return verify(kind, token); }

    public String encodeEventoId(long id) { return encode(Kind.EVT, id); }
    public long decodeEventoId(String token) { return decode(Kind.EVT, token); }
    public String encodeItemId(long id) { return encode(Kind.ITM, id); }
    public long decodeItemId(String token) { return decode(Kind.ITM, token); }
    public String encodeClaimId(long id) { return encode(Kind.CLM, id); }
    public long decodeClaimId(String token) { return decode(Kind.CLM, token); }
    public String encodeCategoriaId(long id) { return encode(Kind.CAT, id); }
    public long decodeCategoriaId(String token) { return decode(Kind.CAT, token); }
    public String encodeUsuarioId(long id) { return encode(Kind.USR, id); }
    public long decodeUsuarioId(String token) { return decode(Kind.USR, token); }
    public String encodeEmpresaId(long id) { return encode(Kind.EMP, id); }
    public long decodeEmpresaId(String token) { return decode(Kind.EMP, token); }
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

    public long decodeEntidadeId(String tpEntidade, String token) {
        return switch (tpEntidade.trim().toUpperCase()) {
            case "ITEM" -> decodeItemId(token);
            case "CLAIM" -> decodeClaimId(token);
            case "DEVOLUCAO" -> decodeDevolucaoId(token);
            case "CRIANCA" -> decodeCriancaId(token);
            default -> decodeNumeric(token);
        };
    }

    public String encodeEntidadeId(String tpEntidade, long id) {
        return switch (tpEntidade.trim().toUpperCase()) {
            case "ITEM" -> encodeItemId(id);
            case "CLAIM" -> encodeClaimId(id);
            case "DEVOLUCAO" -> encodeDevolucaoId(id);
            case "CRIANCA" -> encodeCriancaId(id);
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

    private long verify(Kind expectedKind, String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Token de ID não informado.");
        String value = token.trim();
        if (value.chars().allMatch(Character::isDigit)) return Long.parseLong(value);
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

    public enum Kind { EMP, USR, EVT, CAT, ITM, CLM, DEP, STA, LOC, DEV, CRI, CRR, ARQ, MOV, SLA, CAC, ICC, AUD, CLV, CTO, LCR, SLG, IHI }
}
