package com.gearmind.application.email;

public record SendInvoiceEmailRequest(long invoiceId, String recipientEmail, String message) {

}
