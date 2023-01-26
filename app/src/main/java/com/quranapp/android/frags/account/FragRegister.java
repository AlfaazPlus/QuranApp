/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

package com.quranapp.android.frags.account;

import static com.quranapp.android.utils.account.AccManager.isInvalid;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_CPASSWORD;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_EMAIL;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_FNAME;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_LNAME;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_PASSWORD;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.View;

import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.quranapp.android.R;
import com.quranapp.android.activities.account.ActivityAccount;
import com.quranapp.android.databinding.FragRegisterBinding;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;

public class FragRegister extends FragAccountBase {
    private FragRegisterBinding mBinding;

    public FragRegister() {
    }

    public static FragRegister newInstance() {
        return new FragRegister();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.frag_register;
    }

    @Override
    protected void onAccountViewReady(Context ctx, View mainContent) {
        mBinding = FragRegisterBinding.bind(mainContent);
        setupAccountHelperMethod(ctx);

        mBinding.submit.setOnClickListener(v -> initRegistration(ctx));
    }

    private void initRegistration(Context ctx) {
        if (isInvalid(ctx, mBinding.fname, USER_KEY_FNAME, null) || isInvalid(ctx, mBinding.lname, USER_KEY_LNAME, null) ||
                isInvalid(ctx, mBinding.email, USER_KEY_EMAIL, null) || isInvalid(ctx, mBinding.password, USER_KEY_PASSWORD, null) ||
                isInvalid(ctx, mBinding.cpassword, USER_KEY_CPASSWORD, String.valueOf(mBinding.password.getText()))) {
            return;
        }

        CharSequence fname = mBinding.fname.getText();
        CharSequence lname = mBinding.lname.getText();
        CharSequence email = mBinding.email.getText();
        CharSequence password = mBinding.password.getText();

        ActivityAccount activity = activity();
        if (activity == null) {
            return;
        }

        if (!NetworkStateReceiver.canProceed(ctx)) {
            return;
        }

        activity.register(trim(fname), trim(lname), trim(email), trim(password), e -> {
            if (e instanceof FirebaseAuthUserCollisionException) {
                mBinding.email.setFieldWarning(R.string.strMsgEmailAlreadyTaken);
            } else if (e instanceof FirebaseAuthWeakPasswordException) {
                mBinding.password.setFieldWarning(R.string.strMsgWeakPassword);
            } else {
                activity.showInfoDialog(R.string.strTitleError, R.string.strMsgRegisterFailed);
            }

            activity.hideProgressDialog();
        });
    }

    private void setupAccountHelperMethod(Context ctx) {
        mBinding.privacyLink.setOnClickListener(v -> {
            ActivityAccount activity = activity();
            if (activity != null) {
                activity.privacyPolicy();
            }
        });
        mBinding.loginRef.setOnClickListener(v -> {
            ActivityAccount activity = activity();
            if (activity != null) {
                activity.switchToLogin();
            }
        });

        SpannableString loginText = createLink(ctx, getString(R.string.strLabelClickLogin));

        CharSequence txt = TextUtils.concat(getString(R.string.strMsgHaveAccount), " ", loginText);
        mBinding.loginRef.setText(txt);
    }
}
