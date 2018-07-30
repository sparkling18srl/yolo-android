package org.tensorflow.yolo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import org.tensorflow.yolo.Config;
import org.tensorflow.yolo.model.BoxPosition;
import org.tensorflow.yolo.model.Recognition;
import org.tensorflow.yolo.util.ClassAttrProvider;

import java.util.LinkedList;
import java.util.List;

/**
 * A simple View providing a render callback to other classes.
 * Modified by Alessio Mangano
 */
public class OverlayView extends View {

    private final static float STROKE_WIDTH = 10f;
    private final static float FILL_ALPHA = 255f;
    private final static float TITLE_FONT_SIZE = 16f;
    private final Paint paint, paintTitle, paintText;
    private final List<DrawCallback> callbacks = new LinkedList();
    private List<Recognition> results;
    private List<Integer> colors;
    private float resultsViewHeight;

    public OverlayView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);

        paintTitle = new Paint();
        paintTitle.setColor(Color.GREEN);
        paintTitle.setStyle(Paint.Style.FILL_AND_STROKE);
        paintTitle.setStrokeWidth(STROKE_WIDTH);

        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TITLE_FONT_SIZE, getResources().getDisplayMetrics()));

        resultsViewHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                112, getResources().getDisplayMetrics());
        colors = ClassAttrProvider.newInstance(context.getAssets()).getColors();
    }

    public void addCallback(final DrawCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public synchronized void onDraw(final Canvas canvas) {
        for (final DrawCallback callback : callbacks) {
            callback.drawCallback(canvas);
        }

        if (results != null) {
            for (int i = 0; i < results.size(); i++) {
                String title = results.get(i).getTitle() + ":"
                        + String.format("%.2f", results.get(i).getConfidence() * 100f);

                RectF box = reCalcSize(results.get(i).getLocation());
                RectF boxTitle = reCalcSizeTitle(box, title);
                paint.setColor(colors.get(results.get(i).getId()));
                paint.setStrokeWidth(results.get(i).getConfidence() * STROKE_WIDTH);
                paint.setAlpha((int) (FILL_ALPHA * results.get(i).getConfidence()));
                paintTitle.setColor(colors.get(results.get(i).getId()));
                paintTitle.setStrokeWidth(results.get(i).getConfidence() * STROKE_WIDTH);
                paintTitle.setAlpha((int) (FILL_ALPHA * results.get(i).getConfidence()));

                canvas.drawRect(box, paint);
                canvas.drawRect(boxTitle, paintTitle);
                canvas.drawText(title, boxTitle.left + 2 * STROKE_WIDTH, box.top - 2 * STROKE_WIDTH, paintText);
            }
        }
    }

    public void setResults(final List<Recognition> results) {
        this.results = results;
        postInvalidate();
    }

    /**
     * Interface defining the callback for client classes.
     */
    public interface DrawCallback {
        void drawCallback(final Canvas canvas);
    }

    private RectF reCalcSize(BoxPosition rect) {
        int padding = 5;
        float overlayViewHeight = this.getHeight() - resultsViewHeight;
        float sizeMultiplier = Math.min((float) this.getWidth() / (float) Config.INPUT_SIZE,
                overlayViewHeight / (float) Config.INPUT_SIZE);

        float offsetX = (this.getWidth() - Config.INPUT_SIZE * sizeMultiplier) / 2;
        float offsetY = (overlayViewHeight - Config.INPUT_SIZE * sizeMultiplier) / 2 + resultsViewHeight;

        float left = Math.max(padding,sizeMultiplier * rect.getLeft() + offsetX);
        float top = Math.max(offsetY + padding, sizeMultiplier * rect.getTop() + offsetY);

        float right = Math.min(rect.getRight() * sizeMultiplier, this.getWidth() - padding);
        float bottom = Math.min(rect.getBottom() * sizeMultiplier + offsetY, this.getHeight() - padding);

        return new RectF(left, top, right, bottom);
    }

    private RectF reCalcSizeTitle(RectF rect, String title) {
        int padding = 0;

        float offsetX = 0;
        float offsetY = -(40 + 2 * STROKE_WIDTH);

        float left = rect.left + offsetX;
        float top = rect.top + offsetY;

        float right = rect.left + title.length() * 25;
        float bottom = rect.top;

        return new RectF(left, top, right, bottom);
    }
}
