package space.klapeyron.robotmgok.InteractiveMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import space.klapeyron.robotmgok.MainActivity;
import space.klapeyron.robotmgok.Navigation;

public class InteractiveMapView extends View {

    private MainActivity mainActivity;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint paint, mBitmapPaint;
    private final int horizontalCountOfCells = 13;
    private final int verticalCountOfCells = 18;
    private final int textCoordinatesSize = 14;

    private int screenWidth;
    private int screenHeight;
    private int cellWidth;
    private int cellHeight;
    private int cellsLineWidth = 2;

    public InteractiveMapView(Context context, int X, int Y) {
        super(context);
        mainActivity = (MainActivity) context;

        setEmptyMapBitmap();
        drawRobotOnMap(X, Y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

    /**
     * Draw empty map on InteractiveMapView.mBitmap variable
     */
    private void setEmptyMapBitmap() {
        //вычисляем размеры экрана
        DisplayMetrics metrics = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        //TODO//вычисляем какая сторона меньше и подгоняем под нее размеры квадратов
        //сейчас всегда держим вертикальную ориентацию
        cellWidth = (screenWidth-15)/horizontalCountOfCells;
        cellHeight = cellWidth;

        mBitmap = Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        //определяем параметры кисти, которой будем рисовать сетку и атомы
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(0xffff0505);
        paint.setStrokeWidth(cellsLineWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        //кисть для координат на сетке
        Paint paintForCoordinates = new Paint();
        paintForCoordinates.setColor(0xffff0101);
        paintForCoordinates.setStrokeWidth(1);
        paintForCoordinates.setTextSize(textCoordinatesSize);

        //рисуем сетку и координаты
        for(int x=0;x< horizontalCountOfCells +1;x++) {
            mCanvas.drawLine(x * cellWidth, 0, x * cellWidth, verticalCountOfCells * cellHeight, paint);
        }
        for(int x=0;x< horizontalCountOfCells;x++) {
            mCanvas.drawText(Integer.toString(x),x*cellWidth,verticalCountOfCells*cellHeight+textCoordinatesSize,paintForCoordinates);
        }
        for(int y=0;y< verticalCountOfCells +1;y++) {
            mCanvas.drawLine(0, y * cellHeight, horizontalCountOfCells * cellWidth, y * cellHeight, paint);
        }
        for(int y=0;y< verticalCountOfCells;y++) {
            mCanvas.drawText(Integer.toString(y),horizontalCountOfCells*cellWidth,y*cellHeight+textCoordinatesSize,paintForCoordinates);
        }
    }

    public void drawRobotOnMap(int X, int Y) {
    //    mCanvas.drawPoint(X *cellWidth,Y*cellHeight,paint);
        mCanvas.drawCircle(X *cellWidth+cellWidth/2,Y*cellHeight+cellHeight/2,cellWidth/4,paint);
    }

    private void onDrawPath() {
        Navigation navigation = new Navigation();
        navigation.setStart(5, 3);
        navigation.setFinish(16, 9);
        navigation.getPathCommandsForRobot();
        ArrayList<Integer> path = new ArrayList<>(navigation.absolutePath);
        for(int i=0;i<path.size();i++)
            Log.i(MainActivity.TAG, path.get(i) + "");
    }

    //переводим dp в пиксели
    public float convertDpToPixel(float dp,Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi/160f);
    }
}
