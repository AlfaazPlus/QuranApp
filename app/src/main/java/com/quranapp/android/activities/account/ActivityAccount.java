/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

package com.quranapp.android.activities.account;

import static com.quranapp.android.utils.account.AccManager.ACCOUNT_LANDING_PAGE_CREATE;
import static com.quranapp.android.utils.account.AccManager.ACCOUNT_LANDING_PAGE_LOGIN;
import static com.quranapp.android.utils.account.AccManager.KEY_ACCOUNT_LANDING_PAGE;
import static com.quranapp.android.utils.account.AccManager.KEY_CURRENT_USER;
import static com.quranapp.android.utils.account.AccManager.updateProfileInFB;
import static com.quranapp.android.utils.account.AccManager.validateEmail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.peacedesign.android.utils.Log;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;
import com.quranapp.android.components.account.CurrentUser;
import com.quranapp.android.databinding.ActivityAccountBinding;
import com.quranapp.android.frags.account.FragLogin;
import com.quranapp.android.frags.account.FragRegister;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.account.AccManager;
import com.quranapp.android.utils.app.InfoUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.PendingProfilesUtils;
import com.quranapp.android.utils.univ.Codes;
import com.quranapp.android.utils.univ.NotifUtils;
import com.quranapp.android.widgets.form.InputBottomSheet;
import com.quranapp.android.widgets.form.PeaceFormInputField;

import java.util.Calendar;

public class ActivityAccount extends BaseActivity implements InputBottomSheet.InputBottomSheetBtnActionListener {
    private static final String RESET_PASSWORD_DIALOG_TAG = "ResetPasswordDialog";
    private FirebaseAuth mAuth;
    private ActivityAccountBinding mBinding;
    private ProgressDialog mProgressDialog;
    private FragLogin mFragLogin;

    public static void launchLogin(Context ctx) {
        Intent intent = new Intent(ctx, ActivityAccount.class);
        intent.putExtra(KEY_ACCOUNT_LANDING_PAGE, ACCOUNT_LANDING_PAGE_LOGIN);
        ctx.startActivity(intent);
    }

    public static void launchRegister(Context ctx) {
        Intent intent = new Intent(ctx, ActivityAccount.class);
        intent.putExtra(KEY_ACCOUNT_LANDING_PAGE, ACCOUNT_LANDING_PAGE_CREATE);
        ctx.startActivity(intent);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_account;
    }

    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGPageVariable);
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        super.preActivityInflate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityAccountBinding.bind(activityView);
        setContentView(activityView);

        mBinding.close.setOnClickListener(v -> onBackPressed());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            NotifUtils.popMsg(this, str(R.string.strTitleError), str(R.string.strMsgAlreadyLoggedIn), str(R.string.strLabelClose),
                    this::finish);
        } else {
            initViewpage2(mBinding.viewPager);
        }

        if (savedInstanceState != null) {
            Fragment dialog = getSupportFragmentManager().findFragmentByTag(RESET_PASSWORD_DIALOG_TAG);
            if (dialog instanceof InputBottomSheet) {
                ((InputBottomSheet) dialog).setActionListener(this);
            }
        }
    }

    private void initViewpage2(ViewPager2 viewPager) {
        mBinding.btnLogin.setOnClickListener(v -> viewPager.setCurrentItem(0));
        mBinding.btnRegister.setOnClickListener(v -> viewPager.setCurrentItem(1));

        ViewPagerAdapter2 adp = new ViewPagerAdapter2(this);

        mFragLogin = FragLogin.newInstance();
        adp.addFragment(mFragLogin, str(R.string.strLabelLogin));

        FragRegister fragRegister = FragRegister.newInstance();
        adp.addFragment(fragRegister, str(R.string.strLabelRegister));


        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mBinding.btnLogin.setSelected(position == 0);
                mBinding.btnRegister.setSelected(position != 0);
            }
        });
        viewPager.setAdapter(adp);
        viewPager.setOffscreenPageLimit(adp.getItemCount());
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        if (getIntent().getIntExtra(KEY_ACCOUNT_LANDING_PAGE, 0) == ACCOUNT_LANDING_PAGE_CREATE) {
            viewPager.setCurrentItem(1, false);
        } else {
            viewPager.setCurrentItem(0, false);
        }
    }

    public void login(String email, String password, OnResultReadyCallback<Exception> excCallback) {
        showProgressDialog(R.string.strTextLoggingIn);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            getProfileFromFirestoreAndUpdateInFB(currentUser);
                        } else {
                            Log.d("LOGIN FAILED", task.getException());
                            mAuth.signOut();
                            showInfoDialog(R.string.strTitleError, R.string.strMsgLoginFailed);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e != null) {
                            excCallback.onReady(e);
                            hideProgressDialog();
                        } else {
                            showInfoDialog(R.string.strTitleError, R.string.strMsgLoginFailed);
                        }
                    }
                });
    }

    private void getProfileFromFirestoreAndUpdateInFB(FirebaseUser fbUser) {
        AccManager.fetchProfile(this, fbUser, currentUser -> {
            Log.d("PROFILE CREATE SUCCESS");
            hideProgressDialog();
            updateProfileInFB(fbUser, currentUser.getFname(), currentUser.getFname());
            loginSuccess(currentUser);
        }, e -> {
            Log.d("LOGIN FAILED", e);
            e.printStackTrace();
            mAuth.signOut();
            showInfoDialog(R.string.strTitleError, R.string.strMsgLoginFailed);
        }, true);
    }

    public void register(String fname, String lname, String email, String password, OnResultReadyCallback<Exception> excCallback) {
        showProgressDialog(R.string.strTextCreatingAccount);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            createProfileInFB(currentUser, fname, lname);
                            updateProfileInFB(currentUser, fname, lname);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e != null) {
                            e.printStackTrace();
                        }

                        Log.d("CREATE FAILED", e);

                        if (e != null) {
                            excCallback.onReady(e);
                        }
                    }
                });
    }

    private void createProfileInFB(FirebaseUser fbUser, String fname, String lname) {
        long timeInMillis = Calendar.getInstance().getTimeInMillis();

        AccManager.updateProfileInFirestore(
                new PendingProfilesUtils(this),
                fbUser, fbUser.getEmail(),
                fname, lname,
                timeInMillis, timeInMillis,
                (OnResultReadyCallback<CurrentUser>) currentUser -> {
                    Log.d("PROFILE CREATE SUCCESS");
                    hideProgressDialog();
                    loginSuccess(currentUser);
                },
                e -> {
                    Log.d("PROFILE CREATE FAILED", e);
                    fbUser.delete();
                    showInfoDialog(R.string.strTitleError, R.string.strMsgRegisterFailed);
                }
        );
    }

    private void loginSuccess(CurrentUser currentUser) {
        Log.d("LOGIN SUCCESS");
        Intent data = new Intent();
        data.putExtra(KEY_CURRENT_USER, currentUser);
        setResult(Codes.REQ_CODE_LOG_IN, data);
        finish();
    }

    public void switchToLogin() {
        mBinding.viewPager.setCurrentItem(0);
    }

    public void switchToRegister() {
        mBinding.viewPager.setCurrentItem(1);
    }

    public void privacyPolicy() {
        InfoUtils.openPrivacyPolicy(this);
    }

    public void initForgotPassword() {
        InputBottomSheet dialog = new InputBottomSheet();
        dialog.setStateSaveEnabled(true);
        dialog.setHeaderTitle(str(R.string.strLabelResetPassword));
        dialog.setMessage(str(R.string.strMsgEnterAssocEmail));
        dialog.setInputHint(str(R.string.strFieldHintEmail));
        dialog.setInputText(mFragLogin.getInputEmail());
        dialog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        dialog.setButton(str(R.string.strLabelNext), this);
        dialog.show(getSupportFragmentManager(), RESET_PASSWORD_DIALOG_TAG);
    }

    public void showInfoDialog(int titleRes, int msgRes) {
        hideProgressDialog();

        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(titleRes);
        builder.setMessage(msgRes);
        builder.setNeutralButton(R.string.strLabelClose, null);
        builder.show();
    }

    private void showProgressDialog(int msgRes) {
        mProgressDialog = new ProgressDialog(this);
        if (msgRes != 0) {
            mProgressDialog.setTitle(msgRes);
        }
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }


    /**
     * InputBottomSheet button action listener
     */
    @Override
    public void onButtonAction(InputBottomSheet dialog, CharSequence inputText) {
        if (!validateEmail(this, dialog.getInputField())) {
            return;
        }

        if (!NetworkStateReceiver.isNetworkConnected(this)) {
            dialog.setInputWarning(str(R.string.strMsgNoInternet));
            return;
        }

        dialog.loader(true);
        String email = inputText.toString().trim();

        mAuth.sendPasswordResetEmail(email).addOnSuccessListener(unused -> {
            dialog.dismiss();
            String msg = str(R.string.strMsgEmailSentResetPassword, email);
            NotifUtils.popMsg(this, str(R.string.strTitleEmailSent), msg, str(R.string.strLabelOkay),
                    () -> mBinding.viewPager.setCurrentItem(0, false));
        }).addOnFailureListener(e -> {
            dialog.loader(false);

            PeaceFormInputField field = dialog.getInputField();
            if (e instanceof FirebaseAuthInvalidUserException) {
                String errorCode = ((FirebaseAuthInvalidUserException) e).getErrorCode();
                if ("ERROR_USER_DISABLED".equalsIgnoreCase(errorCode)) {
                    field.setFieldWarning(R.string.strMsgDisabledUser);
                } else if ("ERROR_USER_NOT_FOUND".equalsIgnoreCase(errorCode)) {
                    field.setFieldWarning(R.string.strMsgInvalidUserCreateAccount);
                } else {
                    field.setFieldWarning(R.string.strMsgInvalidUserRightNow);
                }
            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                field.setFieldWarning(R.string.strMsgEmailNotValid);
            } else {
                Log.d(e);
                NotifUtils.popMsg(this, str(R.string.strTitleOops), getString(R.string.strMsgSomethingWrong),
                        getString(R.string.strLabelClose), null);
            }
        });
    }
}