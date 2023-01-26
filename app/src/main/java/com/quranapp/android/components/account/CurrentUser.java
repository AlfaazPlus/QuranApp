/*
 * (c) Faisal Khan. Created on 28/1/2022.
 */

package com.quranapp.android.components.account;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.quranapp.android.R;
import com.quranapp.android.utils.fb.FirebaseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CurrentUser implements Parcelable {
    public static final String SP_CURRENT_USER = "sp_current_user";
    public static final String FIRESTORE_COLLECTION = FirebaseUtils.FIRESTORE_USERS_COLLECTION;
    public static final String USER_KEY_UID = "uid";
    public static final String USER_KEY_REGISTERED_EMAIL = "registeredEmail";
    public static final String USER_KEY_EMAIL = "email";
    public static final String USER_KEY_FNAME = "fname";
    public static final String USER_KEY_LNAME = "lname";
    public static final String USER_KEY_GENDER_CODE = "genderCode";
    public static final String USER_KEY_JOINED = "joined";
    public static final String USER_KEY_LAST_MODIFIED = "lastModified";
    public static final String USER_KEY_PASSWORD = "password";
    public static final String USER_KEY_CPASSWORD = "cpassword";
    private final FirebaseUser fbUser;
    private final String registeredEmail;
    private final String fname;
    private final String lname;
    private final int genderCode;
    private final long joined;
    private final long lastModified;

    public CurrentUser(FirebaseUser fbUser, String registeredEmail, String fname, String lname,
                       int genderCode, long joined, long lastModified) {
        this.fbUser = fbUser;
        this.registeredEmail = registeredEmail;
        this.fname = fname;
        this.lname = lname;
        this.genderCode = genderCode;
        this.joined = joined;
        this.lastModified = lastModified;
    }

    protected CurrentUser(Parcel in) {
        fbUser = in.readParcelable(FirebaseUser.class.getClassLoader());
        registeredEmail = in.readString();
        fname = in.readString();
        lname = in.readString();
        genderCode = in.readInt();
        joined = in.readLong();
        lastModified = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(fbUser, flags);
        dest.writeString(registeredEmail);
        dest.writeString(fname);
        dest.writeString(lname);
        dest.writeInt(genderCode);
        dest.writeLong(joined);
        dest.writeLong(lastModified);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CurrentUser> CREATOR = new Creator<CurrentUser>() {
        @Override
        public CurrentUser createFromParcel(Parcel in) {
            return new CurrentUser(in);
        }

        @Override
        public CurrentUser[] newArray(int size) {
            return new CurrentUser[size];
        }
    };

    public FirebaseUser getFbUser() {
        return fbUser;
    }

    public String getUid() {
        return fbUser.getUid();
    }

    public String getRegisteredEmail() {
        return registeredEmail;
    }

    public String getEmail() {
        return fbUser.getEmail();
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getFullname() {
        return TextUtils.concat(getFname(), " ", getLname()).toString();
    }

    public int getGenderCode() {
        return genderCode;
    }

    public String getGender(Context ctx) {
        switch (genderCode) {
            case 0: return ctx.getString(R.string.strGenderMale);
            case 1: return ctx.getString(R.string.strGenderFemale);
            default: return ctx.getString(R.string.strGenderNotSpecified);
        }
    }

    public long getJoined() {
        return joined;
    }

    public long getLastModified() {
        return lastModified;
    }

    @NonNull
    @Override
    public String toString() {
        return "CurrentUser{" +
                "fname='" + fname + '\'' +
                ", lname='" + lname + '\'' +
                ", genderCode=" + genderCode +
                ", joined=" + joined +
                ", lastModified=" + lastModified +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrentUser)) return false;
        CurrentUser that = (CurrentUser) o;
        return getGenderCode() == that.getGenderCode()
                && getJoined() == that.getJoined()
                && getLastModified() == that.getLastModified()
                && getEmail().equals(that.getEmail())
                && getFname().equals(that.getFname())
                && getLname().equals(that.getLname());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFbUser(), getEmail(), getFname(), getLname(), getGenderCode(), getJoined(), getLastModified());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(USER_KEY_UID, getUid());
        map.put(USER_KEY_EMAIL, getEmail());
        map.put(USER_KEY_FNAME, getFname());
        map.put(USER_KEY_LNAME, getLname());
        map.put(USER_KEY_GENDER_CODE, getGenderCode());
        return map;
    }
}
