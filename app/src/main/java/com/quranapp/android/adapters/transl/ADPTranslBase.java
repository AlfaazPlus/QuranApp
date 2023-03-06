/*
 * Created by Faisal Khan on (c) 22/8/2021.
 */

package com.quranapp.android.adapters.transl;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.components.transls.TranslBaseModel;
import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.components.transls.TranslTitleModel;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ADPTranslBase<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected final LayoutInflater mInflater;
    private final int mColorPrimary;

    protected final List<TranslBaseModel> mModelsStored;
    protected List<TranslBaseModel> mModelsVisible;

    public ADPTranslBase(Context context, List<TranslBaseModel> items) {
        mInflater = LayoutInflater.from(context);
        mColorPrimary = ContextCompat.getColor(context, R.color.colorPrimary);

        resetPositions(items);
        mModelsStored = new ArrayList<>(items);
        mModelsVisible = items;
        setHasStableIds(true);
    }

    private void resetPositions(List<TranslBaseModel> models) {
        for (int i = 0, l = models.size(); i < l; i++) {
            models.get(i).setPosition(i);
        }
    }


    public List<TranslBaseModel> getStoredModels() {
        return mModelsStored;
    }

    public void setModels(List<TranslBaseModel> models) {
        mModelsVisible = new ArrayList<>(models);
        resetPositions(mModelsVisible);
    }

    @Override
    public int getItemCount() {
        return mModelsVisible.size();
    }

    public int getStoredItemCount() {
        return mModelsStored.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mModelsVisible.get(position) instanceof TranslTitleModel ? 0 : 1;
    }

    protected AppCompatTextView createTitleView(Context context) {
        AppCompatTextView titleView = new AppCompatTextView(context);
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

        ViewPaddingKt.updatePaddingHorizontal(titleView, Dimen.dp2px(context, 20));

        titleView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        titleView.setTextColor(mColorPrimary);

        ViewGroup.MarginLayoutParams p = new ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LayoutParamsKt.updateMarginVertical(p, Dimen.dp2px(context, 10));
        titleView.setLayoutParams(p);

        return titleView;
    }

    public void remove(String slug) {
        for (TranslBaseModel storedModel : mModelsStored) {
            if (storedModel instanceof TranslModel) {
                if (Objects.equals(slug, ((TranslModel) storedModel).getBookInfo().getSlug())) {
                    mModelsStored.remove(storedModel);
                    break;
                }
            }
        }

        int removedPos = -1;
        for (int i = 0, size = mModelsVisible.size(); i < size; i++) {
            TranslBaseModel visibleModel = mModelsVisible.get(i);
            if (visibleModel instanceof TranslModel) {
                if (Objects.equals(slug, ((TranslModel) visibleModel).getBookInfo().getSlug())) {
                    mModelsVisible.remove(visibleModel);
                    removedPos = i;
                    break;
                }
            }
        }

        resetPositions(mModelsStored);
        resetPositions(mModelsVisible);

        if (removedPos != -1) {
            notifyItemRemoved(removedPos);
        }
    }

    public TranslBaseModel getItemVisible(int position) {
        return mModelsVisible.get(position);
    }
}
