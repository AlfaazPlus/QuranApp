package com.peacedesign.android.widget.dialog.loader;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.R;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.dialog.base.PeaceDialogController;

public class ProgressDialog extends PeaceDialog {
    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
    }

    @NonNull
    @Override
    protected PeaceDialogController getController(@NonNull Context context, @NonNull PeaceDialog dialog) {
        return ProgressDialogController.create(context, dialog);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        View progressDialogLayout = View.inflate(getContext(), R.layout.layout_progress_dialog, null);
        setView(progressDialogLayout);
        setDialogWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFullScreen(false);
        setCanceledOnTouchOutside(false);
        setDialogGravity(PeaceDialog.GRAVITY_CENTER);
        super.onCreate(savedInstanceState);
    }

    public void setAllowOutsideTouches(Boolean allow) {
        controller.setAllowOutsideTouches(allow);
    }

    public static class ProgressDialogController extends PeaceDialogController {
        protected ProgressDialogController(@NonNull Context context, @NonNull PeaceDialog dialog) {
            super(context, dialog);
        }

        @NonNull
        public static ProgressDialogController create(@NonNull Context context, @NonNull PeaceDialog dialog) {
            return new ProgressDialogController(context, dialog);
        }

        @Override
        protected void setupContent(ViewGroup contentPanel) {
            super.setupContent(contentPanel);

            TextView messageView = contentPanel.findViewById(R.id.message);
            if (!TextUtils.isEmpty(mMessage)) {
                messageView.setText(mMessage);
                messageView.setVisibility(View.VISIBLE);
            } else {
                messageView.setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        protected TextView createTitleView(@NonNull Context context) {
            TextView titleView = super.createTitleView(context);
            titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            return titleView;
        }

        @Nullable
        @Override
        protected TextView createMessageView(@NonNull Context context) {
            TextView messageView = super.createMessageView(context);
            if (messageView != null) {
                messageView.setPadding(0, 0, 0, Dimen.dp2px(getContext(), 10));
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
            return messageView;
        }

        @Override
        public int getDialogMaxWidth() {
            return Dimen.dp2px(getContext(), 200);
        }

        @Override
        public int getDialogMinHeight() {
            return 0;
        }
    }
}
