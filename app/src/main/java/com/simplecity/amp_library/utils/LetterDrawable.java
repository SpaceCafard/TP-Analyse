package com.simplecity.amp_library.utils;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class LetterDrawable extends Drawable {

    String mDisplayName;
    Paint mPaint;
    String mKeyName;
    char[] mFirstChar;
    TypedArray mColors;

    public LetterDrawable(String displayName, TypedArray colors, Paint paint) {

        mDisplayName = displayName;
        mColors = colors;
        mPaint = paint;
        mKeyName = StringUtils.keyFor(displayName);
        if (displayName != null && !displayName.isEmpty()) {
            String key = StringUtils.keyFor(displayName);
            if (key != null && !key.isEmpty()) {
                mFirstChar = new char[] { Character.toUpperCase(key.charAt(0)) };
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (mFirstChar == null || mKeyName == null || mFirstChar.length == 0 || mKeyName.isEmpty()) {
            return;
        }
        canvas.drawColor(pickColor(mDisplayName));
        if (!mKeyName.isEmpty()) {
            mPaint.setTextSize(canvas.getHeight() * 3 / 5);
            mPaint.getTextBounds(mFirstChar, 0, 1, getBounds());
            canvas.drawText(mFirstChar, 0, 1, canvas.getWidth() / 2, canvas.getHeight() / 2
                    + (getBounds().bottom - getBounds().top) / 2, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // This drawable does not support setting alpha because its appearance is determined by the display name.
        // If alpha support is needed, implement this method accordingly.
        throw new UnsupportedOperationException("Setting alpha is not supported for LetterDrawable.");
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // This drawable does not support color filters because its color is determined by the display name.
        // If color filter support is needed, implement this method accordingly.
        // For now, we leave it empty intentionally.
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    /**
     * @param key The key used to generate the tile color
     * @return A new or previously chosen color for <code>key</code> used as the
     * tile background color
     */
    private int pickColor(String key) {
        // String.hashCode() is not supposed to change across java versions, so
        // this should guarantee the same key always maps to the same color
        final int color = key.hashCode() % mColors.length();
        return mColors.getColor(color, Color.BLACK);
    }
}
