/*
 * (c) Faisal Khan. Created on 28/1/2022.
 */

package com.quranapp.android.activities.account;

import static com.quranapp.android.utils.account.AccManager.KEY_CURRENT_USER;
import static com.quranapp.android.utils.account.AccManager.KEY_EMAIL_VERIFICATION_SENT;
import static com.quranapp.android.utils.account.AccManager.KEY_NEW_EMAIL;
import static com.quranapp.android.utils.univ.Codes.REQ_CODE_LOG_IN;
import static com.quranapp.android.utils.univ.Codes.REQ_CODE_PROFILE_UPDATE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.Log;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.components.account.CurrentUser;
import com.quranapp.android.databinding.ActivityProfileBinding;
import com.quranapp.android.databinding.LytProfileInfoItemBinding;
import com.quranapp.android.utils.account.AccManager;
import com.quranapp.android.utils.account.AccManager.EmailVerifyHelper;

import org.apache.commons.lang3.BooleanUtils;

public class ActivityProfile extends BaseActivity implements AccManager.EmailVerificationListener {
    private FirebaseAuth mAuth;
    private EmailVerifyHelper mEmailVerifyHelper;
    private AsyncLayoutInflater mInflater;
    private ActivityProfileBinding mBinding;
    private View mVerifyEmailButton;
    private CurrentUser mCurrentUser;
    private ProgressDialog mProgressDialog;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_profile;
    }

    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGCardVariable);
    }

    @Override
    protected int getNavBarBG() {
        return color(R.color.colorBGPageVariable);
    }

    @Override
    protected void initCreate(Bundle savedInstanceState) {
        if (!checkLoggedIn(FirebaseAuth.getInstance())) {
            return;
        }

        super.initCreate(savedInstanceState);
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        mInflater = new AsyncLayoutInflater(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mEmailVerifyHelper == null) {
            return;
        }

        if (mVerifyEmailButton != null) {
            mEmailVerifyHelper.checkEmailVerified(aBoolean -> {
                if (BooleanUtils.isTrue(aBoolean)) {
                    ViewUtils.removeView(mVerifyEmailButton);
                }
            });
        }

        FirebaseUser fbUser = mAuth.getCurrentUser();
        if (fbUser == null) {
            return;
        }
        if (mCurrentUser != null) {
            CurrentUser nCurrentUser = AccManager.getUserFromSP(this, fbUser);
            if (!mCurrentUser.equals(nCurrentUser)) {
                populateProfile(nCurrentUser);
            }
        }
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityProfileBinding.bind(activityView);
        mBinding.back.setOnClickListener(v -> onBackPressed());

        validateAndPopulateProfile(mAuth);
    }

    private boolean checkLoggedIn(FirebaseAuth auth) {
        if (auth.getCurrentUser() == null) {
            auth.signOut();
            startLoginActivity();
            finish();
            return false;
        }
        return true;
    }

    private void validateAndPopulateProfile(FirebaseAuth auth) {
        if (!checkLoggedIn(auth)) {
            return;
        }

        getProfileFromFirestoreAndPopulate(auth.getCurrentUser() /*NetworkStateReceiver.isNetworkConnected(this)*/);
    }

    private void startLoginActivity() {
        startActivity4Result(new Intent(this, ActivityAccount.class), null);
    }

    private void getProfileFromFirestoreAndPopulate(FirebaseUser fbUser) {
        showProgressDialog(true);
        AccManager.fetchProfile(this, fbUser, this::populateProfile, e -> {
            Log.d("FETCH FAILED", e);
            hideProgressDialog();
            Toast.makeText(this, str(R.string.strMsgSomethingWrong), Toast.LENGTH_SHORT).show();
        }, false);
    }

    private void populateProfile(CurrentUser currentUser) {
        hideProgressDialog();
        mCurrentUser = currentUser;
        mEmailVerifyHelper = new EmailVerifyHelper(this, currentUser.getFbUser(), this);

        CharSequence fullName = TextUtils.concat(currentUser.getFname(), " ", currentUser.getLname());
        mBinding.fullName.setText(fullName);
        mBinding.editProfile.setOnClickListener(v -> editProfile());
        mBinding.btnLogout.setOnClickListener(v -> logoutWithAlert(mAuth));

        mBinding.fullName.setVisibility(View.VISIBLE);
        mBinding.editProfile.setVisibility(View.VISIBLE);
        mBinding.btnLogout.setVisibility(View.VISIBLE);

        mEmailVerifyHelper.checkEmailVerified(aBoolean -> {
            if (!BooleanUtils.isTrue(aBoolean)) {
                makeEmailVerificationButton();
            }
        });

        mBinding.detailsContainer.removeAllViews();
        makeProfileInfo(R.drawable.dr_icon_user, R.string.strFieldTitleName, fullName);
        makeProfileInfo(R.drawable.dr_icon_email, R.string.strFieldTitleEmail, currentUser.getEmail());
        // makeProfileInfo(R.drawable.dr_icon_user, R.string.strFieldTitleGender, currentUser.getGender(this));
    }

    private void makeEmailVerificationButton() {
        mInflater.inflate(R.layout.lyt_btn_verify_account, mBinding.detailsContainer, (view, resid, parent) -> {
            mVerifyEmailButton = view;
            mBinding.detailsContainer.addView(view, 0);
            view.setOnClickListener(v -> mEmailVerifyHelper.initVerification());
        });
    }

    @SuppressLint("RtlHardcoded")
    private void makeProfileInfo(int iconRes, int nameRes, CharSequence value) {
        mInflater.inflate(R.layout.lyt_profile_info_item, mBinding.detailsContainer, (view, resid, parent) -> {
            LytProfileInfoItemBinding binding = LytProfileInfoItemBinding.bind(view);
            binding.icon.setImageResource(iconRes);
            binding.infoName.setText(nameRes);
            binding.infoValue.setText(value);
            binding.infoValue.setGravity(WindowUtils.isRTL(this) ? Gravity.RIGHT : Gravity.LEFT);
            mBinding.detailsContainer.addView(binding.getRoot());
        });
    }

    private void editProfile() {
        startActivity4Result(new Intent(this, ActivityEditProfile.class), null);
    }

    private void logoutWithAlert(FirebaseAuth auth) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(R.string.strLabelLogout);
        builder.setMessage(R.string.strMsgLogout);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setNegativeButton(R.string.strLabelLogout, ColorUtils.DANGER, (dialog, which) -> {
            AccManager.logoutUser(this, auth);
            startLoginActivity();
        });
        builder.setFocusOnNegative(true);
        builder.show();
    }

    @Override
    public void onActivityResult2(ActivityResult result) {
        if (result.getResultCode() == REQ_CODE_LOG_IN || result.getResultCode() == REQ_CODE_PROFILE_UPDATE) {
            Intent data = result.getData();
            if (data != null) {
                CurrentUser currentUser = data.getParcelableExtra(KEY_CURRENT_USER);
                if (currentUser != null) {
                    if (result.getResultCode() == REQ_CODE_LOG_IN) {
                        alertLoginSucceed();
                    } else {
                        if (data.getBooleanExtra(KEY_EMAIL_VERIFICATION_SENT, false)) {
                            mEmailVerifyHelper.notifyLinkSent(data.getStringExtra(KEY_NEW_EMAIL), () -> AccManager.logoutUser(this, mAuth));
                        }
                    }
                    populateProfile(currentUser);
                } else {
                    alertLogin(R.string.strTitleSessionExpired);
                }
            }
        } else {
            if (mAuth.getCurrentUser() != null) {
                return;
            }

            Log.d("FROM HERE2");
            alertLogin(R.string.strTitleError);
        }
    }

    private void showProgressDialog(boolean cancelable) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.show();
        mProgressDialog.setCancelable(cancelable);
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void alertLoginSucceed() {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(R.string.strTitleSuccess);
        builder.setMessage(R.string.strMsgNowLoggedIn);
        builder.setNeutralButton(R.string.strLabelOkay, color(R.color.colorPrimary), null);
        builder.setFocusOnNeutral(true);
        PeaceDialog dialog = builder.create();
        dialog.show();
        new Handler().postDelayed(dialog::dismiss, 2000);
    }

    private void alertLogin(int titleRes) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(titleRes);
        builder.setMessage(R.string.strMsgLoginToContinue);
        builder.setNegativeButton(R.string.strLabelCancel, ColorUtils.DANGER, (dialog, which) -> finish());
        builder.setPositiveButton(R.string.strLabelLogin, color(R.color.colorPrimary), (dialog, which) -> startLoginActivity());
        builder.setFocusOnPositive(true);
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void preEmailVerificationSend() {
        showProgressDialog(false);
    }

    @Override
    public void postEmailVerificationSend() {
        hideProgressDialog();
    }
}
