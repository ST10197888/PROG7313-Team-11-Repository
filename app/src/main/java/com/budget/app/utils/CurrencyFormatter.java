package com.budget.app.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {

    private static final NumberFormat formatter =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));

    public static String format(double amount) {
        return formatter.format(amount);
    }

    public static String formatNoSymbol(double amount) {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }
}
