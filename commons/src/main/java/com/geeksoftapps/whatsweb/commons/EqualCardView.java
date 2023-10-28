package com.geeksoftapps.whatsweb.commons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

public class EqualCardView extends CardView {
    public EqualCardView(@NonNull Context context) {
        super(context);
    }

    public EqualCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EqualCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = Math.max(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(height, height);
    }
}
