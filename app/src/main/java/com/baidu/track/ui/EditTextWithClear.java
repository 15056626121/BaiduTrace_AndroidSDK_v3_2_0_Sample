package com.baidu.track.ui;

import com.baidu.track.R;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */
public class EditTextWithClear extends AppCompatEditText {

    private static final int CLEAR_DRAWABLE_LEFT = 0;
    private static final int CLEAR_DRAWABLE_TOP = 1;
    private static final int CLEAR_DRAWABLE_RIGHT = 2;
    private static final int CLEAR_DRAWABLE_BOTTOM = 3;
    private Drawable mClearDrawable;

    public EditTextWithClear(Context context) {
        super(context);
        init();
    }

    public EditTextWithClear(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditTextWithClear(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mClearDrawable = getResources().getDrawable(R.mipmap.clear_edittext);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        showClearIcon(length() > 0 && hasFocus());
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        showClearIcon(length() > 0 && focused);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Drawable drawable = getCompoundDrawables()[CLEAR_DRAWABLE_RIGHT];
            if (drawable != null && event.getX() <= (getWidth() - getPaddingRight())
                    && event.getX() >= (getWidth() - getPaddingRight() - drawable.getBounds()
                    .width())) {
                setText("");
            }
        }
        return super.onTouchEvent(event);
    }

    private void showClearIcon(boolean visible) {
        setCompoundDrawablesWithIntrinsicBounds(getCompoundDrawables()[CLEAR_DRAWABLE_LEFT],
                getCompoundDrawables()[CLEAR_DRAWABLE_TOP],
                visible ? mClearDrawable : null, getCompoundDrawables()[CLEAR_DRAWABLE_BOTTOM]);
    }
}
