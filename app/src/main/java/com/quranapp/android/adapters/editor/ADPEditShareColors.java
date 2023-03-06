/*
 * (c) Faisal Khan. Created on 7/2/2022.
 */

/*
 * (c) Faisal Khan. Created on 26/9/2021.
 */

package com.quranapp.android.adapters.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.frags.editshare.FragEditorColors;
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect;

public class ADPEditShareColors extends RecyclerView.Adapter<ADPEditShareColors.VHColor> {
    private final int[] colors = {0xFFFFFFFF, 0xFF000000, 0xFF151515, 0xFFDCE310, 0xFF7CC1D6, 0xFF470500, 0xFF003B13, 0xFF286159, 0xFFff8b82, 0xFFff96f4, 0xFF012a36, 0xFF40083a};
    private final FragEditorColors mFragColors;

    public ADPEditShareColors(FragEditorColors fragColors) {
        mFragColors = fragColors;
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    @NonNull
    @Override
    public VHColor onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VHColor(new ColorView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull VHColor holder, int position) {
        holder.bind(colors[position]);
    }

     class VHColor extends RecyclerView.ViewHolder {
        public VHColor(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(int color) {
            itemView.setBackgroundColor(color);
            itemView.setOnClickListener(v -> mFragColors.onColorSelect(color));
        }
    }

    public static class ColorView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int normalColor;
        private final int activeColor;
        private int bgColor = Color.TRANSPARENT;
        private boolean selected = false;

        public ColorView(@NonNull Context context) {
            super(context);
            this.normalColor = ContextCompat.getColor(context, R.color.colorDivider);
            this.activeColor = ContextCompat.getColor(context, R.color.colorPrimary);

            init();
        }

        private void init() {
            setBackgroundResource(R.drawable.dr_bg_editshare_color);
            setClipToOutline(true);

            int dimen = Dimen.dp2px(getContext(), 50);
            setLayoutParams(new ViewGroup.LayoutParams(dimen, dimen));

            setOnTouchListener(new HoverPushOpacityEffect());
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.FILL);

            paint.setShader(null);
            paint.setColor(bgColor);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);

            if (selected) {
                paint.setStrokeWidth(16);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);
            }

            paint.setStrokeWidth(selected ? 8 : 4);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(selected ? activeColor : normalColor);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);
        }

        @Override
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void setBackgroundColor(int color) {
            this.bgColor = color;
            invalidate();
        }
    }
}
