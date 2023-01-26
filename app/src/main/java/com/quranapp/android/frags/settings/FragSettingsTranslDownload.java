/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.quranapp.android.utils.account.AccManager.isLoggedIn;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.ACTION_NO_MORE_DOWNLOADS;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.ACTION_TRANSL_DOWNLOAD_STATUS;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_CANCELED;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED;
import static com.quranapp.android.utils.receivers.TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED;
import static com.quranapp.android.utils.univ.ExceptionCause.NO_TRANSL_LANGUAGES;
import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_TRANSL_FORCE_LOAD;
import static com.quranapp.android.utils.univ.ExceptionCause.TRANSLS_INFO_NULL;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.firebase.auth.FirebaseAuth;
import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.adapters.transl.ADPDownloadTransls;
import com.quranapp.android.adapters.transl.ADPDownloadTransls.VHDownloadTransl;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.transls.TranslBaseModel;
import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.components.transls.TranslTitleModel;
import com.quranapp.android.databinding.FragSettingsTranslBinding;
import com.quranapp.android.exc.NoInternetException;
import com.quranapp.android.interfaceUtils.TranslDownloadExplorerImpl;
import com.quranapp.android.utils.account.AccManager;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslFactory;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.receivers.TranslDownloadReceiver;
import com.quranapp.android.utils.services.TranslDownloadService;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.utils.sp.SPDownloadTrack;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.SimpleDataLoaderTask;
import com.quranapp.android.utils.univ.DateUtils;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.NotifUtils;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.widgets.PageAlert;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

public class FragSettingsTranslDownload extends FragSettingsBase implements TranslDownloadReceiver.TranslDownloadStateListener,
        ServiceConnection, AccManager.EmailVerificationListener, TranslDownloadExplorerImpl {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final CallableTaskRunner<String> mTaskRunner = new CallableTaskRunner<>(mHandler);
    private FirebaseAuth mAuth;
    private AccManager.EmailVerifyHelper mEmailVerifyHelper;
    private FragSettingsTranslBinding mBinding;
    private ADPDownloadTransls mAdapter;
    private FileUtils mFileUtils;
    private QuranTranslFactory mTranslFactory;
    private TranslDownloadReceiver mTranslDownloadReceiver;
    private TranslDownloadService mTranslDownloadService;
    private String[] mNewTransls;
    private ProgressDialog mProgressDialog;
    private PageAlert mPageAlert;

    @Override
    public String getFragTitle(Context ctx) {
        return ctx.getString(R.string.strTitleDownloadTranslations);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.frag_settings_transl;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() == null) {
            return;
        }

        mTranslDownloadReceiver = new TranslDownloadReceiver();
        mTranslDownloadReceiver.setDownloadStateListener(this);

        IntentFilter filters = new IntentFilter(ACTION_TRANSL_DOWNLOAD_STATUS);
        filters.addAction(ACTION_NO_MORE_DOWNLOADS);
        getActivity().registerReceiver(mTranslDownloadReceiver, filters);

        bindTranslService(getActivity());
    }

    private void bindTranslService(Activity actvt) {
        actvt.bindService(new Intent(actvt, TranslDownloadService.class), this, BIND_AUTO_CREATE);
    }

    private void unbindTranslService(Activity actvt) {
        // if mTranslDownloadService is null, it means the service is already unbound or it was not bound in the first place.
        if (mTranslDownloadService == null) {
            return;
        }

        try {
            actvt.unbindService(this);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mTranslDownloadReceiver != null && getActivity() != null) {
            mTranslDownloadReceiver.removeListener();
            getActivity().unregisterReceiver(mTranslDownloadReceiver);

            unbindTranslService(getActivity());
        }

        mTaskRunner.cancel();
        if (mTranslFactory != null) {
            mTranslFactory.close();
        }
    }

    @Override
    public void setupHeader(ActivitySettings activity, BoldHeader header) {
        super.setupHeader(activity, header);
        header.setCallback(new BoldHeader.BoldHeaderCallback() {
            @Override
            public void onBackIconClick() {
                activity.onBackPressed();
            }

            @Override
            public void onRightIconClick() {
                refreshTranslations(header.getContext(), true);
            }

            @Override
            public void onSearchRequest(EditText searchBox, CharSequence newText) {
                search(newText);
            }
        });

        header.setShowSearchIcon(false);
        header.setShowRightIcon(false);

        header.setSearchHint(R.string.strHintSearchTranslation);
        header.setRightIconRes(R.drawable.dr_icon_refresh, activity.getString(R.string.strLabelRefresh));
    }

    @Override
    public void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        mFileUtils = FileUtils.newInstance(ctx);
        mTranslFactory = new QuranTranslFactory(ctx);
        mBinding = FragSettingsTranslBinding.bind(view);

        Bundle args = getArgs();
        mNewTransls = args.getStringArray(TranslUtils.KEY_NEW_TRANSLATIONS);

        view.post(() -> init(ctx));
    }

    private void init(Context ctx) {
        refreshTranslations(ctx, SPAppActions.getFetchTranslsForce(ctx));
    }

    private void initPageAlert(Context ctx) {
        mPageAlert = new PageAlert(ctx);
    }

    private void refreshTranslations(Context ctx, boolean force) {
        if (!mTaskRunner.isDone()) {
            if (force) {
                mTaskRunner.cancel();
            } else {
                return;
            }
        }

        LoadTranslManifestTask loadTranslManifestTask = new LoadTranslManifestTask(ctx, force);
        if (force) {
            if (!NetworkStateReceiver.isNetworkConnected(ctx)) {
                noInternet(ctx);
                return;
            }

            showLoader();
            TranslUtils.prepareTranslsInfoUrlFB(uri -> {
                loadTranslManifestTask.setUrl(uri.toString());
                mTaskRunner.callAsync(loadTranslManifestTask);
            }, loadTranslManifestTask::onFailed);
        } else {
            mTaskRunner.callAsync(loadTranslManifestTask);
        }
    }

    private void search(CharSequence query) {
        if (mAdapter == null) return;

        List<TranslBaseModel> storedModels = mAdapter.getStoredModels();
        if (TextUtils.isEmpty(query)) {
            if (mAdapter.getItemCount() != mAdapter.getStoredItemCount()) {
                mAdapter.setModels(storedModels);
                mBinding.list.setAdapter(mAdapter);
            }
            return;
        }

        Pattern pattern = Pattern.compile(StringUtils.escapeRegex(String.valueOf(query)), CASE_INSENSITIVE | DOTALL);

        List<TranslBaseModel> found = new ArrayList<>();
        for (TranslBaseModel model : storedModels) {
            if (model instanceof TranslTitleModel) {
                found.add(model);
            }

            if (!(model instanceof TranslModel)) {
                continue;
            }

            QuranTranslBookInfo bookInfo = ((TranslModel) model).getBookInfo();

            String bookName = bookInfo.getBookName();
            String authorName = bookInfo.getAuthorName();
            String langName = bookInfo.getLangName();


            if (TextUtils.isEmpty(bookName) && TextUtils.isEmpty(authorName) && TextUtils.isEmpty(langName)) {
                continue;
            }

            if (pattern.matcher(bookName + authorName + langName).find()) {
                found.add(model);
            }
        }

        mAdapter.setModels(found);
        mBinding.list.setAdapter(mAdapter);
    }

    private void populateTransls(Context ctx, List<TranslBaseModel> models) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(ctx);
        mBinding.list.setLayoutManager(layoutManager);

        RecyclerView.ItemAnimator itemAnimator = mBinding.list.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        mAdapter = new ADPDownloadTransls(ctx, this, models);
        mBinding.list.setAdapter(mAdapter);


        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(true);
            header.setShowRightIcon(true);
        }
    }

    private void noDownloadsAvailable(Context ctx) {
        showAlert(0, R.string.strMsgTranslNoDownloadsAvailable, R.string.strLabelRefresh,
                () -> refreshTranslations(ctx, true));
    }

    public boolean isTranslDownloaded(String slug) {
        return mTranslFactory.isTranslationDownloaded(slug);
    }

    private boolean isTranslDownloading(String slug) {
        if (mTranslDownloadService != null) {
            return mTranslDownloadService.isDownloading(slug);
        }
        return false;
    }

    @Override
    public void onDownloadAttempt(VHDownloadTransl vhTransl, View referencedView, TranslModel model) {
        if (model.isDownloading()) {
            return;
        }

        Context ctx = referencedView.getContext();
        QuranTranslBookInfo bookInfo = model.getBookInfo();

        if (isTranslDownloading(bookInfo.getSlug())) {
            model.setDownloading(true);
            mAdapter.notifyItemChanged(vhTransl.getAdapterPosition());
            return;
        }

        onDownloadCheckPoint(ctx, bookInfo);
    }

    private void onDownloadCheckPoint(Context ctx, QuranTranslBookInfo bookInfo) {
        if (!NetworkStateReceiver.canProceed(ctx)) {
            return;
        }

        if (!isLoggedIn(mAuth)) {
            AccManager.alertNotLoggedIn(ctx, this, R.string.strMsgLogin4Transl);
            return;
        }

        if (mEmailVerifyHelper == null) {
            mEmailVerifyHelper = new AccManager.EmailVerifyHelper(ctx, mAuth.getCurrentUser(), this);
        }

        mAdapter.onDownloadStatus(bookInfo.getSlug(), true);
        mEmailVerifyHelper.checkEmailVerified(bool -> {
            if (bool == null) {
                failedDownloadInitiation(ctx, bookInfo.getSlug());
            } else {
                if (bool) {
                    onDownloadCheckpointFinal(ctx, bookInfo);
                } else {
                    mEmailVerifyHelper.alertEmailNotVerified();
                    mAdapter.onDownloadStatus(bookInfo.getSlug(), false);
                }
            }
        });
    }

    private void onDownloadCheckpointFinal(Context ctx, QuranTranslBookInfo bookInfo) {
        if (isTranslDownloaded(bookInfo.getSlug())) {
            onTranslDownloadStatus(bookInfo, TRANSL_DOWNLOAD_STATUS_SUCCEED);
            return;
        }

        int threshold = 3;
        int downloadsUnder24Hrs = SPDownloadTrack.getTranslDownloadsUnder24Hrs(ctx, bookInfo.getSlug());
        if (downloadsUnder24Hrs >= threshold) {
            NotifUtils.popMsg(ctx,
                    ctx.getString(R.string.strTitleThresholdReached),
                    ctx.getString(R.string.strMsgTranslDownloadThresholdReached, bookInfo.getBookName(), threshold),
                    ctx.getString(R.string.strLabelClose), null);
            return;
        }

        if (!NetworkStateReceiver.canProceed(ctx)) {
            return;
        }

        TranslDownloadService.startDownloadService((ContextWrapper) ctx, bookInfo);

        if (getActivity() != null) {
            bindTranslService(getActivity());
        }
    }

    private void failedDownloadInitiation(Context ctx, String slug) {
        NotifUtils.popMsg(ctx, ctx.getString(R.string.strTitleError),
                ctx.getString(R.string.strMsgSomethingWrong), ctx.getString(R.string.strLabelRetry), null);
        mAdapter.onDownloadStatus(slug, false);
    }

    @Override
    public void onTranslDownloadStatus(QuranTranslBookInfo bookInfo, String status) {
        mAdapter.onDownloadStatus(bookInfo.getSlug(), false);

        Context ctx = mBinding.getRoot().getContext();
        String title = null;
        String msg = null;
        switch (status) {
            case TRANSL_DOWNLOAD_STATUS_CANCELED:
                break;
            case TRANSL_DOWNLOAD_STATUS_FAILED:
                title = ctx.getString(R.string.strTitleFailed);
                msg = ctx.getString(R.string.strMsgTranslFailedToDownload, bookInfo.getBookName())
                        + " " + ctx.getString(R.string.strMsgTryLater);
                break;
            case TRANSL_DOWNLOAD_STATUS_SUCCEED:
                title = ctx.getString(R.string.strTitleSuccess);
                msg = ctx.getString(R.string.strMsgTranslDownloaded, bookInfo.getBookName());
                mAdapter.remove(bookInfo.getSlug());
                break;
        }

        if (title != null && getContext() != null) {
            NotifUtils.popMsg(getContext(), title, msg, ctx.getString(R.string.strLabelClose), null);
        }
    }

    @Override
    public void onNoMoreDownloads() {
        if (getActivity() != null) {
            unbindTranslService(getActivity());
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mTranslDownloadService = ((TranslDownloadService.TranslDownloadServiceBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mTranslDownloadService = null;
    }

    @Override
    public void preEmailVerificationSend() {
        if (mBinding == null) {
            return;
        }

        mProgressDialog = new ProgressDialog(mBinding.getRoot().getContext());
        mProgressDialog.setMessage(R.string.strTextPleaseWait);
        mProgressDialog.show();
    }

    @Override
    public void postEmailVerificationSend() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showLoader() {
        hideAlert();
        mBinding.loader.setVisibility(VISIBLE);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(false);
            header.setShowRightIcon(false);
        }
    }

    private void hideLoader() {
        mBinding.loader.setVisibility(GONE);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(true);
            header.setShowRightIcon(true);
        }
    }

    private void showAlert(int titleRes, int msgRes, int btnRes, Runnable action) {
        hideLoader();
        Context ctx = mBinding.getRoot().getContext();

        if (mPageAlert == null) {
            initPageAlert(ctx);
        }

        mPageAlert.setIcon(null);
        mPageAlert.setMessage(titleRes > 0 ? ctx.getString(titleRes) : "", ctx.getString(msgRes));
        mPageAlert.setActionButton(btnRes, action);
        mPageAlert.show(mBinding.container);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(false);
            header.setShowRightIcon(true);
        }
    }

    private void hideAlert() {
        if (mPageAlert == null) {
            return;
        }
        mPageAlert.remove();
    }

    private void noInternet(Context ctx) {
        if (mPageAlert == null) {
            initPageAlert(ctx);
        }
        mPageAlert.setupForNoInternet(() -> refreshTranslations(ctx, true));
        mPageAlert.show(mBinding.container);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(false);
            header.setShowRightIcon(true);
        }
    }

    private class LoadTranslManifestTask extends SimpleDataLoaderTask {
        private final LinkedList<TranslBaseModel> translItems = new LinkedList<>();
        private final Context mCtx;
        private final boolean mForce;

        public LoadTranslManifestTask(Context ctx, boolean force) {
            super(null);
            mCtx = ctx;
            mForce = force;
        }

        @Override
        public void preExecute() {
            showLoader();
        }

        @Nullable
        @Override
        public String call() throws Exception {
            File storedAvailableDownloads = mFileUtils.getTranslsManifestFile();

            String data;
            if (mForce) {
                data = loadUrl();
            } else {
                if (!storedAvailableDownloads.exists()) {
                    throw new Exception(REQUIRE_TRANSL_FORCE_LOAD);
                }

                try {
                    data = mFileUtils.readFile(storedAvailableDownloads);

                    if (TextUtils.isEmpty(data)) {
                        throw new Exception(REQUIRE_TRANSL_FORCE_LOAD);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new Exception(REQUIRE_TRANSL_FORCE_LOAD);
                }
            }

            if (TextUtils.isEmpty(data)) {
                throw new NullPointerException(TRANSLS_INFO_NULL);
            }

            JSONObject object = new JSONObject(data);
            JSONObject translations = object.getJSONObject("translations");
            JSONArray langs = translations.names();

            if (langs == null) {
                throw new NullPointerException(NO_TRANSL_LANGUAGES);
            }

            for (int langIndex = 0, l = langs.length(); langIndex < l; langIndex++) {
                String langCode = langs.getString(langIndex);
                JSONObject slugsOfLangCode = translations.getJSONObject(langCode);
                JSONArray slugs = slugsOfLangCode.names();

                if (slugs == null) {
                    continue;
                }

                TranslTitleModel translTitleModel = new TranslTitleModel(langCode, null);
                translItems.add(translTitleModel);

                int traceAddedTranslCount = 0;
                for (int slugIndex = 0, l2 = slugs.length(); slugIndex < l2; slugIndex++) {
                    String slug = slugs.getString(slugIndex);
                    TranslModel model = readTranslInfo(langCode, slug, slugsOfLangCode.getJSONObject(slug));
                    QuranTranslBookInfo bookInfo = model.getBookInfo();

                    final boolean isAlreadyDownloaded;

                    isAlreadyDownloaded = isTranslDownloaded(bookInfo.getSlug());

                    if (ArrayUtils.contains(mNewTransls, bookInfo.getSlug())) {
                        model.addMiniInfo(mCtx.getString(R.string.strLabelNew));
                    }

                    if (/*isUpdatable ||*/ !isAlreadyDownloaded) {
                        model.setDownloading(isTranslDownloading(bookInfo.getSlug()));

                        translTitleModel.setLangName(bookInfo.getLangName());

                        translItems.add(model);
                        traceAddedTranslCount++;
                    }
                }

                // If no translation was added in this language category, then remove the language title item
                if (traceAddedTranslCount == 0) {
                    translItems.removeLast();
                }
            }

            mFileUtils.createFile(storedAvailableDownloads);
            mFileUtils.writeToFile(storedAvailableDownloads, data);
            return null;
        }

        public TranslModel readTranslInfo(String langCode, String slug, JSONObject translObject) {
            QuranTranslBookInfo bookInfo = new QuranTranslBookInfo(slug);
            bookInfo.setLangCode(langCode);
            bookInfo.setBookName(translObject.optString("book", ""));
            bookInfo.setAuthorName(translObject.optString("author", ""));
            bookInfo.setDisplayName(translObject.optString("displayName", ""));
            bookInfo.setLangName(translObject.optString("langName", ""));
            bookInfo.setPremium(translObject.optBoolean("isPremium", false));
            bookInfo.setLastUpdated(translObject.optLong("lastUpdated", -1));
            bookInfo.setDownloadPath(translObject.optString("downloadPath", ""));
            return new TranslModel(bookInfo);
        }

        private boolean isUpdatable(TranslModel model) {
            QuranTranslBookInfo oldTranslBookInfo = mTranslFactory.getTranslationBookInfo(model.getBookInfo().getSlug());

            boolean isUpdatable = false;
            if (oldTranslBookInfo.getLastUpdated() != -1 && model.getBookInfo().getLastUpdated() != -1) {
                isUpdatable = DateUtils.isNewer(oldTranslBookInfo.getLastUpdated(), model.getBookInfo().getLastUpdated());
                if (isUpdatable) {
                    model.addMiniInfo(mCtx.getString(R.string.strLabelUpdate));
                }
            }
            return isUpdatable;
        }

        public String loadUrl() throws Exception {
            if (!NetworkStateReceiver.isNetworkConnected(mCtx)) {
                throw new NoInternetException();
            }
            return super.call();
        }

        @Override
        public void onComplete(String result) {
            if (translItems.size() > 0) {
                populateTransls(mCtx, translItems);
            } else {
                noDownloadsAvailable(mCtx);
            }
            SPAppActions.setFetchTranslsForce(mCtx, false);
            hideLoader();
        }

        @Override
        public void onFailed(@NonNull Exception e) {
            super.onFailed(e);

            // It may be caused by the migration, so we nee to update the manifest file
            if (e.getCause() instanceof JSONException) {
                //noinspection ResultOfMethodCallIgnored
                mFileUtils.getTranslsManifestFile().delete();
                refreshTranslations(mCtx, true);
                return;
            } else if (e instanceof CancellationException) {
                hideLoader();
                return;
            } else if (e instanceof NoInternetException || e.getCause() instanceof NoInternetException) {
                noInternet(mCtx);
                return;
            }

            if (e.getMessage() != null && e.getMessage().contains(REQUIRE_TRANSL_FORCE_LOAD)) {
                refreshTranslations(mCtx, true);
            } else {
                showAlert(R.string.strTitleOops, R.string.strMsgTranslLoadFailed, R.string.strLabelRetry,
                        () -> refreshTranslations(mCtx, true));
            }
        }
    }
}
