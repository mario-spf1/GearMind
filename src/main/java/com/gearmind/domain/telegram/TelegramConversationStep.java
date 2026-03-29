package com.gearmind.domain.telegram;

public enum TelegramConversationStep {
    ASK_DNI,
    CONFIRM_IDENTITY,
    ASK_DATE_YEAR,
    ASK_DATE_MONTH,
    ASK_DATE_DAY,
    ASK_TIME_SLOT,
    ASK_VEHICLE,
    CONFIRM_APPOINTMENT,
    ASK_APPOINTMENT_TO_CANCEL,
    CONFIRM_APPOINTMENT_CANCELLATION
}
