/*
 * (c) Faisal Khan. Created on 7/2/2022.
 */

package com.quranapp.android.widgets.editor;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.utils.extensions.ContextKt;

public class EditorBGView extends View {
    private final int mIconSize;
    private int bgType;
    private final Path path = new Path();
    private Bitmap mIconBitmap;
    private Paint mIconPaint;
    private Paint mIconBGPaint;
    private Paint mBGPaint;
    private int[] colors = {};
    private Drawable mImageDrawable;
    private Bitmap mImage;
    private boolean mSelected;
    private final int mPrimaryColor;
    private final Rect mImageRect = new Rect();

    public EditorBGView(Context context) {
        super(context);
        mPrimaryColor = ContextKt.obtainPrimaryColor(getContext());
        mIconSize = Dimen.dp2px(context, 20);
        generateIconPaint(R.drawable.dr_icon_opacity);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(path);

        if (bgType == VerseEditor.BG_TYPE_COLORS) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), mBGPaint);
        } else if (bgType == VerseEditor.BG_TYPE_IMAGE) {
            if (mImage == null) {
                mImage = DrawableUtils.getBitmapFromDrawable(mImageDrawable, getWidth(), getHeight());
            }
            canvas.getClipBounds(mImageRect);
            canvas.drawBitmap(mImage, null, mImageRect, mBGPaint);
        }

        if ((mSelected) && mIconBitmap != null) {
            int iconWidth = mIconBitmap.getWidth();
            int iconHeight = mIconBitmap.getHeight();

            mIconBGPaint.setStyle(Paint.Style.FILL);
            mIconBGPaint.setColor(Color.WHITE);
            mIconBGPaint.setShadowLayer(4, 0, 1.5f, ColorUtils.createAlphaColor(Color.BLACK, .6f));
            int circleR = (iconWidth / 2) + 6;
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

        if (bgType == VerseEditor.BG_TYPE_COLORS && colors.length > 0) {
            if (colors.length > 1) {
                LinearGradient gradient = new LinearGradient(0, 0, getWidth(), getHeight(), colors, null, Shader.TileMode.CLAMP);
                mBGPaint.setShader(gradient);
            } else {
                mBGPaint.setColor(colors[0]);
            }
        } /*else if (bgType == VerseEditor.BG_TYPE_IMAGE) {
            try {
                mImage = Bitmap.createBitmap(mImage, 0, 0, getWidth(), getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
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
        regeneratePaints();
        resetPath();
        invalidate();
    }

    public void setImage(Drawable d) {
        bgType = VerseEditor.BG_TYPE_IMAGE;
        mImageDrawable = d;
        mImage = null;
        reset();
    }

    public void setColors(int[] colors) {
        bgType = VerseEditor.BG_TYPE_COLORS;
        this.colors = colors;
        mImage = null;
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