package space.klapeyron.robotmgok;

import android.util.Log;
import android.view.View;

import ru.rbot.android.bridge.service.robotcontroll.controllers.BodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.TwoWheelsBodyController;
import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;

public class MoveToBeacons {
    private MainActivity mainActivity;

    private final float speedL = 7.0f;
    private final float speedR = 7.0f;

    private boolean stopFlag = false;

    MoveToBeacons(MainActivity m) {
        mainActivity = m;
    }

    public void ride(View v) {
        stopFlag = false;
        ForwardThread forwardThread = new ForwardThread();
        forwardThread.start();
    }

    public void stop(View v) {
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
