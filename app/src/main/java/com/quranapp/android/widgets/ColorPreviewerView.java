package com.quranapp.android.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.quranapp.android.R;


public class ColorPreviewerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int normalColor;
    private final int activeColor;
    private int bgColor = Color.TRANSPARENT;
    private boolean selected;
    private BitmapShader bitmapShader;

    public ColorPreviewerView(@NonNull Context context) {
        this(context, null);
    }

    public ColorPreviewerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPreviewerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        normalColor = ContextCompat.getColor(context, R.color.colorDividerContrast);
        activeColor = ContextCompat.getColor(context, R.color.colorPrimary);
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.dr_bg_cornered_preview);
        setClipToOutline(true);
        BitmapDrawable drawable = (BitmapDrawable) ContextCompat.getDrawable(getContext(),
            R.drawable.dr_img_checkered_tiled);
        if (drawable != null) {
            Bitmap checkeredBitmap = drawable.getBitmap();
            bitmapShader = new BitmapShader(checkeredBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);

        if (bgColor == Color.TRANSPARENT) {
            paint.setColor(Color.WHITE);
            paint.setShader(bitmapShader);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);
        }

        paint.setShader(null);
        paint.setColor(bgColor);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);

        /*if (selected) {
            paint.setStrokeWidth(16);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);
        }*/

        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(selected ? activeColor : normalColor);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10, paint);
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        bgColor = color;
        invalidate();
    }
}