package com.example.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
* Created by thevery on 11/09/14.
*/

class WhirlView extends SurfaceView implements Runnable {
    private int[] drawingField;
    private Bitmap buffer;
    private int width, height;
    private RectF screenRect;
    private UpdateThread updater;
    private SurfaceHolder holder;
    private Thread thread = null;
    private volatile boolean running = false;
    private volatile CyclicBarrier barrier;

    public WhirlView(Context context) {
        super(context);
        holder = getHolder();
        barrier = new CyclicBarrier(2);
        setDrawingCacheQuality(DRAWING_CACHE_QUALITY_HIGH);
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        running = false;
        barrier.reset();
        barrier = new CyclicBarrier(1);
        updater.stopRunning();
        try {
            thread.join();
            updater.join();
        } catch (InterruptedException ignore) {}
    }

    public void run() {
        int timeSum = 0, tot = 0;
        while (running) {
            if (holder.getSurface().isValid()) {
                long startTime = System.nanoTime();
                Canvas canvas = holder.lockCanvas();
                drawingField = updater.lockField();
                updater.update();
                draw(canvas);
                try {
                    barrier.await();
                } catch (BrokenBarrierException ignore) {}
                catch (InterruptedException e) {}
                holder.unlockCanvasAndPost(canvas);
                long finishTime = System.nanoTime();
                ++tot;
                timeSum += (finishTime - startTime) / 1000000;
                if (timeSum > 5000) {
                    Log.i("TIME", "FPS: " + tot / (timeSum / 1000.0));
                    timeSum = 0;
                    tot = 0;
                }
                //return;
            }
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        if (w < h) {
            width = 240;
            height = 320;
        } else {
            width = 320;
            height = 240;
        }
        buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (updater != null)
            updater.interrupt();
        updater = new UpdateThread(width, height, barrier);
        updater.start();
        screenRect = new RectF(0, 0, w, h);
    }

    @Override
    public void draw(Canvas canvas) {
        Log.i("PIXEL", "COL" + drawingField[0] + " " + drawingField[1]);
        buffer.setPixels(drawingField, 0, width, 0, 0, width, height);
        canvas.drawBitmap(buffer, null, screenRect, null);
    }
}
