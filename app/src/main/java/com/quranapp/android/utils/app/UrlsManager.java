/*
 * (c) Faisal Khan. Created on 21/1/2022.
 */

package com.quranapp.android.utils.app;

import static com.quranapp.android.utils.app.AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.fb.FirebaseUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.SimpleDataLoaderTask;
import com.quranapp.android.utils.univ.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CancellationException;

public class UrlsManager {
    public static final String DIR_NAME_4_URLS = FileUtils.createPath(BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "urls");
    public static final String URLS_FILE_NAME = "urls.json";

    public static final String URL_KEY_FEEDBACK = "feedback";
    public static final String URL_KEY_PRIVACY_POLICY = "privacy-policy";
    public static final String URL_KEY_ABOUT = "about";
    public static final String URL_KEY_HELP = "help";

    private final CallableTaskRunner<String> mTaskRunner = new CallableTaskRunner<>();
    private final FileUtils mFileUtils;
    private final Context mContext;
    private JSONObject mUrlsObj;
    private boolean mCancelled;

    public UrlsManager(Context context) {
        mFileUtils = FileUtils.newInstance(context);
        mContext = context;
    }

    public static UrlsManager newInstance(Context context) {
        return new UrlsManager(context);
    }

    public File getUrlsFile() {
        File urlsDir = mFileUtils.makeAndGetAppResourceDir(DIR_NAME_4_URLS);
        return new File(urlsDir, URLS_FILE_NAME);
    }

    public void getUrlsJson(Context ctx,
                            OnResultReadyCallback<JSONObject> readyCallback,
                            OnResultReadyCallback<Exception> failedCallback) {
        if (mUrlsObj != null) {
            readyCallback.onReady(mUrlsObj);
            return;
        }

        boolean forceUrlsDownload = SPAppActions.getFetchUrlsForce(ctx);

        File urlsFile = getUrlsFile();
        if (!forceUrlsDownload && (urlsFile.exists() && urlsFile.length() > 0)) {
            prepareUrlsObj(urlsFile, null, readyCallback, failedCallback);
        } else {
            if (!urlsFile.exists() && !mFileUtils.createFile(urlsFile)) {
                if (failedCallback != null) {
                    failedCallback.onReady(new IOException("Could not create urlsFile."));
                }
                return;
            }


            if (!NetworkStateReceiver.canProceed(ctx)) {
                return;
            }

            OnFailureListener failureListener = e -> {
                SPAppActions.addToPendingAction(mContext, AppActions.APP_ACTION_URLS_UPDATE, null);
                if (failedCallback != null) {
                    failedCallback.onReady(e);
                }
            };

            getDownloadUrl().addOnSuccessListener(uri -> {
                if (mCancelled) {
                    mCancelled = false;
                    failureListener.onFailure(new CancellationException("Canceled by the user."));
                    return;
                }

                mTaskRunner.callAsync(new SimpleDataLoaderTask(uri.toString()) {
                    @Override
                    public void onComplete(String result) {
                        prepareUrlsObj(urlsFile, result, readyCallback, failedCallback);
                    }

                    @Override
                    public void onFailed(@NonNull Exception e) {
                        failureListener.onFailure(e);
                    }
                });
            }).addOnFailureListener(failureListener);
        }
    }

    private void prepareUrlsObj(File urlsFile, String urlsData,
                                OnResultReadyCallback<JSONObject> readyCallback,
                                OnResultReadyCallback<Exception> failedCallback) {
        try {
            if (urlsData == null) {
                urlsData = mFileUtils.readFile(urlsFile);
            } else {
                mFileUtils.writeToFile(urlsFile, urlsData);
            }

            mUrlsObj = new JSONObject(urlsData);
            readyCallback.onReady(mUrlsObj);
            SPAppActions.setFetchUrlsForce(mContext, false);

        } catch (JSONException | IOException e) {
            SPAppActions.addToPendingAction(mContext, AppActions.APP_ACTION_URLS_UPDATE, null);
            if (failedCallback != null) {
                failedCallback.onReady(e);
            }
        }
    }

    private Task<Uri> getDownloadUrl() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(FirebaseUtils.STORAGE_BUCKET);

        StorageReference pathReference = storageRef.child("other/urls.json");

        return pathReference.getDownloadUrl();
    }

    public void updateUrls() {
        File urlsFile = getUrlsFile();
        if (!mFileUtils.createFile(urlsFile)) {
            SPAppActions.addToPendingAction(mContext, AppActions.APP_ACTION_URLS_UPDATE, null);
            return;
        }

        getDownloadUrl()
                .addOnSuccessListener(uri -> mTaskRunner.callAsync(new SimpleDataLoaderTask(uri.toString()) {
                    @Override
                    public void onComplete(String result) {
                        try {
                            mFileUtils.writeToFile(urlsFile, result);
                            SPAppActions.removeFromPendingAction(mContext, AppActions.APP_ACTION_URLS_UPDATE, null);
                        } catch (IOException e) {
                            SPAppActions.addToPendingAction(mContext, AppActions.APP_ACTION_URLS_UPDATE, null);
                            e.printStackTrace();
                        }
                    }
                }))
                .addOnFailureListener(e -> {
                    SPAppActions.addToPendingAction(mContext, AppActions.APP_ACTION_URLS_UPDATE, null);
                    e.printStackTrace();
                });
    }

    public void updateIfRightUrlMissing(String url) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setUseCaches(false);
                conn.setConnectTimeout(100000);
                conn.setAllowUserInteraction(false);
                conn.connect();

                if (conn.getResponseCode() != 200) {
                    updateUrls();
                }
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void cancel() {
        mCancelled = true;
        mTaskRunner.cancel();
    }
}
