package space.klapeyron.robotmgok;

import android.util.Log;

import ru.rbot.android.bridge.service.robotcontroll.controllers.BodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.TwoWheelsBodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.data.TwoWheelState;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.listeners.TwoWheelBodyControllerStateListener;
import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;
import ru.rbot.android.bridge.service.robotcontroll.robots.Robot;
import ru.rbot.android.bridge.service.robotcontroll.robots.listeners.RobotStateListener;

public class RobotWrap {
    //Robot states list:
    public String ROBOT_STATE;
    private static final String ROBOT_CONNECTED = "connected";
    private static final String ROBOT_INIT_ERROR = "init error";
    private static final String ROBOT_DISCONNECTED = "disconnected";

    public Robot robot;
    private MainActivity mainActivity;

    //current cells coordinates
    public int currentCellX;
    public int currentCellY;

    //counted odometry info
    float countedPath;
    public int currentDirection; //0: positive direction on X; 1: positive dir on Y; 2: negative on X; 3: negative on Y;

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
            mainActivity.setServerState(MainActivity.SERVER_WAITING_NEW_TASK);
            mainActivity.setRobotConnectionState(ROBOT_STATE);
            Log.i(mainActivity.TAG, ROBOT_STATE);
        }

        @Override
        public void onRobotInitError() {
            ROBOT_STATE = ROBOT_INIT_ERROR;
            mainActivity.setServerState(MainActivity.SERVER_WAITING_ROBOT);
            mainActivity.setRobotConnectionState(ROBOT_STATE);
            Log.i(mainActivity.TAG, ROBOT_STATE);
            //TODO //dialog with instruction to run Bridge
        }

        @Override
        public void onRobotDisconnect() {
            ROBOT_STATE = ROBOT_DISCONNECTED;
            Log.i(mainActivity.TAG, ROBOT_STATE);
            synchronized (this) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.setServerState(MainActivity.SERVER_WAITING_ROBOT);
                        mainActivity.setRobotConnectionState(ROBOT_STATE);
                    }
                });
            }
        }
    };

    public RobotWrap(MainActivity m) {
        robot = new Robot(m);
        mainActivity = m;
        robot.setRobotStateListener(robotStateListener);
        robot.start();
        setStartCoordinatesByServerEditText();
    }

    /**
     * Set robot position
     * @param cellX current number of cell in X
     * @param cellY current number of cell in Y
     * @param direction current direction: 0 - positive on X, 1 - positive on Y, 2 - negative on X, 3 - negative on Y
     */
    public void setPosition(int cellX,int cellY,int direction) {
        currentCellX = cellX;
        currentCellY = cellY;
        currentDirection = direction;
    }

    public void writeCurrentPositionOnServerDisplay() {
        synchronized (this) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.editTextStartX.setText(Integer.toString(currentCellX));
                    mainActivity.editTextStartY.setText(Integer.toString(currentCellY));
                    mainActivity.editTextDirection.setText(Integer.toString(currentDirection));
                }
            });
        }
    }

    public void setStartCoordinatesByServerEditText() {
        currentCellX = Integer.parseInt(mainActivity.editTextStartX.getText().toString());
        currentCellY = Integer.parseInt(mainActivity.editTextStartY.getText().toString());
        currentDirection = Integer.parseInt(mainActivity.editTextDirection.getText().toString());
    }

    public void reconnect() {
        robot = new Robot(mainActivity);
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

                mainActivity.textViewCountedPath.setText(Float.toString(countedPath));
                mainActivity.textViewOdometryPath.setText(Float.toString(odometryPath));
                mainActivity.textViewOdometryAngle.setText(Float.toString(odometryAngle));
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
