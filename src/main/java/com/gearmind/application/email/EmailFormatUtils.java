package com.gearmind.application.email;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class EmailFormatUtils {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("es", "ES")));

    private EmailFormatUtils() {
    }

    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0,00 €";
        }
        return MONEY_FORMAT.format(amount) + " €";
    }
}
