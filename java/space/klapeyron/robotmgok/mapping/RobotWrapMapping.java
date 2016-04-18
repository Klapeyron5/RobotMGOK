package space.klapeyron.robotmgok.mapping;

import android.util.Log;

import ru.rbot.android.bridge.service.robotcontroll.controllers.BodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.TwoWheelsBodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.data.TwoWheelState;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.listeners.TwoWheelBodyControllerStateListener;
import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;
import ru.rbot.android.bridge.service.robotcontroll.robots.Robot;
import ru.rbot.android.bridge.service.robotcontroll.robots.listeners.RobotStateListener;

public class RobotWrapMapping {
    //Robot states list:
    public String ROBOT_STATE;
    private static final String ROBOT_CONNECTED = "connected";
    private static final String ROBOT_INIT_ERROR = "init error";
    private static final String ROBOT_DISCONNECTED = "disconnected";

    public Robot robot;
    private MappingActivity mappingActivity;

    public float SWAN_MAX_SPEED;

    //counted odometry info
    float countedPath;
    //primary odometry info
    float odometryPath;
    float odometryAngle;
    float odometryAbsoluteX;
    float odometryAbsoluteY;
    float odometryWheelSpeedLeft;
    float odometryWheelSpeedRight;

    public final RobotStateListener robotStateListener = new RobotStateListener() {
        @Override
        public void onRobotReady() {
            setReadingOdometry();
            ROBOT_STATE = ROBOT_CONNECTED;

            if( robot.isControllerAvailable( BodyController.class ) ) {
                try {
                    BodyController bodyController = (BodyController) robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) ) {
                        TwoWheelsBodyController wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        SWAN_MAX_SPEED = wheelsController.SWAN_MAX_SPEED;
                        Log.i("TAG","Max speed: "+(float)SWAN_MAX_SPEED);
                    }
                } catch (ControllerException e) {e.printStackTrace();}
            }
        }

        @Override
        public void onRobotInitError() {
            ROBOT_STATE = ROBOT_INIT_ERROR;
            //TODO //dialog with instruction to run Bridge
        }

        @Override
        public void onRobotDisconnect() {
            ROBOT_STATE = ROBOT_DISCONNECTED;
        }
    };

    public RobotWrapMapping(MappingActivity m) {
        robot = new Robot(m);
        mappingActivity = m;
        robot.setRobotStateListener(robotStateListener);
        robot.start();
    }

    private void setReadingOdometry() {
        TwoWheelBodyControllerStateListener twoWheelBodyControllerStateListener = new TwoWheelBodyControllerStateListener() {
            @Override
            public void onWheelStateRecieved(TwoWheelState twoWheelState) {
                odometryPath = twoWheelState.getOdometryInfo().getPath();
                odometryAngle = twoWheelState.getOdometryInfo().getAngle();
                odometryAbsoluteX = (float) (-twoWheelState.getOdometryInfo().getX() + 0.5 * 3 + 0.25);
                odometryAbsoluteY = (float) (-twoWheelState.getOdometryInfo().getY() + 0.5 + 0.25);
                odometryWheelSpeedLeft = twoWheelState.getSpeed().getLWheelSpeed();
                odometryWheelSpeedRight = twoWheelState.getSpeed().getRWheelSpeed();

        //        mainActivity.textViewAngle.setText(Float.toString(odometryAngle));
        //        mainActivity.textViewOdometryPath.setText(Float.toString(odometryPath));
        //        mainActivity.textViewOdometryAngle.setText(Float.toString(odometryAngle));
            }
        };
        if (robot.isControllerAvailable(BodyController.class)) {
            try {
                BodyController bodyController = (BodyController) robot.getController(BodyController.class);
                if (bodyController.isControllerAvailable(TwoWheelsBodyController.class)) {
                    TwoWheelsBodyController wheelsController = (TwoWheelsBodyController) bodyController.getController(TwoWheelsBodyController.class);
                    wheelsController.setListener(twoWheelBodyControllerStateListener, 100);
                }
            } catch (ControllerException e) {
                e.printStackTrace();
            }
        }
    }
}
