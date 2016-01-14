package com.example.zhaimeng.imooc_guaguaka.com.example.zhaimeng.imooc_guaguaka.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.example.zhaimeng.imooc_guaguaka.R;

/**
 * Created by zhaimeng on 2016/1/12.
 */
public class GuaGuaKa extends View {

    private Paint mOutterPaint;
    private Path mPath;//手指划屏幕的路径
    private Canvas mCanvas;
    private Bitmap mBitmap;//使用mOutterPaint在mBitmap上绘制
    private int mLastX;
    private int mLastY;
    private Bitmap mOutterBitmap;
    //--------------以上为遮盖层的变量
//    private Bitmap bitmap;
    private String mText;
    private int mTextSize;
    private int mTextColor;
    private Paint mBackPaint;//绘制“谢谢参与”的画笔
    private Rect mTextBound;//“谢谢参与”的矩形范围

    private volatile boolean mComplete = false;//判断擦除的比例是否达到60%

    public GuaGuaKa(Context context) {
        this(context, null);
    }

    public GuaGuaKa(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GuaGuaKa(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        //获得自定义属性
        TypedArray a = null;
        try {
            a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GuaGuaKa, defStyleAttr, 0);
            int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                switch (attr) {
                    case R.styleable.GuaGuaKa_text:
                        mText = a.getString(attr);
                        break;
                    case R.styleable.GuaGuaKa_textColor:
                        mTextColor = a.getColor(attr, 0x000000);
                        break;
                    case R.styleable.GuaGuaKa_textSize:
                        //将22sp转为像素值
                        mTextSize = (int) a.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22, getResources().getDisplayMetrics()));
                        break;
                }
            }
        } finally {
            a.recycle();
        }
        init();
    }

    private void init() {
        mOutterPaint = new Paint();
        mPath = new Path();
        mTextBound = new Rect();
        mBackPaint = new Paint();
        mText = "谢谢惠顾！";

        mTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22, getResources().getDisplayMetrics());
        mOutterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fg_guaguaka);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        //初始化bitmap
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        setupOutPaint();
        setupBackPaint();
//        mCanvas.drawColor(Color.parseColor("#c0c0c0"));//在控件区域绘制一个灰色的图层
        //画覆盖层的bitmap，是圆角矩形
        mCanvas.drawRoundRect(new RectF(0, 0, width, height), 30, 30, mOutterPaint);
        mCanvas.drawBitmap(mOutterBitmap, null, new Rect(0, 0, width, height), null);
    }

    /**
     * 设置绘制“谢谢参与”的画笔属性
     */
    private void setupBackPaint() {
        mBackPaint.setColor(mTextColor);
        mBackPaint.setStyle(Paint.Style.FILL);
        mBackPaint.setTextSize(mTextSize);
        //获得画笔绘制文本的宽和高（矩形范围）
        mBackPaint.getTextBounds(mText, 0, mText.length(), mTextBound);
    }

    /**
     * 设置 橡皮擦 画笔的属性
     */
    private void setupOutPaint() {
        mOutterPaint.setColor(Color.parseColor("#c0c0c0"));
        mOutterPaint.setAntiAlias(true);
        mOutterPaint.setDither(true);
        mOutterPaint.setStrokeJoin(Paint.Join.ROUND);
        mOutterPaint.setStrokeCap(Paint.Cap.ROUND);
        mOutterPaint.setStyle(Paint.Style.STROKE);
        mOutterPaint.setStrokeWidth(20);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //绘制path
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_UP:
                new Thread(mRunnable).start();
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = Math.abs(x - mLastX);//用户滑动的距离
                int dy = Math.abs(y - mLastY);
                if (dx > 3 || dy > 3) {
                    mPath.lineTo(x, y);
                }
                mLastX = x;
                mLastY = y;
                break;
        }
        invalidate();//执行此方法会调用onDraw方法绘制
        return true;
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            int width = getWidth();//拿到控件宽、高
            int height = getHeight();

            float wipeArea = 0;//已经擦除的比例
            float totalArea = width * height;
            Bitmap bitmap = mBitmap;
            int[] mPixels = new int[width * height];
            //获得bitmap的所有像素信息保存在mPixels中
            mBitmap.getPixels(mPixels, 0, width, 0, 0, width, height);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int index = j * width + i;
                    if (mPixels[index] == 0) {
                        wipeArea++;
                    }
                }
            }
            if (wipeArea > 0 && totalArea > 0) {
                int percent = (int) (wipeArea * 100 / totalArea);
                Log.i("cool", percent + "");
                if (percent > 60) {
                    //清楚掉覆盖图层区域
                    mComplete = true;
                    postInvalidate();
                }
            }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawBitmap(bitmap, 0, 0, null);
        //绘制“谢谢参与”
        canvas.drawText(mText, getWidth() / 2 - mTextBound.width() / 2, getHeight() / 2 + mTextBound.height() / 2, mBackPaint);//绘制“谢谢参与”的文本
        if (mComplete) {
            if (mListener != null) {
                mListener.onComplete();
            }
        }
        if (!mComplete) {
            drawPath();
            canvas.drawBitmap(mBitmap, 0, 0, null);//使bitmap显示到屏幕上，在内存中准备好bitmap，然后在屏幕上绘制出来
        }

    }

    private void drawPath() {
        mOutterPaint.setStyle(Paint.Style.STROKE);
        mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mCanvas.drawPath(mPath, mOutterPaint);
    }

    /**
     * 挂完的回调
     */
    public interface OnGuaGuaKaCompleteListener {
        void onComplete();
    }

    private OnGuaGuaKaCompleteListener mListener;

    public void setOnGuaGuaKaCompleteListener(OnGuaGuaKaCompleteListener mListener) {
        this.mListener = mListener;
    }

    public void setText(String text){
        this.mText = text;
        //获得画笔绘制文本的宽和高（矩形范围）
        mBackPaint.getTextBounds(mText, 0, mText.length(), mTextBound);
    }
}
