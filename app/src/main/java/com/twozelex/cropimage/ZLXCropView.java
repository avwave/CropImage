package com.twozelex.cropimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.utils.ImageSizeUtils;
import com.nostra13.universalimageloader.utils.L;

/**
 * Created by ant on 15/3/7.
 */
public class ZLXCropView extends ImageView {
    private static final String TAG = "ZLXCropView";

    private GestureDetector gd;
    private ScaleGestureDetector sgd;

    private float mMaxScale;

    // World coordinate system
    private Rect mBitmapRect = new Rect();
    private RectF mBitmapRectF;
    private Matrix mMatrix = new Matrix();

    // Screen coordinate system
    private int mViewWidth;
    private int mViewHeight;
    private Rect mWindow = new Rect();
    private PointF mOffset = new PointF();
    public float mScale = 1f;
    private PointF mScalePoint = new PointF();

    private Boolean mIsCircle = false;

    private Bitmap mBitmap;
    private ZLXImageDecoder.PhotoFileInfo mInfo;
    private ImageSize mSize;
    private float mSubScale = 1f;
    private ImageSize mSubSize = new ImageSize(0, 0);
    private float scalePercent;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                checkOffset();
                invalidate();
                return true;
        }
        gd.onTouchEvent(event);
        sgd.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mViewWidth<=0||mViewHeight<=0||mBitmap==null) return;
        Paint paint = new Paint();

        // Draw scaled subsampled image on a snap bitmap
        Bitmap snap = Bitmap.createBitmap(mViewWidth, mViewHeight, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(snap);

        // Background
        c.translate(mOffset.x, mOffset.y);
        c.scale(mScale, mScale, mScalePoint.x, mScalePoint.y);
        c.drawBitmap(mBitmap, 0, 0, paint);

        // Crop: Debug info should draw here
        drawDebugInfo(paint, c);

        // Draw the snap bitmap on view's canvas
        canvas.drawBitmap(snap, new Matrix(), paint);

        // Crop: Draw the mask
        int oldAlpha = paint.getAlpha();
        paint.setAlpha(150);
        canvas.drawRect(0, 0, mViewWidth, mViewHeight, paint);
        paint.setAlpha(oldAlpha);

        // Draw the bitmap in the window using snap
        BitmapShader crop = new BitmapShader(snap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(crop);
        if (mIsCircle) {
            canvas.drawCircle(mWindow.centerX(), mWindow.centerY(), mWindow.width()/2, paint);
        }else{
            canvas.drawRect(mWindow, paint);
        }
    }

    private void drawDebugInfo(Paint paint, Canvas c) {
        if (mBitmap!=null&&mInfo!=null&&mSize!=null) {
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            final int FONT_SIZE=36;
            paint.setTextSize(FONT_SIZE);
            String msg =
                    String.format("Image %dx%d",
                            mInfo.imageSize.getWidth(), mInfo.imageSize.getHeight()) + "\n" +
                            String.format("Size %dx%d",
                                    mSize.getWidth(), mSize.getHeight()) + "\n \n" +
                            String.format("Sub-sampled %dx%d Sub-scale %.2f",
                                    mBitmap.getWidth(), mBitmap.getHeight(), mSubScale) + "\n \n" +
                            String.format("View %dx%d", mViewWidth, mViewHeight) + "\n" +
                            String.format("Bitmap %s %dx%d", mBitmapRect,
                                    mBitmapRect.width(), mBitmapRect.height()) + "\n" +
                            String.format("Window %s %dx%d", mWindow,
                                    mWindow.width(), mWindow.height()) + "\n" +
                            String.format("Offset (%.0f,%.0f)" , mOffset.x , mOffset.y) +
                            String.format("Scale %.2f @(%.0f,%.0f)", mScale,
                                    mScalePoint.x, mScalePoint.y) + "\n" +
                            String.format("MaxScale %.2f", mMaxScale) + "\n" +
                            "";
            c.save(Canvas.MATRIX_SAVE_FLAG);
            mMatrix.reset();
            c.setMatrix(mMatrix);
            ZLXDrawUtils.drawMultiline(c, msg, 0, FONT_SIZE, paint);

            Paint.Style oldStyle = paint.getStyle();
            paint.setStyle(Paint.Style.STROKE);
            c.drawRect(mBitmapRectF, paint);

            paint.setStyle(oldStyle);
            c.restore();
        }
    }

    public ZLXCropView(Context context, AttributeSet attrs) {
        super(context, attrs);

        gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float dx = -distanceX;
                float dy = -distanceY;
                offset(dx, dy);
                invalidate();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                defaultAndOffset();
                invalidate();
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Get the target scale from bitmap rect to window rect
                defaultScaleAndOffset();
                invalidate();
                return super.onDoubleTap(e);
            }
        });
        sgd = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                float newScale = mScale*scale;
                if (newScale>mMaxScale) {
                    newScale = mMaxScale;
                }
                scale(newScale);
                invalidate();
                return true;
            }
        }
        );
    }

    private void checkOffset() {
        if (mBitmapRectF==null||mWindow==null) return;

        float d;
        Boolean alignLeft=false, alignRight=false, alignTop=false, alignBottom=false;
        int PIXEL_ERROR = -1; // Float scale will cause 1 pixel error
        L.i("%d", mBitmapRect.width()-mWindow.width());
        L.i("%d", mBitmapRect.height()-mWindow.height());
        if (mBitmapRect.width()-mWindow.width()>=PIXEL_ERROR &&
                mBitmapRect.height()-mWindow.height()>=PIXEL_ERROR) {
            int nearLeft = Math.abs(mWindow.left-mBitmapRect.left);
            int nearRight = Math.abs(mWindow.right-mBitmapRect.right);

            if (nearLeft<nearRight) {
                alignLeft = true;
            }else{
                alignRight = true;
            }
            if (nearLeft==nearRight) {
                if (mBitmapRect.left>mWindow.left) {
                    alignLeft = true;
                }
                if (mBitmapRect.right>mWindow.right) {
                    alignRight = true;
                }
            }

            int nearTop = Math.abs(mWindow.top-mBitmapRect.top);
            int nearBottom = Math.abs(mWindow.bottom-mBitmapRect.bottom);

            if (nearTop<nearBottom) {
                alignTop = true;
            }else{
                alignBottom = true;
            }
            if (nearTop==nearBottom) {
                if (mBitmapRect.top>mWindow.top) {
                    alignTop = true;
                }
                if (mBitmapRect.bottom>mWindow.bottom) {
                    alignBottom = true;
                }
            }
        }else{
            defaultScaleAndOffset();
        }
        if (alignLeft) {
            d = (mWindow.left - mBitmapRectF.left)/2f;
            // Animate back
            while (mBitmapRect.left>mWindow.left) {
                offset(d, 0);
                invalidate();
                d = (mWindow.left - mBitmapRectF.left)/2;
            }
        }
        if (alignRight) {
            d = (mWindow.right - mBitmapRectF.right)/2;
            // Animate back
            while (mBitmapRect.right<mWindow.right) {
                offset(d, 0);
                invalidate();
                d = (mWindow.right - mBitmapRectF.right)/2;
            }
        }
        if (alignTop) {
            d = (mWindow.top - mBitmapRectF.top)/2;
            while (mBitmapRect.top>mWindow.top) {
                offset(0, d);
                invalidate();
                d = (mWindow.top - mBitmapRectF.top)/2;
            }
        }
        if (alignBottom) {
            d = (mWindow.bottom - mBitmapRectF.bottom)/2;
            // Animate back
            while (mBitmapRect.bottom<mWindow.bottom) {
                offset(0, d);
                invalidate();
                d = (mWindow.bottom - mBitmapRectF.bottom)/2;
            }
        }
    }

    private void defaultAndOffset() {
        float dx = mWindow.centerX() - mBitmapRectF.centerX();
        float dy = mWindow.centerY() - mBitmapRectF.centerY();
        offset(dx, dy);
    }

    private void defaultScaleAndOffset() {
        // Scale
        float scale = ImageSizeUtils.computeImageScale(
                new ImageSize(mBitmap.getWidth(), mBitmap.getHeight()),
                new ImageSize(mWindow.width(), mWindow.height()),
                ViewScaleType.CROP, true);
        scale(scale);

        // Offset
        float dx = mWindow.centerX() - mBitmapRectF.centerX();
        float dy = mWindow.centerY() - mBitmapRectF.centerY();
        offset(dx, dy);
    }

    private void scale(float scale) {
        mScale = scale;

        // Update bitmap rect
        float w = mBitmap.getWidth();
        float h = mBitmap.getHeight();
        float sw = w*mScale;
        float sh = h*mScale;
        float cx = mBitmapRectF.centerX();
        float cy = mBitmapRectF.centerY();
        mBitmapRectF.set(cx - sw / 2f, cy - sh / 2f,
                cx + sw / 2f, cy + sh / 2f);
        mBitmapRectF.round(mBitmapRect);
    }

    private void offset(float dx, float dy) {
        mOffset.offset(dx, dy);

        // Update bitmap rect
        mBitmapRectF.offset(dx, dy);
        mBitmapRectF.round(mBitmapRect);
    }

    public void init(Bitmap bitmap, ZLXImageDecoder.PhotoFileInfo info, ImageSize size, Boolean isCircle) {
        mBitmap = bitmap;
        mBitmapRectF = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mBitmapRectF.round(mBitmapRect);

        // called after setImageDrawable
        mInfo = info;
        mSize = size;
        mMaxScale = Math.min((float)mInfo.imageSize.getWidth()/mSize.getWidth(),
                (float)mInfo.imageSize.getHeight()/mSize.getHeight());

        // View width and height
        mViewWidth = getWidth();
        mViewHeight = getHeight();

        // is circle flag
        mIsCircle = isCircle;

        // Window
        int margin = mViewWidth/20;
        mSubScale = (float)mBitmap.getWidth()/mInfo.imageSize.getWidth();
        mSubSize = new ImageSize(mSize.getWidth(), mSize.getHeight());
        mSubSize = mSubSize.scale(mSubScale);

        float scale = ImageSizeUtils.computeImageScale(
                new ImageSize(mSubSize.getWidth(), mSubSize.getHeight()),
                new ImageSize(mViewWidth - margin, mViewHeight - margin),
                ViewScaleType.FIT_INSIDE, true
        );

        ImageSize windowSize = mSubSize.scale(scale);

        int left = (mViewWidth-windowSize.getWidth())/2;
        int top = (mViewHeight-windowSize.getHeight())/2;
        mWindow.set(left, top, left + windowSize.getWidth(), top + windowSize.getHeight());

        // Scale point
        mScalePoint.set(mBitmap.getWidth() / 2f, mBitmap.getHeight() / 2f);

        defaultScaleAndOffset();
    }
}

