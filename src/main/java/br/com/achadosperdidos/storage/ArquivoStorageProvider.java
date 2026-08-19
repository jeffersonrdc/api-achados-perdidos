package br.com.achadosperdidos.storage;

/** Provedores físicos de armazenamento de arquivos. */
public enum ArquivoStorageProvider {
    LOCAL,
    S3;

    public static ArquivoStorageProvider from(String raw) {
        if (raw == null || raw.isBlank()) return S3;
        return switch (raw.trim().toUpperCase()) {
            case "S3", "AWS", "AWS_S3" -> S3;
            case "LOCAL", "DISK", "FILESYSTEM" -> LOCAL;
            default -> throw new IllegalArgumentException(
                    "Provedor de armazenamento inválido: " + raw + ". Use LOCAL ou S3.");
        };
    }
}
