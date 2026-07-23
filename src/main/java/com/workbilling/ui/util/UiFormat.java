package com.workbilling.ui.util;

import com.workbilling.util.AppPaths;

public final class UiFormat {
    private UiFormat() {
    }

    public static String currency(double amount) {
        return AppPaths.formatCurrency(amount);
    }
}
