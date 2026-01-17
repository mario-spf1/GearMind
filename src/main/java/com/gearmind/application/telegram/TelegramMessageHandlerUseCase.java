package com.gearmind.application.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.telegram.*;
import com.gearmind.infrastructure.telegram.TelegramBotClient;
import com.gearmind.infrastructure.telegram.TelegramConfig;
import com.gearmind.infrastructure.telegram.dto.TelegramUpdate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TelegramMessageHandlerUseCase {

    private static final int DEFAULT_LIST_LIMIT = 3;

    private static final String OPTION_CITA = "Solicitar cita";
    private static final String OPTION_ESTADO = "Estado de reparaciones";
    private static final String OPTION_CITAS = "Próximas citas";
    private static final String OPTION_FACTURAS = "Facturas recientes";
    private static final String OPTION_IDENTIFICAR = "Identificarme";
    private static final String OPTION_CAMBIAR = "Cambiar cliente";
    private static final String OPTION_CANCELAR = "Cancelar";
    private static final String OPTION_SI = "Sí";
    private static final String OPTION_NO = "No";
    private static final String OPTION_REINTENTAR = "Reintentar";
    private static final String PAYLOAD_PENDING_ACTION = "pendingAction";
    private static final String PAYLOAD_CANDIDATE_ID = "candidateClienteId";
    private static final String PAYLOAD_CANDIDATE_NOMBRE = "candidateNombre";
    private final TelegramConfig config;
    private final TelegramBotClient botClient;
    private final TelegramClientLinkRepository clientLinkRepository;
    private final TelegramConversationRepository conversationRepository;
    private final TelegramAppointmentRequestRepository appointmentRequestRepository;
    private final TelegramQueryRepository queryRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public TelegramMessageHandlerUseCase(TelegramConfig config, TelegramBotClient botClient, TelegramClientLinkRepository clientLinkRepository,
            TelegramConversationRepository conversationRepository, TelegramAppointmentRequestRepository appointmentRequestRepository,
            TelegramQueryRepository queryRepository, CustomerRepository customerRepository, ObjectMapper objectMapper) {
        this.config = config;
        this.botClient = botClient;
        this.clientLinkRepository = clientLinkRepository;
        this.conversationRepository = conversationRepository;
        this.appointmentRequestRepository = appointmentRequestRepository;
        this.queryRepository = queryRepository;
        this.customerRepository = customerRepository;
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

        String mapped = mapMenuSelection(text);
        if (mapped != null) {
            text = mapped;
        }

        Optional<TelegramConversationState> activeState = conversationRepository.findConversationByChatId(config.getEmpresaId(), chatId);

        if (text.equals("/start") || text.equals("/ayuda")) {
            sendHelp(chatId);
            if (clientLinkRepository.findByChatId(config.getEmpresaId(), chatId).isEmpty()) {
                startIdentityConversation(chatId, null);
            }
            return;
        }

        if (text.startsWith("/cancelar") || text.equalsIgnoreCase(OPTION_CANCELAR)) {
            activeState.ifPresent(state -> conversationRepository.delete(state.getId()));
            botClient.sendMessageWithKeyboard(chatId, "Conversación cancelada. Usa el menú para continuar.", buildMainMenu(), true);
            return;
        }

        if (activeState.isPresent() && !text.startsWith("/")) {
            handleConversationStep(activeState.get(), update, text);
            return;
        }

        if (text.startsWith("/identificar") || text.startsWith("/cambiar")) {
            startIdentityConversation(chatId, null);
            return;
        }

        if (text.startsWith("/cita")) {
            if (!ensureLinked(chatId, "/cita")) {
                return;
            }
            startAppointmentConversation(chatId);
            return;
        }

        if (text.startsWith("/estado")) {
            if (!ensureLinked(chatId, "/estado")) {
                return;
            }
            sendRepairStatus(chatId);
            return;
        }

        if (text.startsWith("/citas")) {
            if (!ensureLinked(chatId, "/citas")) {
                return;
            }
            sendUpcomingAppointments(chatId);
            return;
        }

        if (text.startsWith("/facturas")) {
            if (!ensureLinked(chatId, "/facturas")) {
                return;
            }
            sendInvoices(chatId);
            return;
        }

        botClient.sendMessage(chatId, "No te he entendido. Usa /ayuda para ver opciones.");
    }

    private boolean ensureLinked(long chatId, String pendingAction) {
        if (clientLinkRepository.findByChatId(config.getEmpresaId(), chatId).isPresent()) {
            return true;
        }

        startIdentityConversation(chatId, pendingAction);
        return false;
    }

    private void startIdentityConversation(long chatId, String pendingAction) {
        Map<String, String> payload = new HashMap<>();
        if (pendingAction != null && !pendingAction.isBlank()) {
            payload.put(PAYLOAD_PENDING_ACTION, pendingAction);
        }

        botClient.sendMessage(chatId, "No te he entendido. Usa /ayuda para ver opciones.");
        TelegramConversationState state = new TelegramConversationState(null, config.getEmpresaId(), chatId, TelegramConversationStep.ASK_DNI, toJson(payload), LocalDateTime.now());
        conversationRepository.save(state);
        botClient.sendMessageWithKeyboard(chatId, "Para continuar, indícanos tu DNI.", List.of(List.of(OPTION_CANCELAR)), true);
    }

    private void startAppointmentConversation(long chatId) {
        TelegramConversationState state = new TelegramConversationState(null, config.getEmpresaId(), chatId, TelegramConversationStep.ASK_CONTACT, "{}", LocalDateTime.now());
        conversationRepository.save(state);
        botClient.sendMessageWithKeyboard(chatId, "Perfecto. ¿Cuál es tu nombre y un teléfono de contacto?", List.of(List.of(OPTION_CANCELAR)), true);
    }

    private void handleConversationStep(TelegramConversationState state, TelegramUpdate update, String text) {
        Map<String, String> payload = parsePayload(state.getPayload());
        TelegramConversationStep nextStep;

        switch (state.getStep()) {
            case ASK_DNI -> {
                if (OPTION_REINTENTAR.equalsIgnoreCase(text)) {
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Indícanos tu DNI para continuar.", List.of(List.of(OPTION_CANCELAR)), true);
                    return;
                }
                String dni = normalizeDni(text);
                Optional<Customer> customer = customerRepository.findByDni(config.getEmpresaId(), dni);
                if (customer.isEmpty()) {
                    botClient.sendMessageWithKeyboard(state.getChatId(), "No encontramos ese DNI. ¿Quieres intentarlo de nuevo?", List.of(List.of(OPTION_REINTENTAR, OPTION_CANCELAR)), true);
                    return;
                }

                payload.put(PAYLOAD_CANDIDATE_ID, String.valueOf(customer.get().getId()));
                payload.put(PAYLOAD_CANDIDATE_NOMBRE, customer.get().getNombre());
                nextStep = TelegramConversationStep.CONFIRM_IDENTITY;
                botClient.sendMessageWithKeyboard(state.getChatId(), "¿Eres " + customer.get().getNombre() + "?", List.of(List.of(OPTION_SI, OPTION_NO)), true);
            }
            case CONFIRM_IDENTITY -> {
                if (isYes(text)) {
                    linkCustomer(update, state.getChatId(), payload);
                    conversationRepository.delete(state.getId());
                    String pendingAction = payload.get(PAYLOAD_PENDING_ACTION);
                    if (pendingAction != null) {
                        handlePendingAction(state.getChatId(), pendingAction);
                    } else {
                        botClient.sendMessageWithKeyboard(state.getChatId(), "Identidad confirmada. Puedes continuar con el menú.", buildMainMenu(), true);
                    }
                    return;
                }
                if (isNo(text)) {
                    payload.remove(PAYLOAD_CANDIDATE_ID);
                    payload.remove(PAYLOAD_CANDIDATE_NOMBRE);
                    TelegramConversationState updated = new TelegramConversationState(state.getId(), state.getEmpresaId(), state.getChatId(), TelegramConversationStep.ASK_DNI, toJson(payload), LocalDateTime.now());
                    conversationRepository.save(updated);
                    botClient.sendMessageWithKeyboard(state.getChatId(), "De acuerdo. Indícanos tu DNI.", List.of(List.of(OPTION_CANCELAR)), true);
                    return;
                }

                botClient.sendMessageWithKeyboard(state.getChatId(), "Responde con las opciones disponibles.", List.of(List.of(OPTION_SI, OPTION_NO)), true);
                return;
            }

            case ASK_CONTACT -> {
                payload.put("contacto", text);
                nextStep = TelegramConversationStep.ASK_AVAILABILITY;
                botClient.sendMessageWithKeyboard(state.getChatId(), "Genial. Indica tu disponibilidad (fechas y horas aproximadas).", List.of(List.of(OPTION_CANCELAR)), true);
            }
            case ASK_AVAILABILITY -> {
                payload.put("disponibilidad", text);
                nextStep = TelegramConversationStep.ASK_VEHICLE;
                botClient.sendMessageWithKeyboard(state.getChatId(), "Gracias. Indica vehículo (matrícula, marca y modelo).", List.of(List.of(OPTION_CANCELAR)), true);
            }
            case ASK_VEHICLE -> {
                payload.put("vehiculo", text);
                persistAppointmentRequest(update, payload);
                conversationRepository.delete(state.getId());
                botClient.sendMessageWithKeyboard(state.getChatId(), "Solicitud de cita registrada. Te contactaremos para confirmarla.", buildMainMenu(), true);
                return;
            }
            default -> {
                conversationRepository.delete(state.getId());
                botClient.sendMessageWithKeyboard(state.getChatId(), "Conversación reiniciada. Usa el menú para continuar.", buildMainMenu(), true);
                return;
            }
        }

        TelegramConversationState updated = new TelegramConversationState(state.getId(), state.getEmpresaId(), state.getChatId(), nextStep, toJson(payload), LocalDateTime.now());
        conversationRepository.save(updated);
    }

    private void linkCustomer(TelegramUpdate update, long chatId, Map<String, String> payload) {
        String candidateId = payload.get(PAYLOAD_CANDIDATE_ID);
        if (candidateId == null) {
            botClient.sendMessageWithKeyboard(chatId, "No se pudo confirmar tu identidad. Intenta de nuevo.", List.of(List.of(OPTION_CANCELAR)), true);
            return;
        }

        long clienteId = Long.parseLong(candidateId);
        String username = update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUsername() : null;
        clientLinkRepository.saveLink(config.getEmpresaId(), clienteId, chatId, username);
    }

    private void handlePendingAction(long chatId, String pendingAction) {
        switch (pendingAction) {
            case "/cita" ->
                startAppointmentConversation(chatId);
            case "/estado" ->
                sendRepairStatus(chatId);
            case "/citas" ->
                sendUpcomingAppointments(chatId);
            case "/facturas" ->
                sendInvoices(chatId);
            default ->
                botClient.sendMessageWithKeyboard(chatId, "Identidad confirmada. Usa el menú para continuar.", buildMainMenu(), true);
        }
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

        Long clienteId = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId).map(TelegramClientLink::getClienteId).orElse(null);
        TelegramAppointmentRequest request = new TelegramAppointmentRequest(null, config.getEmpresaId(), clienteId, null, chatId, sb.toString(), LocalDateTime.now(), TelegramAppointmentRequestStatus.PENDIENTE);
        appointmentRequestRepository.save(request);
    }

    private void sendRepairStatus(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tenemos tu cuenta vinculada. Usa \"Identificarme\" para asociar tu DNI.", buildMainMenu(), true);
            return;
        }

        List<TelegramRepairSummary> repairs = queryRepository.findRecentRepairs(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (repairs.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No hay reparaciones registradas en este momento.", buildMainMenu(), true);
            return;
        }

        StringBuilder sb = new StringBuilder("Estado de reparaciones:\n");
        for (TelegramRepairSummary repair : repairs) {
            sb.append("• #").append(repair.getId()).append(" - ").append(repair.getDescripcion()).append(" (").append(repair.getEstado()).append(")\n");
        }
        botClient.sendMessageWithKeyboard(chatId, sb.toString().trim(), buildMainMenu(), true);
    }

    private void sendUpcomingAppointments(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tenemos tu cuenta vinculada. Usa \"Identificarme\" para asociar tu DNI.", buildMainMenu(), true);
            return;
        }

        List<TelegramAppointmentSummary> appointments = queryRepository.findUpcomingAppointments(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (appointments.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tienes citas próximas registradas.", buildMainMenu(), true);
            return;
        }

        StringBuilder sb = new StringBuilder("Próximas citas:\n");
        for (TelegramAppointmentSummary appointment : appointments) {
            sb.append("• #").append(appointment.getId()).append(" - ").append(appointment.getFechaHora()).append(" (").append(appointment.getEstado()).append(")\n");
        }
        botClient.sendMessageWithKeyboard(chatId, sb.toString().trim(), buildMainMenu(), true);
    }

    private void sendInvoices(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tenemos tu cuenta vinculada. Usa \"Identificarme\" para asociar tu DNI.", buildMainMenu(), true);
            return;
        }

        List<TelegramInvoiceSummary> invoices = queryRepository.findRecentInvoices(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (invoices.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No hay facturas disponibles.", buildMainMenu(), true);
            return;
        }

        StringBuilder sb = new StringBuilder("Facturas recientes:\n");
        for (TelegramInvoiceSummary invoice : invoices) {
            sb.append("• ").append(invoice.getNumero() != null ? invoice.getNumero() : ("#" + invoice.getId())).append(" - ").append(invoice.getFecha()).append(" (").append(invoice.getEstado()).append(") ").append("Total: ").append(invoice.getTotal()).append("\n");
        }
        botClient.sendMessageWithKeyboard(chatId, sb.toString().trim(), buildMainMenu(), true);
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

    private String normalizeDni(String text) {
        return text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isYes(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return false;
        }
        return normalized.equalsIgnoreCase(OPTION_SI)
                || normalized.equalsIgnoreCase("SI")
                || normalized.equalsIgnoreCase("SÍ");
    }

    private boolean isNo(String text) {
        String normalized = normalize(text);
        return normalized != null && normalized.equalsIgnoreCase(OPTION_NO);
    }

    private String mapMenuSelection(String text) {
        return switch (text) {
            case OPTION_CITA ->
                "/cita";
            case OPTION_ESTADO ->
                "/estado";
            case OPTION_CITAS ->
                "/citas";
            case OPTION_FACTURAS ->
                "/facturas";
            case OPTION_IDENTIFICAR ->
                "/identificar";
            case OPTION_CAMBIAR ->
                "/cambiar";
            default ->
                null;
        };
    }

    private List<List<String>> buildMainMenu() {
        return List.of(
                List.of(OPTION_CITA, OPTION_ESTADO),
                List.of(OPTION_CITAS, OPTION_FACTURAS),
                List.of(OPTION_IDENTIFICAR, OPTION_CAMBIAR)
        );
    }

    private void sendHelp(long chatId) {
        String message = """
            👋 Hola, soy el bot de GearMind.
            Puedes usar los botones para solicitar una cita, consultar estado o vincular tu cuenta.
            """.trim();  
        botClient.sendMessageWithKeyboard(chatId, message, buildMainMenu(), true);          
    }
}
