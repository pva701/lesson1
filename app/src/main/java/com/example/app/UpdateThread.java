package com.example.app;

import android.util.Log;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by pva701 on 9/16/14.
 */
public class UpdateThread extends Thread {
    private boolean fieldStabilized;
    private final int MAX_COLOR = 10;
    final int[] palette = {0xFFFF0000, 0xFF800000, 0xFF808000, 0xFF008000, 0xFF00FF00, 0xFF008080, 0xFF0000FF, 0xFF000080, 0xFF800080, 0xFFFFFFFF};
    private int[][] field;
    private int width;
    private int height;
    private static int[][] drawingField;
    private static int numOfUnlockedDrawingField;
    private static boolean updateNeed;
    private static CyclicBarrier barrier;
    private static boolean running;
    public UpdateThread(int w, int h, CyclicBarrier bar) {
        width = w;
        height = h;
        Log.i("W H", w + " " + h);
        barrier = bar;
        drawingField = new int[2][w * h];
        updateNeed = false;
        numOfUnlockedDrawingField = 0;
        fieldStabilized = false;
    }
    
    @Override
    public void run() {
        initField();
        running = true;
        while (running) {
            if (updateNeed) {
                updateField();
                updateNeed = false;
                try {
                    barrier.await();
                } catch (BrokenBarrierException e) {}
                catch (InterruptedException e) {}
            }
        }
    }

    private void updateField() {
        if (fieldStabilized) {
            incCells();
            return;
        }

        int[][] field2 = new int[width][height];
        int founds = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                field2[x][y] = field[x][y];
                int newColor = (field[x][y] + 1) % MAX_COLOR;
                boolean found = false;
                for (int dx = -1; dx <= 1 && !found; dx++) {
                    int x2 = x + dx;
                    if (x2 < 0) x2 += width;
                    else if (x2 >= width) x2 -= width;
                    for (int dy = -1; dy <= 1 && !found; dy++) {
                        int y2 = y + dy;
                        if (y2 < 0) y2 += height;
                        else if (y2 >= height) y2 -= height;
                        found = newColor == field[x2][y2];
                    }
                }

                if (found) {
                    field2[x][y] = newColor;
                    ++founds;
                    drawingField[numOfUnlockedDrawingField][y * width + x] = palette[field2[x][y]];
                }
            }
        }
        field = field2;
        fieldStabilized = founds == width * height;
    }

    private void incCells() {
        for (int i = 0; i < width; ++i)
            for (int j = 0; j < height; ++j) {
                field[i][j]++;
                if (field[i][j] == MAX_COLOR)
                    field[i][j] = 0;
                drawingField[numOfUnlockedDrawingField][j * width + i] = palette[field[i][j]];
            }
    }

    private void initField() {
        field = new int[width][height];
        Random rand = new Random();
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                field[x][y] = rand.nextInt(MAX_COLOR);
                drawingField[numOfUnlockedDrawingField][width * y + x] = palette[field[x][y]];
            }
    }

    public int[] lockField() {
        numOfUnlockedDrawingField ^= 1;
        return drawingField[numOfUnlockedDrawingField ^ 1];
    }

    public void update() {
        updateNeed = true;
    }

    public void stopRunning() {
        barrier = new CyclicBarrier(1);
        running = false;
    }
}
