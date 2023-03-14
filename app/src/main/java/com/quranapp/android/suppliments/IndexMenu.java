package com.quranapp.android.suppliments;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.google.android.material.divider.MaterialDivider;
import com.peacedesign.android.utils.AppBridge;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityAbout;
import com.quranapp.android.activities.ActivityBookmark;
import com.quranapp.android.activities.ActivityStorageCleanup;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.databinding.LytIndexMenuBinding;
import com.quranapp.android.databinding.LytIndexMenuItemBinding;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.app.InfoUtils;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.univ.PopupWindow2;
import com.quranapp.android.widgets.IconedTextView;

public class IndexMenu implements View.OnClickListener, Destroyable {
    private BaseActivity mActivity;
    private View mRootView;
    private PopupWindow2 mPopup;
    private final LayoutInflater mInflater;
    private LytIndexMenuBinding mBinding;

    public IndexMenu(BaseActivity activity, View root) {
        mActivity = activity;
        mRootView = root;
        mPopup = new PopupWindow2();
        mInflater = LayoutInflater.from(activity);
        init();
    }

    private void init() {
        AsyncLayoutInflater inflater = new AsyncLayoutInflater(mActivity);
        inflater.inflate(R.layout.lyt_index_menu, null, (view, resid, parent) -> {
            mBinding = LytIndexMenuBinding.bind(view);

            new Thread(() -> makeItems(mBinding.container)).start();

            mPopup.setContentView(view);
            setup(mBinding);
        });

        mPopup.setWidth((int) (Dimen.getWindowWidth(getContext()) * ContextKt.getFraction(getContext(),
            R.fraction.dmnIndexMenuWidth)));
        mPopup.setHeight(WRAP_CONTENT);
        mPopup.setFocusable(true);
        mPopup.setDimBehind(0.7f);
        mPopup.setClipBackground(true);

        int bgColor = ContextCompat.getColor(getContext(), R.color.colorBGIndexMenu);
        mPopup.setBackgroundDrawable(DrawableUtils.createBackground(bgColor, Dimen.dp2px(getContext(), 10)));

        mPopup.setAnimationStyle(R.style.IndexMenuAnimation);
    }

    private void makeItems(LinearLayout container) {
        Resources resources = getContext().getResources();

        TypedArray indexMenuGroupsIds = resources.obtainTypedArray(R.array.arrIndexMenuIds);
        TypedArray indexMenuGroupsIcons = resources.obtainTypedArray(R.array.arrIndexMenuIcons);
        TypedArray indexMenuGroupsLabels = resources.obtainTypedArray(R.array.arrIndexMenuLabels);

        for (int i = 0, l = indexMenuGroupsLabels.length(); i < l; i++) {
            int idsGroupId = indexMenuGroupsIds.getResourceId(i, -1);
            int iconsGroupId = indexMenuGroupsIcons.getResourceId(i, -1);
            int labelsGroupId = indexMenuGroupsLabels.getResourceId(i, -1);

            if (idsGroupId == -1 || labelsGroupId == -1 || iconsGroupId == -1) {
                continue;
            }

            TypedArray idsGroup = resources.obtainTypedArray(idsGroupId);
            TypedArray iconsGroup = resources.obtainTypedArray(iconsGroupId);
            TypedArray labelsGroup = resources.obtainTypedArray(labelsGroupId);


            makeGroupItems(container, idsGroup, iconsGroup, labelsGroup);

            idsGroup.recycle();
            iconsGroup.recycle();
            labelsGroup.recycle();

            if (i < l - 1) {
                makeDivider(container);
            }
        }

        indexMenuGroupsIds.recycle();
        indexMenuGroupsLabels.recycle();
        indexMenuGroupsIcons.recycle();
    }

    private void makeGroupItems(LinearLayout container, TypedArray idsGroup, TypedArray iconGroup, TypedArray labelGroup) {
        for (int i = 0, l = labelGroup.length(); i < l; i++) {
            int id = idsGroup.getResourceId(i, -1);
            int iconId = iconGroup.getResourceId(i, -1);
            int labelId = labelGroup.getResourceId(i, -1);
            if (id == -1 || iconId == -1 || labelId == -1) {
                continue;
            }

            makeItem(container, id, mActivity.drawable(iconId), labelId);
        }
    }

    private void makeItem(LinearLayout container, int id, Drawable icon, int labelId) {
        LytIndexMenuItemBinding binding = LytIndexMenuItemBinding.inflate(mInflater);

        IconedTextView root = binding.getRoot();
        root.setId(id);
        root.setDrawables(icon, null, null, null);
        root.setText(labelId);
        root.setOnClickListener(this);

        mActivity.runOnUiThread(() -> container.addView(root));
    }

    private void makeDivider(LinearLayout container) {
        MaterialDivider divider = new MaterialDivider(getContext());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH_PARENT, Dimen.dp2px(getContext(), 1));
        LayoutParamsKt.updateMarginVertical(p, Dimen.dp2px(getContext(), 3));

        mActivity.runOnUiThread(() -> container.addView(divider, p));
    }

    private void setup(LytIndexMenuBinding binding) {
        Context ctx = binding.getRoot().getContext();
        int mBgColor = ContextCompat.getColor(ctx, R.color.colorBGIndexMenu);
        int mBorderColor = ContextCompat.getColor(ctx, R.color.colorDividerVariable);

        int stroke = Dimen.dp2px(ctx, 1);
        float radius = Dimen.dp2px(ctx, 10);

        int[] strokeWidthsHeader = Dimen.createBorderWidthsForBG(0, 0, 0, stroke);
        float[] radiiHeader = Dimen.createRadiiForBG(radius, radius, 0, 0);
        binding.header.setBackground(
            DrawableUtils.createBackgroundStroked(mBgColor, mBorderColor, strokeWidthsHeader, radiiHeader));
        binding.close.setOnClickListener(v -> close());
    }

    public void open() {
        mPopup.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
    }

    public void close() {
        mPopup.dismiss();
    }

    public boolean isOpened() {
        return mPopup.isShowing();
    }

    public final Context getContext() {
        return mActivity;
    }

    @Override
    public void onClick(View v) {
        close();

        int id = v.getId();
        if (id == R.id.indexMenuItemBookmark) {
            mActivity.launchActivity(ActivityBookmark.class);
        } else if (id == R.id.indexMenuItemSettings) {
            mActivity.launchActivity(ActivitySettings.class);
        } else if (id == R.id.indexMenuItemStorageCleanup) {
            mActivity.launchActivity(ActivityStorageCleanup.class);
        } else if (id == R.id.indexMenuItemHelp) {
            InfoUtils.openHelp(getContext());
        } else if (id == R.id.indexMenuItemFeedback) {
            InfoUtils.openFeedbackPage(getContext());
        } else if (id == R.id.indexMenuItemPrivacy) {
            InfoUtils.openPrivacyPolicy(getContext());
        } else if (id == R.id.indexMenuItemAbout) {
            mActivity.launchActivity(ActivityAbout.class);
        } else if (id == R.id.indexMenuItemRate) {
            AppBridge.newOpener(getContext()).openPlayStore();
        } else if (id == R.id.indexMenuItemShare) {
            shareApp();
        }
    }

    private void shareApp() {
        AppBridge.Sharer sharer = AppBridge.newSharer(getContext());
        sharer.setData(mActivity.str(R.string.strMsgShareApp, AppBridge.preparePlayStoreLink(getContext(), false)))
            .setPlatform(AppBridge.Platform.SYSTEM_SHARE)
            .setChooserTitle(getContext().getString(R.string.strTitleShareApp));
        sharer.share();
    }

    @Override
    public void destroy() {
        mPopup = null;
        mActivity = null;
        mRootView = null;
        mBinding = null;
    }
}
