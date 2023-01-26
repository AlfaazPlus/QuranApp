/*
 * (c) Faisal Khan. Created on 29/1/2022.
 */

package com.quranapp.android.utils.account;

import static com.quranapp.android.components.account.CurrentUser.FIRESTORE_COLLECTION;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_CPASSWORD;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_EMAIL;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_FNAME;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_GENDER_CODE;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_JOINED;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_LAST_MODIFIED;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_LNAME;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_PASSWORD;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_REGISTERED_EMAIL;
import static com.quranapp.android.components.account.CurrentUser.USER_KEY_UID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.SetOptions;
import com.peacedesign.android.utils.Log;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.account.ActivityAccount;
import com.quranapp.android.components.account.CurrentUser;
import com.quranapp.android.interfaceUtils.ActivityResultStarter;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.fb.FirebaseUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.PendingProfilesUtils;
import com.quranapp.android.utils.univ.NotifUtils;
import com.quranapp.android.widgets.form.InputBottomSheet;
import com.quranapp.android.widgets.form.PeaceFormInputField;

import java.util.HashMap;
import java.util.Map;

public class AccManager {
    public static final String KEY_CURRENT_USER = "key.current_user";
    public static final String KEY_EMAIL_VERIFICATION_SENT = "key.email_verification_sent";
    public static final String KEY_NEW_EMAIL = "key.new_email";
    public static final String KEY_ACCOUNT_LANDING_PAGE = "key.account_landing_page";
    public static final int ACCOUNT_LANDING_PAGE_LOGIN = 0;
    public static final int ACCOUNT_LANDING_PAGE_CREATE = 1;

    public static boolean isLoggedIn() {
        return isLoggedIn(FirebaseAuth.getInstance());
    }

    public static boolean isLoggedIn(FirebaseAuth auth) {
        return auth.getCurrentUser() != null;
    }

    public static void alertNotLoggedIn(Context ctx, ActivityResultStarter actvtResStarter, @StringRes int serviceStrRes) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx);
        builder.setTitle(R.string.strMsgLoginFree);
        builder.setMessage(ctx.getString(R.string.strMsgLogin4Service, ctx.getString(serviceStrRes)));

        Intent intent = new Intent(ctx, ActivityAccount.class);
        builder.setNeutralButton(R.string.strTitleCreateAccount, (dialog, which) -> {
            intent.putExtra(KEY_ACCOUNT_LANDING_PAGE, ACCOUNT_LANDING_PAGE_CREATE);
            actvtResStarter.startActivity4Result(intent, null);
        });
        builder.setPositiveButton(R.string.strLabelLogin, (dialog, which) -> {
            intent.putExtra(KEY_ACCOUNT_LANDING_PAGE, ACCOUNT_LANDING_PAGE_LOGIN);
            actvtResStarter.startActivity4Result(intent, null);
        });

        builder.setFocusOnNeutral(true);
        builder.setFocusOnPositive(true);
        builder.setButtonsDirection(PeaceDialog.STACKED);
        builder.setCanceledOnTouchOutside(false);
        builder.show();
    }

    public static void initReAuthentication(Context ctx, FragmentManager fm, CurrentUser user, Runnable runOnSuccess) {
        Logger.print("RE-AUTHENTICATION: INIT");
        InputBottomSheet dialog = new InputBottomSheet();
        dialog.setHeaderTitle(ctx.getString(R.string.strTitleVerifyPassword));
        dialog.setMessage(ctx.getString(R.string.strMsgRepeatPasswordProceed));
        dialog.setInputHint(ctx.getString(R.string.strFieldHintPassword));
        dialog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        dialog.setButton(ctx.getString(R.string.strLabelProceed), (dialog1, inputText) -> {
            String password = String.valueOf(inputText);

            if (TextUtils.isEmpty(password)) {
                Logger.print("RE-AUTHENTICATION: PASSWORD EMPTY");
                dialog.setInputWarning(ctx.getString(R.string.strMsgPasswordRequired));
                return;
            }

            dialog.loader(true);
            AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.getFbUser().reauthenticate(cred).addOnSuccessListener(unused -> {
                Logger.print("RE-AUTHENTICATION: SUCCESSFUL");
                if (runOnSuccess != null) {
                    runOnSuccess.run();
                }
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Logger.print("RE-AUTHENTICATION: FAILED: " + e.getMessage());
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    dialog.setInputWarning(ctx.getString(R.string.strMsgIncorrectPassword));
                } else if (e instanceof FirebaseNetworkException) {
                    dialog.setInputWarning(ctx.getString(R.string.strMsgNoInternet));
                } else {
                    e.printStackTrace();
                    dialog.setInputWarning(ctx.getString(R.string.strMsgSomethingWrong));
                }
                dialog.loader(false);
            });
        });
        dialog.show(fm);
    }

    public static boolean validateEmail(Context ctx, PeaceFormInputField field) {
        field.setFieldWarning(null);

        String value = "";

        if (field.getText() != null) {
            value = field.getText().toString().trim();
        }

        if (TextUtils.isEmpty(value)) {
            field.setFieldWarning(R.string.strMsgEmailRequired);
            field.setFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            field.setFieldWarning(ctx.getString(R.string.strMsgEmailNotValid));
            field.setFocus();
            return false;
        }

        return true;
    }

    public static boolean validatePassword(Context ctx, PeaceFormInputField field, String title, String name, String password4Confirmation) {
        field.setFieldWarning(null);

        String value = "";

        if (field.getText() != null) {
            value = field.getText().toString().trim();
        }

        if (TextUtils.isEmpty(value)) {
            field.setFieldWarning(ctx.getString(R.string.strMsgFieldRequired, title));
            field.setFocus();
            return false;
        }

        if (name.equals(USER_KEY_PASSWORD) && value.length() < 8) {
            field.setFieldWarning(ctx.getString(R.string.strMsgMinPassword));
            field.setFocus();
            return false;
        }

        if (name.equals(USER_KEY_CPASSWORD) && !TextUtils.equals(value, password4Confirmation)) {
            field.setFieldWarning(ctx.getString(R.string.strMsgMinCPasswordUnmatched));
            field.setFocus();
            return false;
        }

        return true;
    }

    public static Map<String, Object> prepareProfileMap(String uid, String registeredEmail, String fname, String lname, long joinedTime, long updatedTime) {
        Map<String, Object> profile = new HashMap<>();
        profile.put(USER_KEY_UID, uid);
        if (!TextUtils.isEmpty(registeredEmail)) {
            profile.put(USER_KEY_EMAIL, registeredEmail);
        }
        if (!TextUtils.isEmpty(fname)) {
            profile.put(USER_KEY_FNAME, fname);
        }
        if (!TextUtils.isEmpty(lname)) {
            profile.put(USER_KEY_LNAME, lname);
        }
        //        profile.put(USER_KEY_GENDER_CODE, genderCode);
        if (joinedTime > 0) {
            profile.put(USER_KEY_JOINED, joinedTime);
        }

        profile.put(USER_KEY_LAST_MODIFIED, updatedTime);
        return profile;
    }

    public static void updateProfileInFirestore(
            PendingProfilesUtils pendingProfilesUtils,
            FirebaseUser fbUser,
            String registeredEmail,
            String fname,
            String lname,
            long joined,
            long lastModified,
            OnResultReadyCallback<CurrentUser> onSuccess,
            OnFailureListener onFail
    ) {
        Map<String, Object> profile = prepareProfileMap(fbUser.getUid(), registeredEmail, fname, lname, joined, lastModified);
        pendingProfilesUtils.addToPendingProfiles(fbUser.getUid(), profile);

        FirebaseUtils.firestore().collection(FIRESTORE_COLLECTION)
                .document(fbUser.getUid())
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    fbUser.reload();

                    updateProfileInFB(fbUser, fname, lname);
                    CurrentUser currentUser = updateUserInSP(pendingProfilesUtils.getContext(), fbUser, registeredEmail, fname, lname,
                            -1, joined, lastModified);
                    pendingProfilesUtils.removeFromPendingProfiles(fbUser.getUid());
                    if (onSuccess != null) {
                        onSuccess.onReady(currentUser);
                    }
                })
                .addOnFailureListener(e -> {
                    pendingProfilesUtils.removeFromPendingProfiles(fbUser.getUid());
                    if (onFail != null) {
                        onFail.onFailure(e);
                    }
                });
    }

    public static void updateProfileInFB(FirebaseUser currentUser, String fname, String lname) {
        UserProfileChangeRequest.Builder request = new UserProfileChangeRequest.Builder();
        CharSequence displayName = TextUtils.concat(fname, " ", lname);
        request.setDisplayName(displayName.toString());

        try {
            currentUser.updateProfile(request.build());
            currentUser.reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isInvalid(Context ctx, PeaceFormInputField field, String name, String password) {
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

        String nameRegex = "^[\\p{L}\\p{M} .'-]+$";
        if ((name.equals(USER_KEY_FNAME) && !value.matches(nameRegex)) ||
                (name.equals(USER_KEY_LNAME) && !value.matches(nameRegex))) {
            field.setFieldWarning(ctx.getString(R.string.strMsgFieldNotValid, field.getFieldTitle()));
            field.setFocus();
            return true;
        }

        if (name.equals(USER_KEY_EMAIL) && !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            field.setFieldWarning(ctx.getString(R.string.strMsgEmailNotValid));
            field.setFocus();
            return true;
        }

        if (name.equals(USER_KEY_PASSWORD) && value.length() < 8) {
            field.setFieldWarning(ctx.getString(R.string.strMsgMinPassword));
            field.setFocus();
            return true;
        }

        if (name.equals(USER_KEY_CPASSWORD) && !TextUtils.equals(value, password)) {
            field.setFieldWarning(ctx.getString(R.string.strMsgMinCPasswordUnmatched));
            field.setFocus();
            return true;
        }

        return false;
    }

    public static CurrentUser getUserFromSP(Context context, FirebaseUser fbUser) {
        SharedPreferences sp = context.getSharedPreferences(CurrentUser.SP_CURRENT_USER, Context.MODE_PRIVATE);
        String registeredEmail = sp.getString(USER_KEY_REGISTERED_EMAIL, "");
        String fname = sp.getString(USER_KEY_FNAME, "");
        String lname = sp.getString(USER_KEY_LNAME, "");
        int genderCode = sp.getInt(USER_KEY_GENDER_CODE, -1);
        long joined = sp.getLong(USER_KEY_JOINED, -1);
        long lastModified = sp.getLong(USER_KEY_LAST_MODIFIED, -1);

        return new CurrentUser(fbUser, registeredEmail, fname, lname, genderCode, joined, lastModified);
    }

    public static CurrentUser updateUserInSP(Context context, FirebaseUser fbUser, String registeredEmail, String fname, String lname,
                                             int genderCode, long joined, long lastModified) {
        if (TextUtils.isEmpty(registeredEmail)) {
            registeredEmail = "";
        }
        if (TextUtils.isEmpty(fname)) {
            fname = "";
        }
        if (TextUtils.isEmpty(lname)) {
            lname = "";
        }

        SharedPreferences.Editor editor = context.getSharedPreferences(CurrentUser.SP_CURRENT_USER, Context.MODE_PRIVATE).edit();
        editor.putString(USER_KEY_UID, fbUser.getUid());

        if (!TextUtils.isEmpty(registeredEmail)) {
            editor.putString(USER_KEY_REGISTERED_EMAIL, registeredEmail);
        }
        if (!TextUtils.isEmpty(fname)) {
            editor.putString(USER_KEY_FNAME, fname);
        }
        if (!TextUtils.isEmpty(lname)) {
            editor.putString(USER_KEY_LNAME, lname);
        }
        //        editor.putInt(USER_KEY_GENDER_CODE, genderCode);
        if (joined > 0) {
            editor.putLong(USER_KEY_JOINED, joined);
        }
        editor.putLong(USER_KEY_LAST_MODIFIED, lastModified);
        editor.apply();

        return new CurrentUser(fbUser, registeredEmail, fname, lname, genderCode, joined, lastModified);
    }

    public static void logoutUser(Context context, FirebaseAuth auth) {
        auth.signOut();
        SharedPreferences.Editor editor = context.getSharedPreferences(CurrentUser.SP_CURRENT_USER, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static void fetchProfile(Context ctx, FirebaseUser fbUser, OnResultReadyCallback<CurrentUser> readyCallback,
                                    OnFailureListener failCallback, boolean force) {
        if (!force) {
            CurrentUser userFromSP = getUserFromSP(ctx, fbUser);
            if (!TextUtils.isEmpty(userFromSP.getFname())) {
                readyCallback.onReady(userFromSP);
                return;
            }
        }

        fbUser.reload()
                .addOnSuccessListener(unused -> FirebaseUtils.firestore()
                        .collection(FIRESTORE_COLLECTION)
                        .document(fbUser.getUid())
                        .get()
                        .addOnSuccessListener(result -> {
                            String registeredEmail = result.getString(USER_KEY_REGISTERED_EMAIL);
                            String fname = result.getString(USER_KEY_FNAME);
                            String lname = result.getString(USER_KEY_LNAME);

                            int genderCode = -1;
                            Long genderCodeFB = result.getLong(USER_KEY_GENDER_CODE);
                            if (genderCodeFB != null) {
                                genderCode = genderCodeFB.intValue();
                            }

                            long joined0 = -1;
                            Long joined = result.getLong(USER_KEY_JOINED);
                            if (joined != null) {
                                joined0 = joined;
                            }

                            long lastModified0 = -1;
                            Long lastModified = result.getLong(USER_KEY_LAST_MODIFIED);
                            if (lastModified != null) {
                                lastModified0 = lastModified;
                            }

                            CurrentUser currentUser = updateUserInSP(ctx, fbUser, registeredEmail, fname, lname, genderCode,
                                    joined0, lastModified0);
                            readyCallback.onReady(currentUser);
                        })
                        .addOnFailureListener(failCallback))
                .addOnFailureListener(failCallback);
    }

    public static class EmailVerifyHelper {
        private final Context mContext;
        private boolean mVerificationEmailSent;
        private final FirebaseUser mFbUser;
        private final EmailVerificationListener mListener;

        public EmailVerifyHelper(Context context, FirebaseUser fbUser, EmailVerificationListener listener) {
            mContext = context;
            mFbUser = fbUser;
            mListener = listener;
        }

        public void checkEmailVerified(OnResultReadyCallback<Boolean> resultCallback) {
            Logger.print("CHECK EMAIL VERIFIED: INIT");
            if (mFbUser.isEmailVerified()) {
                Logger.print("EMAIL VERIFIED: true");
                resultCallback.onReady(true);
            } else {
                mFbUser.reload()
                        .addOnSuccessListener(unused -> {
                            Logger.print("EMAIL VERIFIED: " + mFbUser.isEmailVerified());
                            resultCallback.onReady(mFbUser.isEmailVerified());
                        })
                        .addOnFailureListener(e -> {
                            Logger.print("CHECK EMAIL VERIFIED: FAILED");
                            resultCallback.onReady(null);
                        });
            }
        }

        public void alertEmailNotVerified() {
            PeaceDialog.Builder builder = PeaceDialog.newBuilder(mContext);
            builder.setTitle(R.string.strMsgVerifyEmail);
            builder.setMessage(R.string.strMsgVerifyEmailLong);

            builder.setPositiveButton(R.string.strLabelSendEmail, (dialog, which) -> initVerification());

            builder.setCanceledOnTouchOutside(false);
            builder.setFocusOnPositive(true);
            builder.show();
        }

        public void initVerification() {
            if (!NetworkStateReceiver.canProceed(mContext)) {
                return;
            }

            preEmailSendVerification();
            mFbUser.sendEmailVerification().addOnSuccessListener(unused -> {
                postEmailSendVerification();
                notifyLinkSent(mFbUser.getEmail(), null);
            }).addOnFailureListener(e -> {
                if (e instanceof FirebaseNetworkException) {
                    NotifUtils.popMsg(mContext, mContext.getString(R.string.strTitleError),
                            mContext.getString(R.string.strMsgNoInternet), mContext.getString(R.string.strLabelClose), null);
                } else {
                    NotifUtils.popMsg(mContext, mContext.getString(R.string.strTitleOops),
                            mContext.getString(R.string.strMsgSomethingWrong), mContext.getString(R.string.strLabelClose), null);
                }

                Log.d(e);
                postEmailSendVerification();
            });
        }

        public void notifyLinkSent(String email, Runnable btnAction) {
            mVerificationEmailSent = true;
            String title = mContext.getString(R.string.strTitleEmailSent);
            String msg = mContext.getString(R.string.strMsgEmailSentVerifyEmail, email);
            String btn = mContext.getString(R.string.strLabelOkay);
            NotifUtils.popMsg(mContext, title, msg, btn, btnAction);
        }

        public boolean isVerificationEmailSent() {
            return mVerificationEmailSent;
        }

        private void preEmailSendVerification() {
            if (mListener == null) {
                return;
            }
            mListener.preEmailVerificationSend();
        }

        private void postEmailSendVerification() {
            if (mListener == null) {
                return;
            }
            mListener.postEmailVerificationSend();
        }
    }

    public interface EmailVerificationListener {
        void preEmailVerificationSend();

        void postEmailVerificationSend();
    }
}
