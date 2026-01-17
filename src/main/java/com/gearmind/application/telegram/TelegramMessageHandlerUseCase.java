package com.gearmind.application.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.domain.telegram.*;
import com.gearmind.infrastructure.telegram.TelegramBotClient;
import com.gearmind.infrastructure.telegram.TelegramConfig;
import com.gearmind.infrastructure.telegram.dto.TelegramUpdate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TelegramMessageHandlerUseCase {

    private static final int DEFAULT_LIST_LIMIT = 3;

    private final TelegramConfig config;
    private final TelegramBotClient botClient;
    private final TelegramClientLinkRepository clientLinkRepository;
    private final TelegramConversationRepository conversationRepository;
    private final TelegramAppointmentRequestRepository appointmentRequestRepository;
    private final TelegramQueryRepository queryRepository;
    private final ObjectMapper objectMapper;

    public TelegramMessageHandlerUseCase(TelegramConfig config, TelegramBotClient botClient, TelegramClientLinkRepository clientLinkRepository, TelegramConversationRepository conversationRepository, TelegramAppointmentRequestRepository appointmentRequestRepository, TelegramQueryRepository queryRepository, ObjectMapper objectMapper) {
        this.config = config;
        this.botClient = botClient;
        this.clientLinkRepository = clientLinkRepository;
        this.conversationRepository = conversationRepository;
        this.appointmentRequestRepository = appointmentRequestRepository;
        this.queryRepository = queryRepository;
        this.objectMapper = objectMapper;
    }

    public void handle(TelegramUpdate update) {
        if (update == null || update.getMessage() == null || update.getMessage().getChat() == null) {
            return;
        }

        long chatId = update.getMessage().getChat().getId();
        String text = normalize(update.getMessage().getText());
        if (text == null) {
            return;
        }

        if (text.equals("/start") || text.equals("/ayuda")) {
            botClient.sendMessage(chatId, buildHelpMessage());
            return;
        }

        Optional<TelegramConversationState> activeState = conversationRepository.findConversationByChatId(config.getEmpresaId(), chatId);
        if (activeState.isPresent() && !text.startsWith("/")) {
            handleConversationStep(activeState.get(), update, text);
            return;
        }

        if (text.startsWith("/cita")) {
            startAppointmentConversation(chatId);
            return;
        }

        if (text.startsWith("/estado")) {
            sendRepairStatus(chatId);
            return;
        }

        if (text.startsWith("/citas")) {
            sendUpcomingAppointments(chatId);
            return;
        }

        if (text.startsWith("/facturas")) {
            sendInvoices(chatId);
            return;
        }

        if (text.startsWith("/cancelar")) {
            activeState.ifPresent(state -> conversationRepository.delete(state.getId()));
            botClient.sendMessage(chatId, "Conversación cancelada. Puedes usar /cita para empezar de nuevo.");
            return;
        }

        botClient.sendMessage(chatId, "No te he entendido. Usa /ayuda para ver opciones.");
    }

    private void startAppointmentConversation(long chatId) {
        TelegramConversationState state = new TelegramConversationState(null, config.getEmpresaId(), chatId, TelegramConversationStep.ASK_CONTACT, "{}", LocalDateTime.now());
        conversationRepository.save(state);
        botClient.sendMessage(chatId, "Perfecto. ¿Cuál es tu nombre y un teléfono de contacto?");
    }

    private void handleConversationStep(TelegramConversationState state, TelegramUpdate update, String text) {
        Map<String, String> payload = parsePayload(state.getPayload());
        TelegramConversationStep nextStep;

        switch (state.getStep()) {
            case ASK_CONTACT -> {
                payload.put("contacto", text);
                nextStep = TelegramConversationStep.ASK_AVAILABILITY;
                botClient.sendMessage(state.getChatId(), "Genial. Indica tu disponibilidad (fechas y horas aproximadas).");
            }
            case ASK_AVAILABILITY -> {
                payload.put("disponibilidad", text);
                nextStep = TelegramConversationStep.ASK_VEHICLE;
                botClient.sendMessage(state.getChatId(), "Gracias. Indica vehículo (matrícula, marca y modelo).");
            }
            case ASK_VEHICLE -> {
                payload.put("vehiculo", text);
                persistAppointmentRequest(update, payload);
                conversationRepository.delete(state.getId());
                botClient.sendMessage(state.getChatId(), "Solicitud de cita registrada. Te contactaremos para confirmarla.");
                return;
            }
            default -> {
                conversationRepository.delete(state.getId());
                botClient.sendMessage(state.getChatId(), "Conversación reiniciada. Usa /cita para comenzar.");
                return;
            }
        }

        TelegramConversationState updated = new TelegramConversationState(state.getId(), state.getEmpresaId(), state.getChatId(), nextStep, toJson(payload), LocalDateTime.now());
        conversationRepository.save(updated);
    }

    private void persistAppointmentRequest(TelegramUpdate update, Map<String, String> payload) {
        long chatId = update.getMessage().getChat().getId();
        String username = update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUsername() : null;
        StringBuilder sb = new StringBuilder();
        sb.append("Contacto: ").append(payload.getOrDefault("contacto", "N/D")).append("\n");
        sb.append("Disponibilidad: ").append(payload.getOrDefault("disponibilidad", "N/D")).append("\n");
        sb.append("Vehículo: ").append(payload.getOrDefault("vehiculo", "N/D")).append("\n");
        if (username != null && !username.isBlank()) {
            sb.append("Telegram: @").append(username);
        }

        TelegramAppointmentRequest request = new TelegramAppointmentRequest(null, config.getEmpresaId(), null, null, chatId, sb.toString(), LocalDateTime.now(), TelegramAppointmentRequestStatus.PENDIENTE);
        appointmentRequestRepository.save(request);
    }

    private void sendRepairStatus(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessage(chatId, "No tenemos tu cuenta vinculada. Contacta con el taller para asociar tu Telegram.");
            return;
        }

        List<TelegramRepairSummary> repairs = queryRepository.findRecentRepairs(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (repairs.isEmpty()) {
            botClient.sendMessage(chatId, "No hay reparaciones registradas en este momento.");
            return;
        }

        StringBuilder sb = new StringBuilder("Estado de reparaciones:\n");
        for (TelegramRepairSummary repair : repairs) {
            sb.append("• #").append(repair.getId()).append(" - ").append(repair.getDescripcion()).append(" (").append(repair.getEstado()).append(")\n");
        }
        botClient.sendMessage(chatId, sb.toString().trim());
    }

    private void sendUpcomingAppointments(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessage(chatId, "No tenemos tu cuenta vinculada. Contacta con el taller para asociar tu Telegram.");
            return;
        }

        List<TelegramAppointmentSummary> appointments = queryRepository.findUpcomingAppointments(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (appointments.isEmpty()) {
            botClient.sendMessage(chatId, "No tienes citas próximas registradas.");
            return;
        }

        StringBuilder sb = new StringBuilder("Próximas citas:\n");
        for (TelegramAppointmentSummary appointment : appointments) {
            sb.append("• #").append(appointment.getId()).append(" - ").append(appointment.getFechaHora()).append(" (").append(appointment.getEstado()).append(")\n");
        }
        botClient.sendMessage(chatId, sb.toString().trim());
    }

    private void sendInvoices(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessage(chatId, "No tenemos tu cuenta vinculada. Contacta con el taller para asociar tu Telegram.");
            return;
        }

        List<TelegramInvoiceSummary> invoices = queryRepository.findRecentInvoices(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (invoices.isEmpty()) {
            botClient.sendMessage(chatId, "No hay facturas disponibles.");
            return;
        }

        StringBuilder sb = new StringBuilder("Facturas recientes:\n");
        for (TelegramInvoiceSummary invoice : invoices) {
            sb.append("• ").append(invoice.getNumero() != null ? invoice.getNumero() : ("#" + invoice.getId())).append(" - ").append(invoice.getFecha()).append(" (").append(invoice.getEstado()).append(") ").append("Total: ").append(invoice.getTotal()).append("\n");
        }
        botClient.sendMessage(chatId, sb.toString().trim());
    }

    private Map<String, String> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String toJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildHelpMessage() {
        return """
                👋 Hola, soy el bot de GearMind.
                Comandos disponibles:
                • /cita -> Solicitar una cita
                • /estado -> Estado de reparaciones
                • /citas -> Próximas citas
                • /facturas -> Facturas recientes
                • /cancelar -> Cancelar conversación
                """.trim();
    }
}
