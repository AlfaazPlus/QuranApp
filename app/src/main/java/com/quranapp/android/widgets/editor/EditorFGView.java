/*
 * (c) Faisal Khan. Created on 7/2/2022.
 */

package com.quranapp.android.widgets.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ContextKt;

public class EditorFGView extends View {
    private final int mIconSize;
    private final Path path = new Path();
    private Bitmap mIconBitmap;
    private Paint mIconPaint;
    private Paint mIconBGPaint;
    private Paint[] paints;
    private int[] colors = {};
    private boolean mSelected;
    private final int mPrimaryColor;

    public EditorFGView(Context context) {
        super(context);
        mPrimaryColor = ContextKt.obtainPrimaryColor(getContext());
        mIconSize = Dimen.dp2px(context, 20);
        generateIconPaint(R.drawable.dr_icon_shuffle);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (paints == null) {
            super.onDraw(canvas);
            return;
        }

        canvas.save();
        canvas.clipPath(path);

        int l = paints.length;
        int height = getHeight() / l;

        int top = 0;
        int currHeight = height;
        for (int i = 0; i < l; i++) {
            canvas.drawRect(0, top + (i > 0 ? 1 : 0), getWidth(), currHeight, paints[i]);
            top += height;
            currHeight += height;
        }

        if (mSelected && mIconBitmap != null) {
            int iconWidth = mIconBitmap.getWidth();
            int iconHeight = mIconBitmap.getHeight();

            mIconBGPaint.setStyle(Paint.Style.FILL);
            mIconBGPaint.setColor(Color.WHITE);
            mIconBGPaint.setShadowLayer(4, 0, 1.5f, ColorUtils.createAlphaColor(Color.BLACK, .6f));
            int circleR = (iconWidth / 2) + 10;
            int iconBGWidth = getWidth() >> 1;
            int iconBGHeight = getHeight() >> 1;
            canvas.drawCircle(iconBGWidth, iconBGHeight, circleR, mIconBGPaint);

            int iconLeft = (getWidth() - iconWidth) >> 1;
            int iconTop = (getHeight() - iconHeight) >> 1;
            canvas.drawBitmap(mIconBitmap, iconLeft, iconTop, mIconPaint);

            mIconBGPaint.setStrokeWidth(2);
            mIconBGPaint.setStyle(Paint.Style.STROKE);
            mIconBGPaint.setColor(mPrimaryColor);
            mIconBGPaint.setShadowLayer(0, 0, 0, 0);

            canvas.drawCircle(iconBGWidth, iconBGHeight, circleR, mIconBGPaint);
        }

        canvas.restore();

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        resetPath();
    }

    private void regeneratePaints() {
        paints = new Paint[colors.length];
        for (int i = 0, l = colors.length; i < l; i++) {
            Paint paint = new Paint(ANTI_ALIAS_FLAG);
            paint.setColor(colors[i]);
            paints[i] = paint;
        }
    }

    private void generateIconPaint(@DrawableRes int iconRes) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), iconRes);
        mIconBitmap = DrawableUtils.getBitmapFromDrawable(drawable, mIconSize, mIconSize);

        mIconPaint = new Paint(ANTI_ALIAS_FLAG);
        mIconPaint.setColorFilter(new PorterDuffColorFilter(mPrimaryColor, PorterDuff.Mode.SRC_ATOP));

        mIconBGPaint = new Paint(ANTI_ALIAS_FLAG);
    }

    private void resetPath() {
        path.reset();
        float cornerRadius = 20;
        path.addRoundRect(new RectF(0, 0, getWidth(), getHeight()), cornerRadius, cornerRadius, Path.Direction.CW);
        path.close();
    }

    private void reset() {
        resetPath();
        invalidate();
    }

    public void setColors(int[] colors) {
        this.colors = colors;
        regeneratePaints();
        reset();
    }

    @Override
    public boolean isSelected() {
        return mSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        mSelected = selected;
        invalidate();
    }
}