/*
 * (c) Faisal Khan. Created on 28/1/2022.
 */

package com.quranapp.android.activities.account;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.quranapp.android.components.account.CurrentUser.FIRESTORE_COLLECTION;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_CPASSWORD;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_EMAIL;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_FNAME;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_LNAME;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_PASSWORD;
import static com.quranapp.android.utils.account.AccManager.KEY_CURRENT_USER;
import static com.quranapp.android.utils.account.AccManager.KEY_EMAIL_VERIFICATION_SENT;
import static com.quranapp.android.utils.account.AccManager.KEY_NEW_EMAIL;
import static com.quranapp.android.utils.account.AccManager.isInvalid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.Log;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.components.account.CurrentUser;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.databinding.ActivityProfileEditBinding;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.account.AccManager;
import com.quranapp.android.utils.fb.FirebaseUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.PendingProfilesUtils;
import com.quranapp.android.utils.univ.Codes;
import com.quranapp.android.utils.univ.NotifUtils;
import com.quranapp.android.utils.univ.SimpleTextWatcher;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.views.helper.Spinner2;
import com.quranapp.android.widgets.form.InputBottomSheet;
import com.quranapp.android.widgets.form.PeaceFormInputField;
import com.quranapp.android.widgets.form.PeaceFormSelectField;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityEditProfile extends BaseActivity implements InputBottomSheet.InputBottomSheetBtnActionListener {
    private static final String CHANGE_PASSWORD_DIALOG_TAG = "ChangePasswordDialog";

    private final AtomicReference<String> mChangePassFieldKey = new AtomicReference<>();
    private final AtomicReference<String> mChangePassFieldTitle = new AtomicReference<>();
    private final AtomicReference<String> mChangePassPassword = new AtomicReference<>();

    private FirebaseAuth mAuth;
    private ActivityProfileEditBinding mBinding;
    private ProgressDialog mProgressDialog;
    private boolean mFnameChanged;
    private boolean mLnameChanged;
    private boolean mEmailChanged;
    private boolean mGenderChanged;
    private int nGenderCode;
    private PeaceFormInputField fnameField;
    private PeaceFormInputField lnameField;
    private PeaceFormInputField emailField;
    private CurrentUser mCurrentUser;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_profile_edit;
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGPage);
    }

    @Override
    protected int getNavBarBG() {
        return color(R.color.colorBGPageVariable);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityProfileEditBinding.bind(activityView);

        validateAndPopulateProfile(mAuth);

        if (savedInstanceState != null) {
            Fragment dialog = getSupportFragmentManager().findFragmentByTag(CHANGE_PASSWORD_DIALOG_TAG);
            if (dialog instanceof InputBottomSheet) {
                ((InputBottomSheet) dialog).setActionListener(this);
            }
        }
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(new BoldHeader.BoldHeaderCallback() {
            @Override
            public void onBackIconClick() {
                finish();
            }

            @Override
            public void onRightIconClick() {
                updateProfile(mCurrentUser);
            }
        });

        header.setRightIconRes(R.drawable.dr_icon_check, str(R.string.strLabelDone));
        header.setShowRightIcon(true);
        header.disableRightBtn(true);
        header.setTitleText(R.string.strTitleEditProfile);
        header.setBGColor(R.color.colorBGPage);
    }

    private void validateAndPopulateProfile(FirebaseAuth auth) {
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            invalidateUser();
            return;
        }

        initHeader(mBinding.header);
        getProfileFromFirestoreAndPopulate(fbUser);
    }

    private void getProfileFromFirestoreAndPopulate(FirebaseUser fbUser) {
        if (!NetworkStateReceiver.canProceed(this, false, this::finish)) return;

        showProgressDialog(R.string.strTextLoading, true);
        AccManager.fetchProfile(this, fbUser, currentUser -> {
            hideProgressDialog();
            populateProfile(currentUser);
        }, e -> {
            Log.d("FETCH FAILED", e);
            showInfoDialog(R.string.strTitleError, R.string.strMsgSomethingWrong);
        }, false);
    }

    private void invalidateUser() {
        Log.d("INVALIDATED");
        hideProgressDialog();
        mAuth.signOut();
        launchActivity(ActivityAccount.class);
        finish();
    }

    private void populateProfile(CurrentUser currentUser) {
        mCurrentUser = currentUser;
        mBinding.btnChangePassword.setOnClickListener(v -> changePassword(currentUser));
        mBinding.btnDeleteAccount.setOnClickListener(v -> deleteAccount(currentUser));
        mBinding.btnChangePassword.setVisibility(View.VISIBLE);
        mBinding.btnDeleteAccount.setVisibility(View.VISIBLE);

        nGenderCode = currentUser.getGenderCode();

        mBinding.detailsContainer.removeAllViews();
        fnameField = makeProfileInfo(USER_KEY_FNAME, R.string.strFieldTitleFName, currentUser.getFname());
        lnameField = makeProfileInfo(USER_KEY_LNAME, R.string.strFieldTitleLName, currentUser.getLname());
        emailField = makeProfileInfo(USER_KEY_EMAIL, R.string.strFieldTitleEmail, currentUser.getEmail());
        // makeProfileGender(nGenderCode);
    }

    private PeaceFormInputField makeProfileInfo(String key, int nameRes, String orgValue) {
        PeaceFormInputField formField = new PeaceFormInputField(this);
        formField.setBackgroundResource(R.drawable.dr_bg_login);
        formField.setFieldTitle(nameRes);
        formField.setFieldText(orgValue);

        ViewUtils.setPaddings(formField, dp2px(15));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        ViewUtils.setMarginVertical(p, dp2px(5));
        mBinding.detailsContainer.addView(formField, p);

        formField.getFieldInputView().addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String nValue = String.valueOf(s);

                if (!TextUtils.isEmpty(nValue)) {
                    nValue = nValue.toLowerCase();
                }

                if (USER_KEY_FNAME.equalsIgnoreCase(key)) {
                    mFnameChanged = !orgValue.equals(nValue);
                } else if (USER_KEY_LNAME.equalsIgnoreCase(key)) {
                    mLnameChanged = !orgValue.equals(nValue);
                } else if (USER_KEY_EMAIL.equalsIgnoreCase(key)) {
                    mEmailChanged = !orgValue.equalsIgnoreCase(nValue);
                }

                setupButtonVisibility();
            }
        });

        return formField;
    }

    private void makeProfileGender(int orgGenderCode) {
        PeaceFormSelectField formField = new PeaceFormSelectField(this);
        formField.setBackgroundResource(R.drawable.dr_bg_login);
        formField.setFieldTitle(R.string.strFieldTitleGender);

        List<SpinnerItem> genders = new ArrayList<>();
        String[] genderNames = strArray(R.array.arrGenders);
        int[] genderCodes = intArray(R.array.arrGenderCodes);

        int selectedPos = -1;
        for (int i = 0, l = genderNames.length; i < l; i++) {
            SpinnerItem genderItem = new SpinnerItem(genderNames[i]);
            genderItem.setId(genderCodes[i]);
            if (genderCodes[i] == orgGenderCode) selectedPos = i;
            genders.add(genderItem);
        }

        formField.setOptions(genders, new Spinner2.SimplerSpinnerItemSelectListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nGenderCode = genders.get(position).getId();
                mGenderChanged = orgGenderCode != nGenderCode;
                setupButtonVisibility();
            }
        });

        if (selectedPos != -1) {
            formField.select(selectedPos);
        }

        ViewUtils.setPaddings(formField, dp2px(15));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        ViewUtils.setMarginVertical(p, dp2px(5));
        mBinding.detailsContainer.addView(formField, p);
    }

    private void setupButtonVisibility() {
        mBinding.header.disableRightBtn(!(mFnameChanged || mLnameChanged || mEmailChanged || mGenderChanged));
    }

    private void showProgressDialog(int msgRes, boolean cancelable) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(msgRes);
        mProgressDialog.show();
        mProgressDialog.setCancelable(cancelable);
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    public void showInfoDialog(int titleRes, int msgRes) {
        showInfoDialog(titleRes, msgRes, null);
    }

    public void showInfoDialog(int titleRes, int msgRes, Runnable action) {
        hideProgressDialog();
        NotifUtils.popMsg(this, str(titleRes), str(msgRes), str(R.string.strLabelClose), action);
    }

    private void updateProfile(CurrentUser user) {
        if (!NetworkStateReceiver.canProceed(this)) return;

        final String nEmail;
        if (!TextUtils.isEmpty(emailField.getText())) nEmail = emailField.getText().toString().trim();
        else nEmail = "";

        final String nFname;
        if (!TextUtils.isEmpty(fnameField.getText())) nFname = fnameField.getText().toString().trim();
        else nFname = "";

        final String nLname;
        if (!TextUtils.isEmpty(lnameField.getText())) nLname = lnameField.getText().toString().trim();
        else nLname = "";


        if (isInvalid(this, fnameField, USER_KEY_FNAME, null) || isInvalid(this, lnameField, USER_KEY_LNAME, null) ||
                isInvalid(this, emailField, USER_KEY_EMAIL, null)) {
            return;
        }

        if (mEmailChanged) {
            AccManager.initReAuthentication(this, getSupportFragmentManager(), user, () -> {
                showProgressDialog(R.string.strTextUpdating, false);
                user.getFbUser().verifyBeforeUpdateEmail(nEmail).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = getIntent();
                        intent.putExtra(KEY_EMAIL_VERIFICATION_SENT, true);
                        intent.putExtra(KEY_NEW_EMAIL, nEmail);
                        Log.d("VERIFICATION LINK SENT");
                        updateInFB(user, nFname, nLname);
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            emailField.setFieldWarning(R.string.strMsgEmailAlreadyTaken);
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            emailField.setFieldWarning(R.string.strMsgEmailNotValid);
                        } else if (e instanceof FirebaseNetworkException) {
                            NotifUtils.popMsg(this, str(R.string.strTitleError),
                                    str(R.string.strMsgNoInternet), str(R.string.strLabelClose), null);
                        } else {
                            Log.d("FAILED TO UPDATE", e);
                            updateFailed();
                        }

                        hideProgressDialog();
                    }
                });
            });
        } else {
            showProgressDialog(R.string.strTextUpdating, false);
            updateInFB(user, nFname, nLname);
        }
    }

    private void updateInFB(CurrentUser user, String fname, String lname) {
        AccManager.updateProfileInFirestore(
                new PendingProfilesUtils(this),
                user.getFbUser(), null,
                fname, lname,
                -1, Calendar.getInstance().getTimeInMillis(),
                (OnResultReadyCallback<CurrentUser>) currentUser -> updateSucceed(user),
                e -> {
                    if (e instanceof FirebaseNetworkException) {
                        NotifUtils.popMsg(this, str(R.string.strTitleError),
                                str(R.string.strMsgNoInternet), str(R.string.strLabelClose), null);
                        hideProgressDialog();
                    } else {
                        updateFailed();
                    }

                    Log.d("FAILED TO UPDATE", e);
                }
        );
    }

    private void updateSucceed(CurrentUser user) {
        Log.d("UPDATE SUCCESS.");
        hideProgressDialog();

        Intent data = getIntent();
        data.putExtra(KEY_CURRENT_USER, user);
        setResult(Codes.REQ_CODE_PROFILE_UPDATE, data);
        finish();
    }

    private void updateFailed() {
        showInfoDialog(R.string.strTitleError, R.string.strMsgProfileUpdateFailed);
    }

    private void changePassword(CurrentUser user) {
        if (!NetworkStateReceiver.canProceed(this)) {
            return;
        }

        mChangePassFieldKey.set(USER_KEY_PASSWORD);
        mChangePassFieldTitle.set(str(R.string.strFieldTitlePassword));
        mChangePassPassword.set(null);

        Logger.print("PASSWORD-CHANGE: INIT");
        AccManager.initReAuthentication(this, getSupportFragmentManager(), user, () -> {
            Logger.print("PASSWORD-CHANGE: ASKING FOR NEW PASSWORD");
            InputBottomSheet dialog = new InputBottomSheet();
            dialog.setHeaderTitle(str(R.string.strLabelChangePassword));
            dialog.setMessage(str(R.string.strMsgNewPassword));
            dialog.setInputHint(str(R.string.strFieldHintNewPassword));
            dialog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            dialog.setButton(str(R.string.strLabelNext), this);
            dialog.show(getSupportFragmentManager(), CHANGE_PASSWORD_DIALOG_TAG);
        });
    }

    private void uploadPassword(CurrentUser user, String password) {
        Logger.print("PASSWORD-CHANGE: PROCESSING");

        showProgressDialog(R.string.strTextPleaseWait, true);
        user.getFbUser().updatePassword(password).addOnSuccessListener(unused -> {
            Logger.print("PASSWORD-CHANGE: SUCCESS");
            hideProgressDialog();
            showInfoDialog(R.string.strTitleSuccess, R.string.strMsgPasswordChanged, this::finish);
        }).addOnFailureListener(e -> {
            Logger.print("PASSWORD-CHANGE: FAILED: " + e.getMessage());

            hideProgressDialog();
            if (e instanceof FirebaseAuthWeakPasswordException) {
                showInfoDialog(R.string.strTitleFailed, R.string.strMsgWeakPassword);
            } else {
                showInfoDialog(R.string.strTitleError, R.string.strMsgPasswordChangeFailed);
                e.printStackTrace();
            }
        });
    }

    private void deleteAccount(CurrentUser user) {
        if (!NetworkStateReceiver.canProceed(this)) {
            return;
        }

        AccManager.initReAuthentication(this, getSupportFragmentManager(), user, () -> deleteConfirmation(() -> {
            showProgressDialog(R.string.strTextDeletingAccount, false);
            FirebaseUtils.firestore().collection(FIRESTORE_COLLECTION)
                    .document(user.getUid())
                    .delete()
                    .addOnSuccessListener(unused1 -> user.getFbUser().delete().addOnSuccessListener(unused -> {
                        AccManager.logoutUser(this, mAuth);
                        showInfoDialog(R.string.strTitleAccountDeleted, R.string.strMsgAccountDeleted);
                        finish();
                    }))
                    .addOnFailureListener(e -> {
                        Log.d("FAILED TO DELETE", e);
                        showInfoDialog(R.string.strTitleError, R.string.strMsgAccountDeleteFailed);
                    });
        }));
    }

    private void deleteConfirmation(Runnable runOnConfirm) {
        SpannableString msg = new SpannableString(str(R.string.strMsgDeleteAccount));
        msg.setSpan(new ForegroundColorSpan(ColorUtils.DANGER), 0, msg.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(R.string.strTitleDeleteAccount);
        builder.setMessage(msg);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setNegativeButton(R.string.strLabelDeleteAccount, ColorUtils.DANGER, (dialog, which) -> runOnConfirm.run());
        builder.setFocusOnNegative(true);
        builder.show();
    }

    @Override
    public void onButtonAction(InputBottomSheet dialog, CharSequence inputText) {
        if (mCurrentUser == null) {
            dialog.dismiss();
            return;
        }

        if (!AccManager.validatePassword(this, dialog.getInputField(), mChangePassFieldTitle.get(), mChangePassFieldKey.get(),
                mChangePassPassword.get())) {
            Logger.print("PASSWORD-CHANGE: INVALID PASSWORD");
            return;
        }

        if (USER_KEY_CPASSWORD.equals(mChangePassFieldKey.get())) {
            dialog.dismiss();
            uploadPassword(mCurrentUser, String.valueOf(inputText));
            return;
        }

        Logger.print("PASSWORD-CHANGE: ASKING FOR CONFIRMATION");

        dialog.setInputText(null);
        dialog.setMessage(str(R.string.strMsgRepeatNewPassword));
        dialog.setInputHint(str(R.string.strFieldHintCPassword));

        mChangePassFieldKey.set(USER_KEY_CPASSWORD);
        mChangePassFieldTitle.set(str(R.string.strFieldTitleCPassword));
        mChangePassPassword.set(String.valueOf(inputText));
    }
}
