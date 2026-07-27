package br.com.achadosperdidos.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de status do ticket de devolução (PICKUP / SHIPPING / presencial).
 */
public final class DevolucaoStatusMachine {

    public static final String CREATED = "CREATED";
    public static final String DELIVERY_METHOD_PENDING = "DELIVERY_METHOD_PENDING";
    public static final String PICKUP_SELECTED = "PICKUP_SELECTED";
    public static final String SHIPPING_SELECTED = "SHIPPING_SELECTED";
    public static final String PICKUP_SCHEDULE_REQUESTED = "PICKUP_SCHEDULE_REQUESTED";
    public static final String PICKUP_OPTIONS_PREPARED = "PICKUP_OPTIONS_PREPARED";
    public static final String PICKUP_OPTIONS_SENT = "PICKUP_OPTIONS_SENT";
    public static final String PICKUP_CONFIRMATION_PENDING = "PICKUP_CONFIRMATION_PENDING";
    public static final String PICKUP_OPTIONS_EXPIRED = "PICKUP_OPTIONS_EXPIRED";
    public static final String PICKUP_SCHEDULE_CONFIRMED = "PICKUP_SCHEDULE_CONFIRMED";
    public static final String READY_FOR_PICKUP = "READY_FOR_PICKUP";
    public static final String EM_CONFERENCIA = "EM_CONFERENCIA";
    public static final String TERMO_GERADO = "TERMO_GERADO";
    public static final String COMPLETED = "COMPLETED";
    public static final String SHIPPING_ADDRESS_PENDING = "SHIPPING_ADDRESS_PENDING";
    public static final String SHIPPING_QUOTE_PENDING = "SHIPPING_QUOTE_PENDING";
    public static final String SHIPPING_QUOTE_SENT = "SHIPPING_QUOTE_SENT";
    public static final String PAYMENT_PROOF_PENDING = "PAYMENT_PROOF_PENDING";
    public static final String PAID_AWAITING_POSTING = "PAID_AWAITING_POSTING";
    public static final String POSTED = "POSTED";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";

    public static final String AGUARDANDO_RETIRADA = "AGUARDANDO_RETIRADA";
    public static final String CONCLUIDO = "CONCLUIDO";
    public static final String ASSINADO = "ASSINADO";
    public static final String AGUARDANDO_ASSINATURA = "AGUARDANDO_ASSINATURA";

    public static final String ACTION_PREPARE_PICKUP_OPTIONS = "PREPARE_PICKUP_OPTIONS";
    public static final String ACTION_SEND_PICKUP_OPTIONS = "SEND_PICKUP_OPTIONS";
    public static final String ACTION_REGISTER_QUOTE = "REGISTER_QUOTE";
    public static final String ACTION_SEND_QUOTE = "SEND_QUOTE";
    public static final String ACTION_REGISTER_POSTING = "REGISTER_POSTING";
    public static final String ACTION_SEND_POSTING = "SEND_POSTING";
    public static final String ACTION_UPLOAD_TERMO = "UPLOAD_TERMO";
    public static final String ACTION_CONCLUIR_PRESENCIAL = "CONCLUIR_PRESENCIAL";
    public static final String ACTION_RESEND_EMAIL = "RESEND_EMAIL";
    public static final String ACTION_UPDATE_STATUS = "UPDATE_STATUS";

    private static final Map<String, Set<String>> TRANSITIONS = new LinkedHashMap<>();

    static {
        TRANSITIONS.put(CREATED, Set.of(DELIVERY_METHOD_PENDING, CANCELLED));
        TRANSITIONS.put(DELIVERY_METHOD_PENDING, Set.of(PICKUP_SELECTED, SHIPPING_SELECTED, CANCELLED));
        TRANSITIONS.put(PICKUP_SELECTED, Set.of(
                PICKUP_SCHEDULE_REQUESTED, PICKUP_OPTIONS_PREPARED, PICKUP_OPTIONS_SENT,
                PICKUP_CONFIRMATION_PENDING, READY_FOR_PICKUP, CANCELLED));
        TRANSITIONS.put(PICKUP_SCHEDULE_REQUESTED, Set.of(
                PICKUP_OPTIONS_PREPARED, PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING, CANCELLED));
        TRANSITIONS.put(PICKUP_OPTIONS_PREPARED, Set.of(
                PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING, PICKUP_OPTIONS_EXPIRED, CANCELLED));
        TRANSITIONS.put(PICKUP_OPTIONS_SENT, Set.of(
                PICKUP_CONFIRMATION_PENDING, PICKUP_SCHEDULE_CONFIRMED, READY_FOR_PICKUP,
                PICKUP_OPTIONS_EXPIRED, PICKUP_OPTIONS_PREPARED, CANCELLED));
        TRANSITIONS.put(PICKUP_CONFIRMATION_PENDING, Set.of(
                PICKUP_SCHEDULE_CONFIRMED, READY_FOR_PICKUP, PICKUP_OPTIONS_EXPIRED,
                PICKUP_OPTIONS_PREPARED, PICKUP_OPTIONS_SENT, CANCELLED));
        TRANSITIONS.put(PICKUP_OPTIONS_EXPIRED, Set.of(
                PICKUP_OPTIONS_PREPARED, PICKUP_OPTIONS_SENT, PICKUP_SCHEDULE_REQUESTED, CANCELLED));
        TRANSITIONS.put(PICKUP_SCHEDULE_CONFIRMED, Set.of(READY_FOR_PICKUP, EM_CONFERENCIA, CANCELLED));
        TRANSITIONS.put(READY_FOR_PICKUP, Set.of(EM_CONFERENCIA, TERMO_GERADO, COMPLETED, CONCLUIDO, CANCELLED));
        TRANSITIONS.put(EM_CONFERENCIA, Set.of(TERMO_GERADO, COMPLETED, CONCLUIDO, AGUARDANDO_ASSINATURA, CANCELLED));
        TRANSITIONS.put(TERMO_GERADO, Set.of(COMPLETED, CONCLUIDO, CANCELLED));
        TRANSITIONS.put(SHIPPING_SELECTED, Set.of(SHIPPING_ADDRESS_PENDING, CANCELLED));
        TRANSITIONS.put(SHIPPING_ADDRESS_PENDING, Set.of(SHIPPING_QUOTE_PENDING, CANCELLED));
        TRANSITIONS.put(SHIPPING_QUOTE_PENDING, Set.of(
                SHIPPING_QUOTE_SENT, PAYMENT_PROOF_PENDING, CANCELLED));
        TRANSITIONS.put(SHIPPING_QUOTE_SENT, Set.of(PAYMENT_PROOF_PENDING, SHIPPING_QUOTE_PENDING, CANCELLED));
        TRANSITIONS.put(PAYMENT_PROOF_PENDING, Set.of(PAID_AWAITING_POSTING, SHIPPING_QUOTE_PENDING, CANCELLED));
        TRANSITIONS.put(PAID_AWAITING_POSTING, Set.of(POSTED, CANCELLED));
        TRANSITIONS.put(POSTED, Set.of(IN_TRANSIT, DELIVERED, COMPLETED, CANCELLED));
        TRANSITIONS.put(IN_TRANSIT, Set.of(DELIVERED, COMPLETED, CANCELLED));
        TRANSITIONS.put(DELIVERED, Set.of(COMPLETED, CANCELLED));
        TRANSITIONS.put(AGUARDANDO_RETIRADA, Set.of(
                EM_CONFERENCIA, AGUARDANDO_ASSINATURA, ASSINADO, TERMO_GERADO, CONCLUIDO, COMPLETED, CANCELLED));
        TRANSITIONS.put(AGUARDANDO_ASSINATURA, Set.of(ASSINADO, TERMO_GERADO, CONCLUIDO, COMPLETED, CANCELLED));
        TRANSITIONS.put(ASSINADO, Set.of(CONCLUIDO, COMPLETED, TERMO_GERADO, CANCELLED));
        TRANSITIONS.put(CONCLUIDO, Set.of());
        TRANSITIONS.put(COMPLETED, Set.of());
        TRANSITIONS.put(CANCELLED, Set.of());
    }

    private DevolucaoStatusMachine() {}

    public static String normalize(String status) {
        if (status == null || status.isBlank()) return status;
        String s = status.trim().toUpperCase();
        if (CONCLUIDO.equals(s)) return COMPLETED;
        return s;
    }

    public static void assertCanTransition(String from, String to) {
        String origem = from == null ? "" : from.trim().toUpperCase();
        String destino = to == null ? "" : to.trim().toUpperCase();
        if (origem.equals(destino)) return;
        Set<String> allowed = TRANSITIONS.get(origem);
        if (allowed == null || !allowed.contains(destino)) {
            throw new IllegalArgumentException(
                    "Transição de status inválida: " + origem + " → " + destino);
        }
    }

    public static List<String> allowedActions(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        List<String> actions = new ArrayList<>();
        switch (s) {
            case PICKUP_SELECTED, PICKUP_SCHEDULE_REQUESTED, PICKUP_OPTIONS_EXPIRED -> {
                actions.add(ACTION_PREPARE_PICKUP_OPTIONS);
                actions.add(ACTION_RESEND_EMAIL);
            }
            case PICKUP_OPTIONS_PREPARED -> {
                actions.add(ACTION_PREPARE_PICKUP_OPTIONS);
                actions.add(ACTION_SEND_PICKUP_OPTIONS);
                actions.add(ACTION_RESEND_EMAIL);
            }
            case PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING -> {
                actions.add(ACTION_PREPARE_PICKUP_OPTIONS);
                actions.add(ACTION_SEND_PICKUP_OPTIONS);
                actions.add(ACTION_RESEND_EMAIL);
            }
            case PICKUP_SCHEDULE_CONFIRMED, READY_FOR_PICKUP -> {
                actions.add(ACTION_UPLOAD_TERMO);
                actions.add(ACTION_CONCLUIR_PRESENCIAL);
                actions.add(ACTION_UPDATE_STATUS);
            }
            case EM_CONFERENCIA -> {
                actions.add(ACTION_UPLOAD_TERMO);
                actions.add(ACTION_CONCLUIR_PRESENCIAL);
                actions.add(ACTION_UPDATE_STATUS);
            }
            case TERMO_GERADO, AGUARDANDO_ASSINATURA, ASSINADO -> {
                actions.add(ACTION_UPLOAD_TERMO);
                actions.add(ACTION_CONCLUIR_PRESENCIAL);
            }
            case SHIPPING_ADDRESS_PENDING, SHIPPING_QUOTE_PENDING, SHIPPING_SELECTED -> {
                actions.add(ACTION_REGISTER_QUOTE);
                actions.add(ACTION_RESEND_EMAIL);
            }
            case SHIPPING_QUOTE_SENT, PAYMENT_PROOF_PENDING -> {
                actions.add(ACTION_REGISTER_QUOTE);
                actions.add(ACTION_SEND_QUOTE);
                actions.add(ACTION_RESEND_EMAIL);
            }
            case PAID_AWAITING_POSTING -> {
                actions.add(ACTION_REGISTER_POSTING);
                actions.add(ACTION_RESEND_EMAIL);
            }
            case POSTED, IN_TRANSIT, DELIVERED -> {
                actions.add(ACTION_SEND_POSTING);
                actions.add(ACTION_RESEND_EMAIL);
                actions.add(ACTION_UPDATE_STATUS);
            }
            case AGUARDANDO_RETIRADA -> {
                actions.add(ACTION_UPLOAD_TERMO);
                actions.add(ACTION_UPDATE_STATUS);
                actions.add(ACTION_CONCLUIR_PRESENCIAL);
            }
            case DELIVERY_METHOD_PENDING, CREATED -> actions.add(ACTION_RESEND_EMAIL);
            default -> { /* sem ações extras */ }
        }
        if (!actions.contains(ACTION_UPDATE_STATUS)
                && !COMPLETED.equals(s) && !CONCLUIDO.equals(s) && !CANCELLED.equals(s)) {
            actions.add(ACTION_UPDATE_STATUS);
        }
        return List.copyOf(actions);
    }

    public static String nextAction(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        return switch (s) {
            case DELIVERY_METHOD_PENDING, CREATED -> "Aguardando solicitante escolher modalidade";
            case PICKUP_SELECTED, PICKUP_SCHEDULE_REQUESTED, PICKUP_OPTIONS_EXPIRED ->
                    "Cadastrar opções de agenda de retirada";
            case PICKUP_OPTIONS_PREPARED -> "Enviar opções de agenda por e-mail";
            case PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING ->
                    "Aguardando confirmação do agendamento";
            case PICKUP_SCHEDULE_CONFIRMED, READY_FOR_PICKUP -> "Preparar retirada presencial";
            case EM_CONFERENCIA -> "Gerar termo e concluir";
            case TERMO_GERADO, AGUARDANDO_ASSINATURA, ASSINADO -> "Concluir devolução presencial";
            case SHIPPING_SELECTED, SHIPPING_ADDRESS_PENDING -> "Aguardando endereço do solicitante";
            case SHIPPING_QUOTE_PENDING -> "Informar cotação de frete";
            case SHIPPING_QUOTE_SENT, PAYMENT_PROOF_PENDING -> "Aguardando comprovante de pagamento";
            case PAID_AWAITING_POSTING -> "Registrar postagem e rastreio";
            case POSTED, IN_TRANSIT -> "Acompanhar entrega";
            case DELIVERED -> "Concluir devolução";
            case COMPLETED, CONCLUIDO -> "Fluxo concluído";
            case CANCELLED -> "Cancelada";
            case AGUARDANDO_RETIRADA -> "Aguardando retirada presencial";
            default -> "Verificar status da devolução";
        };
    }
}
