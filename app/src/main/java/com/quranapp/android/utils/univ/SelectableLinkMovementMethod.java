package com.quranapp.android.utils.univ;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

import com.peacedesign.android.utils.span.RoundedBG_FGSpan;

public class SelectableLinkMovementMethod extends LinkMovementMethod {
    private RoundedBG_FGSpan mBGSpan;

    public static SelectableLinkMovementMethod getInstance() {
        return new SelectableLinkMovementMethod();
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mBGSpan = getPressedSpan(textView, spannable, event);
            if (mBGSpan != null) {
                mBGSpan.setPressed(true);
                Selection.setSelection(spannable, spannable.getSpanStart(mBGSpan), spannable.getSpanEnd(mBGSpan));
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            RoundedBG_FGSpan touchedSpan = getPressedSpan(textView, spannable, event);
            if (mBGSpan != null && touchedSpan != mBGSpan) {
                mBGSpan.setPressed(false);
                mBGSpan = null;
                Selection.removeSelection(spannable);
            }
        } else {
            if (mBGSpan != null) {
                mBGSpan.setPressed(false);
            }
            mBGSpan = null;
            Selection.removeSelection(spannable);
        }

        return super.onTouchEvent(textView, spannable, event);
    }

    private RoundedBG_FGSpan getPressedSpan(TextView textView, Spannable spannable, MotionEvent event) {

        int x = (int) event.getX() - textView.getTotalPaddingLeft() + textView.getScrollX();
        int y = (int) event.getY() - textView.getTotalPaddingTop() + textView.getScrollY();

        Layout layout = textView.getLayout();
        int position = layout.getOffsetForHorizontal(layout.getLineForVertical(y), x);

        RoundedBG_FGSpan[] link = spannable.getSpans(position, position, RoundedBG_FGSpan.class);
        RoundedBG_FGSpan touchedSpan = null;
        if (link.length > 0 && positionWithinTag(position, spannable, link[0])) {
            touchedSpan = link[0];
        }

        return touchedSpan;
    }

    private boolean positionWithinTag(int position, Spannable spannable, Object tag) {
        return position >= spannable.getSpanStart(tag) && position <= spannable.getSpanEnd(tag);
    }
}