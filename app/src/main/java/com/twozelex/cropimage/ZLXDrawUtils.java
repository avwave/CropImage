package com.twozelex.cropimage;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by ant on 15/3/7.
 */
public class ZLXDrawUtils {
    public static void drawMultiline(Canvas canvas, String str, int x, int y, Paint paint)
    {
        for (String line: str.split("\n"))
        {
            canvas.drawText(line, x, y, paint);
            y += -paint.ascent() + paint.descent();
        }
    }

}

