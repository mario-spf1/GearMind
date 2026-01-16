package com.gearmind.application.email;

public record SendBudgetEmailRequest(long budgetId, String recipientEmail, String message) {

}
