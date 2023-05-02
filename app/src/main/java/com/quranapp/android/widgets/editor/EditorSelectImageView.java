/*
 * (c) Faisal Khan. Created on 7/2/2022.
 */

package com.quranapp.android.widgets.editor;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableKt;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;

public class EditorSelectImageView extends View {
    private final int mIconSize;
    private final Path path = new Path();
    private final int mColorIcon;
    private final int mCardBGColor;
    private Bitmap mIconBitmap;
    private Paint mIconPaint;
    private Paint mBGPaint;

    public EditorSelectImageView(Context context) {
        super(context);
        mColorIcon = ContextCompat.getColor(context, R.color.colorIcon);
        mCardBGColor = ContextCompat.getColor(context, R.color.colorBackgroundCard);
        mIconSize = Dimen.dp2px(context, 30);
        generateIconPaint(R.drawable.dr_icon_add_image);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(path);

        canvas.drawRect(0, 0, getWidth(), getHeight(), mBGPaint);

        if (mIconBitmap != null) {
            int iconWidth = mIconBitmap.getWidth();
            int iconHeight = mIconBitmap.getHeight();

            int iconLeft = (getWidth() - iconWidth) >> 1;
            int iconTop = (getHeight() - iconHeight) >> 1;
            canvas.drawBitmap(mIconBitmap, iconLeft, iconTop, mIconPaint);
        }

        canvas.restore();

        super.onDraw(canvas);
    }

    @Override
    public void onMeasure(int wMeasureSpec, int hMeasureSpec) {
        super.onMeasure(wMeasureSpec, wMeasureSpec);
        reset();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldW);
        reset();
    }

    private void regeneratePaints() {
        mBGPaint = new Paint(ANTI_ALIAS_FLAG);
        mBGPaint.setColor(mCardBGColor);
    }

    private void generateIconPaint(@DrawableRes int iconRes) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), iconRes);
        mIconBitmap = DrawableKt.toBitmap(drawable, mIconSize, mIconSize, null);

        mIconPaint = new Paint(ANTI_ALIAS_FLAG);
        mIconPaint.setColorFilter(new PorterDuffColorFilter(mColorIcon, PorterDuff.Mode.SRC_ATOP));
    }

    private void resetPath() {
        path.reset();
        float cornerRadius = 20;
        path.addRoundRect(new RectF(0, 0, getWidth(), getHeight()), cornerRadius, cornerRadius, Path.Direction.CW);
        path.close();
    }

    private void reset() {
        regeneratePaints();
        resetPath();
        invalidate();
    }
}