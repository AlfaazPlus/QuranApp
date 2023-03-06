package com.quranapp.android.utils.univ;

import android.content.Context;
import android.widget.Toast;

import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;

import java.lang.ref.WeakReference;

public class MessageUtils {
    private static WeakReference<Toast> mToast;

    public static void showRemovableToast(Context context, CharSequence msg, int duration) {
        try {
            mToast.get().cancel();
        } catch (Exception ignored) {}

        mToast = new WeakReference<>(Toast.makeText(context, msg, duration));
        mToast.get().show();
    }

    public static void popNoInternetMessage(Context ctx, boolean cancelable, Runnable runOnDismiss) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx);
        builder.setTitle(R.string.strTitleNoInternet);
        builder.setMessage(R.string.strMsgNoInternetLong);
        builder.setNeutralButton(R.string.strLabelClose, null);
        if (runOnDismiss != null) {
            builder.setOnDismissListener(dialog -> runOnDismiss.run());
        }
        builder.setCancelable(cancelable);
        builder.setFocusOnNeutral(true);
        builder.show();
    }

    public static void popMessage(Context context, String title, String msg, String btn, Runnable action) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(btn, (dialog, which) -> {
            if (action != null) {
                action.run();
            }
        });
        builder.setFocusOnNeutral(true);
        builder.show();
    }
}
