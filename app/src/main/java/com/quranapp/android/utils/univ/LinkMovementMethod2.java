package com.quranapp.android.utils.univ;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public class LinkMovementMethod2 extends LinkMovementMethod {
    private static LinkMovementMethod2 sInstance;

    public static LinkMovementMethod2 getInstance() {
        if (sInstance == null) {
            sInstance = new LinkMovementMethod2();
        }

        return sInstance;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            if (off >= widget.getText().length()) {
                // Return true so click won't be triggered in the leftover empty space
                return true;
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }
}