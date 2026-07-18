package br.com.achadosperdidos.controller.dto;

/** Configuração administrativa do armazenamento (sem segredos). */
public record ArmazenamentoConfigResponse(
        String provider,
        boolean s3Configurado,
        String s3Bucket,
        String s3Region,
        String s3Prefix,
        boolean s3EndpointCustomizado,
        String diretorioLocal,
        String aviso
) {}
