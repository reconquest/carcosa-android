package io.reconquest.carcosa;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdge {
    public static void apply(View toolbarWrapper) {
        ViewCompat.setOnApplyWindowInsetsListener(
            toolbarWrapper,
            (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                    v.getPaddingLeft(),
                    insets.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom());
                return windowInsets;
            });
    }
}
