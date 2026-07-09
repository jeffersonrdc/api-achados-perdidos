package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;

public record CriancaResponse(String id, String idEvento, String nmCrianca, LocalDate dtNascimento, String nrPulseira, String nrQrCode, String dsObservacao) {}
