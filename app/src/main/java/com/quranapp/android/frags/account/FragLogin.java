/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

package com.quranapp.android.frags.account;

import static com.quranapp.android.components.account.CurrentUser.USER_KEY_EMAIL;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_PASSWORD;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.quranapp.android.R;
import com.quranapp.android.activities.account.ActivityAccount;
import com.quranapp.android.databinding.FragLoginBinding;
import com.peacedesign.android.utils.Log;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.widgets.form.PeaceFormInputField;

public class FragLogin extends FragAccountBase {
    private FragLoginBinding mBinding;

    public FragLogin() {
    }

    public static FragLogin newInstance() {
        return new FragLogin();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.frag_login;
    }

    @Override
    protected void onAccountViewReady(Context ctx, View mainContent) {
        mBinding = FragLoginBinding.bind(mainContent);
        setupAccountHelperMethod(ctx);

        mBinding.submit.setOnClickListener(v -> initLogin(ctx));
    }

    private void initLogin(Context ctx) {
        if (isInvalid(ctx, mBinding.email, USER_KEY_EMAIL) || isInvalid(ctx, mBinding.password, USER_KEY_PASSWORD)) {
            return;
        }

        CharSequence email = mBinding.email.getText();
        CharSequence password = mBinding.password.getText();


        ActivityAccount activity = activity();
        if (activity == null) {
            return;
        }

        if (!NetworkStateReceiver.canProceed(ctx)) {
            return;
        }

        activity.login(trim(email), trim(password), e -> {
            if (e instanceof FirebaseAuthInvalidUserException) {
                String errorCode = ((FirebaseAuthInvalidUserException) e).getErrorCode();
                if ("ERROR_USER_DISABLED".equalsIgnoreCase(errorCode)) {
                    mBinding.email.setFieldWarning(R.string.strMsgDisabledUser);
                } else if ("ERROR_USER_NOT_FOUND".equalsIgnoreCase(errorCode)) {
                    mBinding.email.setFieldWarning(R.string.strMsgInvalidUserCreateAccount);
                } else {
                    mBinding.email.setFieldWarning(R.string.strMsgInvalidUserRightNow);
                }
            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                mBinding.password.setFieldWarning(R.string.strMsgIncorrectPassword);
            } else {
                activity.showInfoDialog(R.string.strTitleError, R.string.strMsgSomethingWrong);
            }
            Log.d("LOGIN FAILED", e);
        });
    }

    public static boolean isInvalid(Context ctx, PeaceFormInputField field, String name) {
        field.setFieldWarning(null);

        String value = "";

        if (field.getText() != null) {
            value = field.getText().toString().trim();
        }

        if (TextUtils.isEmpty(value)) {
            field.setFieldWarning(ctx.getString(R.string.strMsgFieldRequired, field.getFieldTitle()));
            field.setFocus();
            return true;
        }

        if (name.equals(USER_KEY_EMAIL) && !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            field.setFieldWarning(ctx.getString(R.string.strMsgEmailNotValid));
            field.setFocus();
            return true;
        }

        return false;
    }

    private void setupAccountHelperMethod(Context ctx) {
        mBinding.forgotPass.setOnClickListener(v -> {
            ActivityAccount activity = activity();
            if (activity != null) {
                activity.initForgotPassword();
            }
        });
        mBinding.createRef.setOnClickListener(v -> {
            ActivityAccount activity = activity();
            if (activity != null) {
                activity.switchToRegister();
            }
        });

        SpannableString alternateText = createLink(ctx, getString(R.string.strLabelCreateNew));

        CharSequence txt = TextUtils.concat(getString(R.string.strMsgDontHaveAccount), " ", alternateText);
        mBinding.createRef.setText(txt);
    }

    public String getInputEmail() {
        if (mBinding == null) {
            return null;
        }
        return String.valueOf(mBinding.email.getText());
    }
}
