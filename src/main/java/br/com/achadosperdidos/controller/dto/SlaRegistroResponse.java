package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record SlaRegistroResponse(String id, String tpEntidade, String idEntidade, String stSla, LocalDateTime dtInicio, LocalDateTime dtLimite, LocalDateTime dtConclusao) {}
