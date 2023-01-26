/*
 * (c) Faisal Khan. Created on 28/1/2022.
 */

package com.quranapp.android.utils.sp;

import static com.quranapp.android.utils.app.AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.SetOptions;
import com.quranapp.android.components.account.CurrentUser;
import com.quranapp.android.utils.fb.FirebaseUtils;
import com.quranapp.android.utils.univ.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PendingProfilesUtils {
    private static final String DIR_NAME_4_PENDING_PROFILES = FileUtils.createPath(BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
            "pending_profiles");
    private static final String SP_PENDING_PROFILES = "sp_pending_profiles";
    private static final String KEY_PENDING_PROFILES_UIDS = "key.pending_profiles_uids";
    private final FileUtils mFileUtils;
    private final Context mContext;

    public PendingProfilesUtils(Context context) {
        mContext = context;
        mFileUtils = FileUtils.newInstance(context);
    }

    public void addToPendingProfiles(String uid, Map<String, Object> profileData) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_PENDING_PROFILES, Context.MODE_PRIVATE);

        Set<String> pendingProfilesSP = sp.getStringSet(KEY_PENDING_PROFILES_UIDS, null);

        final Set<String> pendingProfiles;
        if (pendingProfilesSP != null && !pendingProfilesSP.isEmpty()) {
            pendingProfiles = new HashSet<>(pendingProfilesSP);
        } else {
            pendingProfiles = new HashSet<>();
        }

        pendingProfiles.add(uid);

        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_PENDING_PROFILES_UIDS, pendingProfiles);
        editor.apply();

        JSONObject profileObject = new JSONObject();
        profileData.forEach((key, object) -> {
            try {
                profileObject.put(key, object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        String profileDataStr = profileObject.toString();
        File pendingProfileFile = getPendingProfileFile(uid);
        boolean created = mFileUtils.createFile(pendingProfileFile);
        if (created) {
            try {
                mFileUtils.writeToFile(pendingProfileFile, profileDataStr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeFromPendingProfiles(String uid) {
        SharedPreferences sp = mContext.getSharedPreferences(SP_PENDING_PROFILES, Context.MODE_PRIVATE);

        Set<String> pendingProfiles = new HashSet<>(sp.getStringSet(KEY_PENDING_PROFILES_UIDS, new HashSet<>()));
        pendingProfiles.remove(uid);

        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(KEY_PENDING_PROFILES_UIDS, pendingProfiles);
        editor.apply();

        File pendingProfileFile = getPendingProfileFile(uid);
        if (pendingProfileFile.exists()) {
            pendingProfileFile.delete();
        }
    }

    public File getPendingProfileFile(String uid) {
        File pendingProfilesDir = mFileUtils.makeAndGetAppResourceDir(DIR_NAME_4_PENDING_PROFILES);
        return new File(pendingProfilesDir, uid + ".json");
    }

    public void uploadPendingProfiles() {
        SharedPreferences sp = mContext.getSharedPreferences(SP_PENDING_PROFILES, Context.MODE_PRIVATE);

        Set<String> pendingProfiles = sp.getStringSet(KEY_PENDING_PROFILES_UIDS, new HashSet<>());
        for (String uid : pendingProfiles) {
            File pendingProfileFile = getPendingProfileFile(uid);
            if (pendingProfileFile.length() > 0) {
                try {
                    uploadPendingProfile(uid, pendingProfileFile);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadPendingProfile(String uid, File pendingProfileFile) throws IOException, JSONException {
        String pendingProfileData = mFileUtils.readFile(pendingProfileFile);
        JSONObject profileObject = new JSONObject(pendingProfileData);

        Map<String, Object> profile = new HashMap<>();
        profileObject.keys().forEachRemaining(key -> {
            try {
                profile.put(key, profileObject.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        FirebaseUtils.firestore().collection(CurrentUser.FIRESTORE_COLLECTION)
                .document(uid)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> removeFromPendingProfiles(uid));
    }

    public Context getContext() {
        return mContext;
    }
}
