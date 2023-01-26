/*
 * (c) Faisal Khan. Created on 7/10/2021.
 */

package com.quranapp.android.suppliments;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.databinding.LytFeedbackDialogBinding;
import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.univ.SimpleTextWatcher;

import java.util.HashMap;
import java.util.Map;

public class FeedbackDialog {
    private final Context mContext;
    private final PeaceDialog mPopup;
    private final DatabaseReference mFeedbacksDB;

    public FeedbackDialog(Context context) {
        mContext = context;
        mPopup = createPopup(mContext);
        mFeedbacksDB = FirebaseDatabase.getInstance().getReference().child("feedbacks");
        init(mContext);
    }

    private PeaceDialog createPopup(Context ctx) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx);
        builder.setFullscreen(true);
        builder.setCancelable(false);
        return builder.create();
    }

    private void init(Context ctx) {
        AsyncLayoutInflater inflater = new AsyncLayoutInflater(ctx);
        inflater.inflate(R.layout.lyt_feedback_dialog, null, (view, resid, parent) -> {
            LytFeedbackDialogBinding binding = LytFeedbackDialogBinding.bind(view);
            mPopup.setContentView(view);
            mPopup.setupDimension();
            setup(binding);
        });
    }

    private void setup(LytFeedbackDialogBinding binding) {
        binding.close.setOnClickListener(v -> close());

        ViewUtils.disableView(binding.btnSend, true);
        ViewUtils.addHoverPushOpacityEffect(binding.btnSend);
        binding.btnSend.setOnClickListener(v -> {
            CharSequence emailText = binding.email.getText();
            CharSequence messageText = binding.message.getText();
            if (!TextUtils.isEmpty(emailText) && !TextUtils.isEmpty(messageText)) {
                sendFeedback(binding, emailText, messageText);
            }
        });

        addTextChangeListener(binding.email, binding);
        addTextChangeListener(binding.message, binding);
    }

    private void addTextChangeListener(TextView email, LytFeedbackDialogBinding binding) {
        email.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                super.onTextChanged(s, start, before, count);
                boolean disable = TextUtils.isEmpty(binding.email.getText()) || TextUtils.isEmpty(binding.message.getText());
                ViewUtils.disableView(binding.btnSend, disable);
                binding.btnSend.setBackgroundResource(disable
                        ? R.drawable.dr_bg_button_action_small_disabled
                        : R.drawable.dr_bg_button_action_alpha);
            }
        });
    }

    private void sendFeedback(LytFeedbackDialogBinding binding, CharSequence emailText, CharSequence message) {
        if (!NetworkStateReceiver.isNetworkConnected(mContext)) {
            Toast.makeText(mContext, R.string.strMsgNoInternet, Toast.LENGTH_LONG).show();
            return;
        }

        boolean validated = validateEmail(emailText);
        if (!validated) {
            binding.email.setError(mContext.getString(R.string.strMsgInvalidEmail));
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("email", emailText.toString());
        map.put("message", message.toString());

        map.put("appConfigs", AppUtils.getAppConfigs(mContext));
        map.put("deviceInformation", AppUtils.getDeviceInformation(mContext));

        String key = mFeedbacksDB.push().getKey();
        if (key == null) {
            return;
        }

        Toast.makeText(mContext, R.string.strTextSending, Toast.LENGTH_SHORT).show();
        mFeedbacksDB.child(key).setValue(map, (error, ref) -> {
            if (error == null) {
                Toast.makeText(mContext, R.string.strMsgFeedbackSendSuccess, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, R.string.strMsgFeedbackSendFailed, Toast.LENGTH_LONG).show();
                error.toException().printStackTrace();
            }
        });
        close();
    }

    private boolean validateEmail(CharSequence emailText) {
        return Patterns.EMAIL_ADDRESS.matcher(emailText).matches();
    }

    public void open() {
        mPopup.show();
    }

    private void close() {
        mPopup.dismiss();
    }
/*

    {
        "email": "faisalkhanp100@gmail.com",
        "name": "Faisal Khan",
        "feedbackType": "0",
        "message": "A Message",
        "appConfigs":  "config",
        "deviceInformation":  "info"
    }

        */
}
