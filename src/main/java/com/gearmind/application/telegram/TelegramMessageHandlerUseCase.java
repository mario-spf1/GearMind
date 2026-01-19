package com.gearmind.application.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.telegram.*;
import com.gearmind.infrastructure.telegram.TelegramBotClient;
import com.gearmind.infrastructure.telegram.TelegramConfig;
import com.gearmind.infrastructure.telegram.dto.TelegramUpdate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
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
    private static final String OPTION_CAMBIAR = "Cambiar cliente";
    private static final String OPTION_CANCELAR = "Cancelar";
    private static final String OPTION_SI = "Sí";
    private static final String OPTION_NO = "No";
    private static final String OPTION_REINTENTAR = "Reintentar";
    private static final String OPTION_CONFIRMAR_CITA = "Confirmar cita";
    private static final String OPTION_CAMBIAR_DIA = "Cambiar día";
    private static final String PAYLOAD_PENDING_ACTION = "pendingAction";
    private static final String PAYLOAD_CANDIDATE_ID = "candidateClienteId";
    private static final String PAYLOAD_CANDIDATE_NOMBRE = "candidateNombre";
    private static final String PAYLOAD_APPOINTMENT_YEAR = "appointmentYear";
    private static final String PAYLOAD_APPOINTMENT_MONTH = "appointmentMonth";
    private static final String PAYLOAD_APPOINTMENT_DAY = "appointmentDay";
    private static final String PAYLOAD_APPOINTMENT_HOUR = "appointmentHour";
    private static final List<Integer> APPOINTMENT_HOURS = List.of(9, 10, 11, 12, 13, 14, 15, 16, 17, 18);
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

        if (text.startsWith("/identificar")) {
            startIdentityConversation(chatId, null, "Necesitamos identificarte para continuar.");
            return;
        }

        if (text.startsWith("/cambiar")) {
            resetLinkedCustomer(chatId);
            startIdentityConversation(chatId, null, "Vamos a cambiar de cliente. Indícanos tu DNI.");
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

        startIdentityConversation(chatId, pendingAction, "Necesitamos identificarte para continuar.");
        return false;
    }

    private void startIdentityConversation(long chatId, String pendingAction, String promptMessage) {
        Map<String, String> payload = new HashMap<>();
        if (pendingAction != null && !pendingAction.isBlank()) {
            payload.put(PAYLOAD_PENDING_ACTION, pendingAction);
        }

        TelegramConversationState state = new TelegramConversationState(null, config.getEmpresaId(), chatId, TelegramConversationStep.ASK_DNI, toJson(payload), LocalDateTime.now());
        conversationRepository.save(state);
        String prompt = promptMessage != null && !promptMessage.isBlank() ? promptMessage : "Para continuar, indícanos tu DNI.";
        botClient.sendMessageWithKeyboard(chatId, prompt, List.of(List.of(OPTION_CANCELAR)), true);
    }

    private void startAppointmentConversation(long chatId) {
        TelegramConversationState state = new TelegramConversationState(null, config.getEmpresaId(), chatId, TelegramConversationStep.ASK_DATE_YEAR, "{}", LocalDateTime.now());
        conversationRepository.save(state);
        sendYearOptions(chatId);
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

            case ASK_DATE_YEAR -> {
                Integer year = parseYear(text);
                if (year == null) {
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona un año válido.", buildYearKeyboard(), true);
                    return;
                }
                payload.put(PAYLOAD_APPOINTMENT_YEAR, String.valueOf(year));
                nextStep = TelegramConversationStep.ASK_DATE_MONTH;
                botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona el mes.", buildMonthKeyboard(year), true);
            }

            case ASK_DATE_MONTH -> {
                Integer month = parseMonth(text);
                Integer year = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_YEAR));
                if (month == null || year == null) {
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona un mes válido.", buildMonthKeyboard(year != null ? year : LocalDate.now().getYear()), true);
                    return;
                }
                payload.put(PAYLOAD_APPOINTMENT_MONTH, String.valueOf(month));
                nextStep = TelegramConversationStep.ASK_DATE_DAY;
                botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona el día.", buildDayKeyboard(year, month), true);
            }

            case ASK_DATE_DAY -> {
                Integer year = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_YEAR));
                Integer month = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_MONTH));
                Integer day = parseDay(text, year, month);
                if (year == null || month == null || day == null) {
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona un día válido.", buildDayKeyboard(year != null ? year : LocalDate.now().getYear(), month != null ? month : LocalDate.now().getMonthValue()), true);
                    return;
                }
                payload.put(PAYLOAD_APPOINTMENT_DAY, String.valueOf(day));
                List<String> hourOptions = buildAvailableHourOptions(year, month, day);
                if (hourOptions.isEmpty()) {
                    botClient.sendMessageWithKeyboard(state.getChatId(), "No hay horas disponibles para ese día. Elige otro día.", buildDayKeyboard(year, month), true);
                    return;
                }
                nextStep = TelegramConversationStep.ASK_TIME_SLOT;
                botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona una hora disponible.", buildKeyboard(hourOptions, 3), true);
            }
            case ASK_TIME_SLOT -> {
                Integer year = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_YEAR));
                Integer month = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_MONTH));
                Integer day = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_DAY));
                Integer hour = parseHour(text);
                if (year == null || month == null || day == null || hour == null) {
                    List<String> hourOptions = buildAvailableHourOptions(year != null ? year : LocalDate.now().getYear(), month != null ? month : LocalDate.now().getMonthValue(), day != null ? day : LocalDate.now().getDayOfMonth());
                    if (hourOptions.isEmpty()) {
                        botClient.sendMessageWithKeyboard(state.getChatId(), "No hay horas disponibles para ese día. Selecciona otra fecha.", buildDayKeyboard(year != null ? year : LocalDate.now().getYear(), month != null ? month : LocalDate.now().getMonthValue()), true);
                        return;
                    }
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona una hora válida.", buildKeyboard(hourOptions, 3), true);
                    return;
                }
                payload.put(PAYLOAD_APPOINTMENT_HOUR, String.valueOf(hour));
                nextStep = TelegramConversationStep.CONFIRM_APPOINTMENT;
                botClient.sendMessageWithKeyboard(state.getChatId(), appointmentConfirmationMessage(year, month, day, hour), buildConfirmAppointmentKeyboard(), true);
            }
            case CONFIRM_APPOINTMENT -> {
                if (OPTION_CONFIRMAR_CITA.equalsIgnoreCase(text)) {
                    persistAppointmentRequest(update, payload);
                    conversationRepository.delete(state.getId());
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Solicitud de cita registrada. Te contactaremos para confirmarla.", buildMainMenu(), true);
                    return;
                }
                if (OPTION_CAMBIAR_DIA.equalsIgnoreCase(text)) {
                    nextStep = TelegramConversationStep.ASK_DATE_DAY;
                    Integer year = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_YEAR));
                    Integer month = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_MONTH));
                    payload.remove(PAYLOAD_APPOINTMENT_DAY);
                    payload.remove(PAYLOAD_APPOINTMENT_HOUR);
                    botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona un nuevo día.", buildDayKeyboard(year != null ? year : LocalDate.now().getYear(), month != null ? month : LocalDate.now().getMonthValue()), true);
                }
                botClient.sendMessageWithKeyboard(state.getChatId(), "Selecciona una opción.", buildConfirmAppointmentKeyboard(), true);
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
        Integer year = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_YEAR));
        Integer month = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_MONTH));
        Integer day = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_DAY));
        Integer hour = parsePayloadInteger(payload.get(PAYLOAD_APPOINTMENT_HOUR));
        LocalDate date = (year != null && month != null && day != null) ? LocalDate.of(year, month, day) : null;
        StringBuilder sb = new StringBuilder();
        if (date != null && hour != null) {
            sb.append("Fecha solicitada: ").append(date).append(" ").append(String.format("%02d:00", hour)).append("\n");
        } else {
            sb.append("Fecha solicitada: N/D\n");
        }
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        link.flatMap(item -> customerRepository.findById(item.getClienteId()))
                .ifPresent(customer -> {
                    sb.append("Cliente: ").append(customer.getNombre()).append("\n");
                    if (customer.getTelefono() != null && !customer.getTelefono().isBlank()) {
                        sb.append("Teléfono: ").append(customer.getTelefono()).append("\n");
                    }
                    if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                        sb.append("Email: ").append(customer.getEmail()).append("\n");
                    }
                });
        sb.append("Vehículo: Por confirmar\n");
        if (username != null && !username.isBlank()) {
            sb.append("Telegram: @").append(username);
        }

        Long clienteId = link.map(TelegramClientLink::getClienteId).orElse(null);
        TelegramAppointmentRequest request = new TelegramAppointmentRequest(null, config.getEmpresaId(), clienteId, null, chatId, sb.toString(), LocalDateTime.now(), TelegramAppointmentRequestStatus.PENDIENTE);
        appointmentRequestRepository.save(request);
    }

    private void sendRepairStatus(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tenemos tu cuenta vinculada. Usa /ayuda para identificarte.", buildMainMenu(), true);
            return;
        }

        List<TelegramRepairSummary> repairs = queryRepository.findRecentRepairs(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (repairs.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No hay reparaciones registradas en este momento.", buildMainMenu(), true);
            return;
        }

        StringBuilder sb = new StringBuilder("Estado de reparaciones:\n");
        appendCustomerHeader(sb, link.get().getClienteId());
        for (TelegramRepairSummary repair : repairs) {
            sb.append("• #").append(repair.getId()).append(" - ").append(repair.getDescripcion()).append(" (").append(repair.getEstado()).append(")\n");
        }
        botClient.sendMessageWithKeyboard(chatId, sb.toString().trim(), buildMainMenu(), true);
    }

    private void sendUpcomingAppointments(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tenemos tu cuenta vinculada. Usa /ayuda para identificarte.", buildMainMenu(), true);
            return;
        }

        List<TelegramAppointmentSummary> appointments = queryRepository.findUpcomingAppointments(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (appointments.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tienes citas próximas registradas.", buildMainMenu(), true);
            return;
        }

        StringBuilder sb = new StringBuilder("Próximas citas:\n");
        appendCustomerHeader(sb, link.get().getClienteId());
        for (TelegramAppointmentSummary appointment : appointments) {
            sb.append("• #").append(appointment.getId()).append(" - ").append(appointment.getFechaHora()).append(" (").append(appointment.getEstado()).append(")\n");
        }
        botClient.sendMessageWithKeyboard(chatId, sb.toString().trim(), buildMainMenu(), true);
    }

    private void sendInvoices(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No tenemos tu cuenta vinculada. Usa /ayuda para identificarte.", buildMainMenu(), true);
            return;
        }

        List<TelegramInvoiceSummary> invoices = queryRepository.findRecentInvoices(config.getEmpresaId(), link.get().getClienteId(), DEFAULT_LIST_LIMIT);
        if (invoices.isEmpty()) {
            botClient.sendMessageWithKeyboard(chatId, "No hay facturas disponibles.", buildMainMenu(), true);
            return;
        }

        StringBuilder sb = new StringBuilder("Facturas recientes:\n");
        appendCustomerHeader(sb, link.get().getClienteId());
        for (TelegramInvoiceSummary invoice : invoices) {
            sb.append("• ").append(invoice.getNumero() != null ? invoice.getNumero() : ("#" + invoice.getId())).append(" - ").append(invoice.getTotal()).append("\n");
        }
        botClient.sendMessageWithKeyboard(chatId, sb.toString().trim(), buildMainMenu(), true);
    }

    private void sendYearOptions(long chatId) {
        String prefix = customerLabel(chatId);
        botClient.sendMessageWithKeyboard(chatId, (prefix != null ? prefix + "\n" : "") + "Selecciona el año.", buildYearKeyboard(), true);
    }

    private List<List<String>> buildYearKeyboard() {
        int currentYear = LocalDate.now().getYear();
        List<String> options = List.of(String.valueOf(currentYear), String.valueOf(currentYear + 1));
        return buildKeyboard(options, 2);
    }

    private List<List<String>> buildMonthKeyboard(Integer year) {
        int currentYear = LocalDate.now().getYear();
        int startMonth = (year != null && year == currentYear) ? LocalDate.now().getMonthValue() : 1;
        List<String> options = new java.util.ArrayList<>();
        for (int month = startMonth; month <= 12; month++) {
            options.add(monthLabel(month));
        }
        return buildKeyboard(options, 3);
    }

    private List<List<String>> buildDayKeyboard(int year, int month) {
        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.of(year, month);
        int startDay = (year == today.getYear() && month == today.getMonthValue()) ? today.getDayOfMonth() : 1;
        List<String> options = new java.util.ArrayList<>();
        for (int day = startDay; day <= yearMonth.lengthOfMonth(); day++) {
            options.add(String.valueOf(day));
        }
        return buildKeyboard(options, 7);
    }

    private List<String> buildAvailableHourOptions(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        List<Integer> bookedHours = queryRepository.findBookedAppointmentHours(config.getEmpresaId(), date);
        LocalDateTime now = LocalDateTime.now();
        List<String> options = new java.util.ArrayList<>();
        for (Integer hour : APPOINTMENT_HOURS) {
            if (bookedHours.contains(hour)) {
                continue;
            }
            if (date.equals(now.toLocalDate()) && hour <= now.getHour()) {
                continue;
            }
            options.add(String.format("%02d:00", hour));
        }
        return options;
    }

    private String appointmentConfirmationMessage(int year, int month, int day, int hour) {
        LocalDate date = LocalDate.of(year, month, day);
        return "Has seleccionado " + date + " a las " + String.format("%02d:00", hour) + ". ¿Confirmas la solicitud?";
    }

    private List<List<String>> buildConfirmAppointmentKeyboard() {
        return List.of(List.of(OPTION_CONFIRMAR_CITA), List.of(OPTION_CAMBIAR_DIA, OPTION_CANCELAR));
    }

    private List<List<String>> buildKeyboard(List<String> options, int columns) {
        List<List<String>> rows = new java.util.ArrayList<>();
        List<String> current = new java.util.ArrayList<>();
        for (String option : options) {
            current.add(option);
            if (current.size() == columns) {
                rows.add(current);
                current = new java.util.ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        rows.add(List.of(OPTION_CANCELAR));
        return rows;
    }

    private Integer parseYear(String text) {
        Integer value = parseInteger(text);
        if (value == null) {
            return null;
        }
        int currentYear = LocalDate.now().getYear();
        if (value < currentYear || value > currentYear + 1) {
            return null;
        }
        return value;
    }

    private Integer parseMonth(String text) {
        Integer numeric = parseInteger(text);
        if (numeric != null && numeric >= 1 && numeric <= 12) {
            return numeric;
        }
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toLowerCase(new Locale("es", "ES"));
        for (int month = 1; month <= 12; month++) {
            if (monthLabel(month).toLowerCase(new Locale("es", "ES")).equals(normalized)) {
                return month;
            }
        }
        return null;
    }

    private Integer parseDay(String text, Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        Integer day = parseInteger(text);
        if (day == null) {
            return null;
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        if (day < 1 || day > yearMonth.lengthOfMonth()) {
            return null;
        }
        LocalDate date = LocalDate.of(year, month, day);
        if (date.isBefore(LocalDate.now())) {
            return null;
        }
        return day;
    }

    private Integer parseHour(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.endsWith(":00")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        Integer value = parseInteger(normalized);
        if (value == null) {
            return null;
        }
        return APPOINTMENT_HOURS.contains(value) ? value : null;
    }

    private Integer parsePayloadInteger(String raw) {
        return parseInteger(raw);
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String monthLabel(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, new Locale("es", "ES"));
    }

    private void appendCustomerHeader(StringBuilder sb, long clienteId) {
        customerRepository.findById(clienteId).ifPresent(customer -> sb.append("Cliente: ").append(customer.getNombre()).append("\n"));
    }

    private String customerLabel(long chatId) {
        return clientLinkRepository.findByChatId(config.getEmpresaId(), chatId).flatMap(link -> customerRepository.findById(link.getClienteId())).map(customer -> "Cliente: " + customer.getNombre()).orElse(null);
    }

    private void resetLinkedCustomer(long chatId) {
        clientLinkRepository.deleteByChatId(config.getEmpresaId(), chatId);
        conversationRepository.findConversationByChatId(config.getEmpresaId(), chatId).ifPresent(state -> conversationRepository.delete(state.getId()));
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
                List.of(OPTION_CAMBIAR)
        );
    }

    private void sendHelp(long chatId) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByChatId(config.getEmpresaId(), chatId);
        if (link.isPresent()) {
            String name = customerRepository.findById(link.get().getClienteId())
                    .map(Customer::getNombre)
                    .orElse("cliente #" + link.get().getClienteId());
            String message = """
                👋 Hola, soy el bot de GearMind.
                Ahora mismo estás identificado como %s.
                Usa los botones para solicitar una cita o consultar tu información.
                Si no eres tú, pulsa "Cambiar cliente".
                """.formatted(name).trim();
            botClient.sendMessageWithKeyboard(chatId, message, buildMainMenu(), true);
            return;
        }
        String message = """
            Hola, soy el bot de GearMind.
            Necesitamos identificarte para continuar.
            """.trim();
        botClient.sendMessageWithKeyboard(chatId, message, buildMainMenu(), true);
        startIdentityConversation(chatId, null, "Indícanos tu DNI para identificarte.");
    }
}
