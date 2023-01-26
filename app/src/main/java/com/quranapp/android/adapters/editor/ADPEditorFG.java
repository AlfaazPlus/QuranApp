/*
 * (c) Faisal Khan. Created on 5/2/2022.
 */

package com.quranapp.android.adapters.editor;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.touchutils.HoverPushOpacityEffect;
import com.quranapp.android.components.editor.EditorFG;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.widgets.editor.EditorFGView;

public class ADPEditorFG extends RecyclerView.Adapter<ADPEditorFG.VHEditorFG> {
    private final VerseEditor mEditor;
    private int mSelected;

    public ADPEditorFG(VerseEditor editor) {
        mEditor = editor;
    }

    @Override
    public int getItemCount() {
        return mEditor.getFGs().size();
    }

    @NonNull
    @Override
    public VHEditorFG onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VHEditorFG(new EditorFGView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull VHEditorFG holder, int position) {
        holder.bind(mEditor.getFGs().get(position));
    }

    public class VHEditorFG extends RecyclerView.ViewHolder {
        private final EditorFGView mEditorFGView;

        public VHEditorFG(@NonNull EditorFGView editorFGView) {
            super(editorFGView);
            mEditorFGView = editorFGView;
            editorFGView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, Dimen.dp2px(editorFGView.getContext(), 120)));
            editorFGView.setOnTouchListener(new HoverPushOpacityEffect());
        }

        public void bind(EditorFG fg) {
            mEditorFGView.setColors(fg.getColors());
            mEditorFGView.setSelected(mSelected == getAdapterPosition());
            mEditorFGView.setOnClickListener(v -> {
                if (mSelected == getAdapterPosition()) {
                    shuffle(fg);
                    return;
                }

                select(getAdapterPosition());
            });
        }

        private void shuffle(EditorFG fg) {
            fg.shuffle();
            notifyItemChanged(getAdapterPosition());
            mEditor.getListener().onFGChange(fg);
        }
    }

    public void select(int position) {
        mEditor.getListener().onFGChange(mEditor.getFGs().get(position));

        int tmpSelected = mSelected;
        mSelected = position;
        if (tmpSelected >= 0) {
            notifyItemChanged(tmpSelected);
        }
        notifyItemChanged(mSelected);
    }

    public int getSelectedItemPos() {
        return mSelected;
    }
}
