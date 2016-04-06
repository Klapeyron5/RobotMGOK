package space.klapeyron.robotmgok;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import ru.rbot.android.bridge.service.robotcontroll.controllers.BodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.TwoWheelsBodyController;
import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;

public class MoveToBeacons {
    private MainActivity mainActivity;

    private float speedL = 0.5f;
    private float speedR = 10.0f;
    private float tempSpeed = 0;
    private double threashold = 0.2;

    private boolean stopFlag = false;

    MoveToBeacons(MainActivity m) {
        mainActivity = m;
    }

    double[] Distance = new double[3];
    double[] distance;
    double[] firstDiffer = new double[2];
    double secondDiffer;

    Thread searchAndGo = new Thread(){
        public void run(){
            ride();
            updateData();
            //Log.i("TAG", Double.toString((distance[0] + distance[1] + distance[2])/3));
            while((distance[0] + distance[1] + distance[2])/3 > 1) {
                updateData();
            if (firstDiffer[0] < 0) //приближение (приращение расстояние отрицательное)
            {
                speedL = 10.0f;
                speedR = 10.0f;
                /*if ((secondDiffer > (-threashold))&&(secondDiffer < (threashold))) {
                    Log.i("TAG","выравнивание скоростей колес, движение вперед");
                    speedL = 10.0f;
                    speedR = 10.0f;
                }
                if (secondDiffer < (-threashold)) {
                    Log.i("TAG","сохранение параметров движения");
                }
                if (secondDiffer > threashold) {
                    Log.i("TAG","смена направления поворота");
                    tempSpeed = speedL;
                    speedL = speedR;
                    speedR = tempSpeed;
                }*/
            }
            else {
                if ((secondDiffer > (-threashold))&&(secondDiffer < (threashold))) {
                    Log.i("TAG", "стартовые параметры езды  ");
                    speedL = 0.5f;
                    speedR = 10.0f;
                }
                if (secondDiffer < (-threashold)) {
                    Log.i("TAG", "сохранение параметров движения");
                }
                if (secondDiffer > threashold){
                    Log.i("TAG", "смена направления поворота");
                    tempSpeed = speedL;
                    speedL = speedR;
                    speedR = tempSpeed;
                }
            }
                //Log.i("TAG", "стартовые параметры езды  " + Double.toString((distance[0] + distance[1] + distance[2]) / 3));
                try {
                    searchAndGo.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i("TAG", "Приехали:)");
            stopRide();
        }
    };

    public void updateData() {
        distance = mainActivity.Distance;

        Distance[2] = Distance[1];
        Distance[1] = Distance[0];
        Distance[0] = (distance[0] + distance[1] + distance[2])/3;

        firstDiffer[1] = firstDiffer[0];
        firstDiffer[0] = Distance[0] - Distance[1];
        secondDiffer = (firstDiffer[0] - firstDiffer[1])/2;
    }
    public void ride() {
        stopFlag = false;
        ForwardThread forwardThread = new ForwardThread();
        forwardThread.start();
    }

    public void stopRide() {
        stopFlag = true;
    }

    class ForwardThread extends Thread {
        TwoWheelsBodyController wheelsController = null;
        @Override
        public void run() {
            if( mainActivity.robotWrap.robot.isControllerAvailable( BodyController.class ) ) {
                try {
                    BodyController bodyController = (BodyController) mainActivity.robotWrap.robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) ) {
                        wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        while(true) {
                            Log.i(mainActivity.TAG, "setSpeed after");
                            wheelsController.setWheelsSpeeds(speedL, speedR);
                            Log.i(mainActivity.TAG, "setSpeed before");
                            sleep(500);
                            Log.i(mainActivity.TAG, "after sleep");
                            if (stopFlag) {
                                wheelsController.setWheelsSpeeds(0.0f, 0.0f);
                                Log.i(mainActivity.TAG,"STOP");
                                return;
                            }
                        }
                    }
                } catch (ControllerException e) {e.printStackTrace();}
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


