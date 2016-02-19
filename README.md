# 自定义控件-刮刮卡
## 此控件会使用到Xfermode类，其有三个子类: ##
1. AvoidXfermode  指定了一个颜色和容差，强制Paint避免在它上面绘图(或者只在它上面绘图)
2. PixelXorXfermode  当覆盖已有的颜色时，应用一个简单的像素异或操作
3. PorterDuffXfermode  这是一个非常强大的转换模式，使用它，可以使用图像合成的16条Porter-Duff规则的任意一条来控制Paint如何与已有的Canvas图像进行交互

PorterDuff.Mode为枚举类,一共有16个枚举值:
![](http://i11.tietuku.com/3988e61607967352.jpg)  
###如何使用到上述效果呢？  
我们举个例子-绘制圆角矩形图片的实现  
通常我们的图片是一个矩形，那么要实现把图片变成圆角矩形，需要以下步骤：  
1. 绘制矩形图片  
2. setXferMode(DstIn)  
3. 绘制圆形  
随后就会显示出他们相交的部分，此控件中，我们使用DstOut模式来实现类似“擦除”效果。
### Canvas 与Bitmap的关系
我们可以把这个Canvas理解成系统提供给我们的一块内存区域(但实际上它只是一套画图的API，真正的内存是下面的Bitmap)，而且它还提供了一整套对这个内存区域进行操作的方法，所有的这些操作都是画图API。
### 下面详细说明控件的实现步骤
1.先实现一个画板，效果如下：  
![](http://i11.tietuku.com/d771896e03488208.gif)  
(1).原理就是创建一个空bitmap，使用画笔（paint）通过canvas（画布）api在bitmap上绘制path。这个path是在onTouch方法中动态生成的，每次onTouch后都会调用invalidate方法，此方法会回调onDraw方法，在onDraw方法中设置XferMode（DST_OUT）并且绘制path。

(2).
	
	private void drawPath() {
        mOutterPaint.setStyle(Paint.Style.STROKE);
        mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mCanvas.drawPath(mPath, mOutterPaint);
    }

(3).在onTouch方法中动态的生成path，随后调用invalidate()（此方法会请求重绘View树，即draw()过程）

	@Override
    public boolean onTouchEvent(MotionEvent event) {
        //绘制path
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = Math.abs(x - mLastX);//用户滑动的距离
                int dy = Math.abs(y - mLastY);
                if(dx > 3 || dy > 3){
                    mPath.lineTo(x, y);
                }
                mLastX = x;
                mLastY = y;
                break;
        }
            invalidate();//执行此方法会调用onDraw方法绘制
        return true;
    }

(4).在onDraw方法中会先在bitmap上绘制path，最后一步，就是将绘制好的bitmap显示到屏幕上。

	protected void onDraw(Canvas canvas) {
        mCanvas.drawPath(mPath, mOutterPaint);
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
### 有了上面的基础，下面实现简易的刮刮卡效果
1. 实现擦除效果要使用Xfermode，先绘制一个图层，然后设置Xfermode，然后再绘制第二个图层时就实现了mode锁设定的效果，这里我们使用DST_OUT模式
2. 我们先绘制一个图片的bitmap，然后设置
    `mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));`

最后将内存中准备好的bitmap绘制出来，经过“合成”，画笔划过的部分被显示出来。效果类似于:

![](http://i11.tietuku.com/7626720bd31b2b1a.gif)

### 我们的获奖信息，比如“谢谢参与”是通过drawText画出来的，并不是一张图片，效果如下：
![](http://i11.tietuku.com/c3a3a2d4bc1e9fd4.gif)
### 以上我们已经初步实现了效果，下面我们要计算 画笔已经挂了多少了，达到一定的像素比例就直接展示结果，最后写一个挂到60%的回调接口，原理就是开一个子线程不断的遍历bitmap从左上角第一个像素到右下角最后一个像素，判断每个像素的情况，最终除以总像素数得到一个比例，达到60%就绘制出结果内容，最后再创建一个挂完的回调接口，用于通知MainActivity。最终效果如下:
![](http://i11.tietuku.com/9eaa8ae8e6ee9406.gif)  
##总结  
1.主要是使用了Paint.setXforMode，有16种模式可以选择，组合出不同图层之间的不同效果。  
2.圆角图片就是使用这个原理实现的（图层1画矩形，图层2绘制圆形，设置一个合适的XferMode，组合出效果）。本项目中使用的模式是DST_OUT。  
3.在构造方法中获得自定义属性  
4.这个控件实际上有两层，最底下是用系统的canvas来drawText，而上层是使用了自定义的mcanvas在mbitmap上先画一个覆盖图片，然后设置XferMode，再用onTouchEvent中动态计算的path来drawpath从而实现挂的效果，这时mcanvas是画在了mbitmap上，最后调用drawbitmap来显示出这个bitmap。我们来看onDraw的代码  
protected void onDraw(Canvas canvas) {

        //绘制“谢谢参与”
        canvas.drawText(mText, getWidth() / 2 - mTextBound.width() / 2, getHeight() / 2 + mTextBound.height() / 2, mBackPaint);//绘制“谢谢参与”的文本
        if (mComplete) {
            if (mListener != null) {
                mListener.onComplete();
            }
        }
        if (!mComplete) {
            mOutterPaint.setStyle(Paint.Style.STROKE);
        	mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        	mCanvas.drawPath(mPath, mOutterPaint);
            canvas.drawBitmap(mBitmap, 0, 0, null);//使bitmap显示到屏幕上，在内存中准备好bitmap，然后在屏幕上绘制出来
        }
    }
可以看出，每次都是先画出“谢谢参与”的中奖结果（使用系统的canvas），然后画出在内存中处理好的（覆盖层和橡皮擦层的合成效果）bitmap（使用自定义的mcanva）  
5.还有一步处理很关键，就是在用户挂了60%时会自动的显示出中奖结果，这其实就是在onTouchEvent事件的UP时启动了一个新线程`new Thread(mRunnable).start();`这个线程去遍历mbitmap的每一个像素，当像素值为0的数量超过总数的60%时则只画中奖信息不画覆盖层。

