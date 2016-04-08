package space.klapeyron.robotmgok;

import android.util.Log;

import java.util.ArrayList;

import ru.rbot.android.bridge.service.robotcontroll.controllers.BodyController;
import ru.rbot.android.bridge.service.robotcontroll.controllers.body.TwoWheelsBodyController;
import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;

/**
 * main class for providing robot's movement on the pathCommandsForRobot
 */
public class TaskHandler {
    private MainActivity mainActivity;
    public RobotWrap robotWrap;
    private Navigation navigation;
    public ArrayList<Integer> absolutePath;
    /**
     * Link to now executable Thread from TaskHandler threads, if robot is riding to target
     */
    public Thread runningLowLevelThread;
    public Thread runningTaskThread;

    private final static float SQUARE_WIDTH = 0.5f;

    private int[] arrayPath = {1,1,2,1,1,0,1};
    private ArrayList<Integer> path;//0-right; 1-forward; 2-left;

    public int finishX = 0;
    public int finishY = 0;

    TaskHandler(MainActivity m) {
        mainActivity = m;
        robotWrap = mainActivity.robotWrap;
        navigation = new Navigation(this);
    }

    /**
     * set target coordinates and robot will go there
     * @param fX target X
     * @param fY target Y
     */
    public void setTask(int fX, int fY) throws ControllerException {
        Log.i(MainActivity.TAG, "SetTask: X: " + fX + "  Y: " + fY);
        robotWrap.setStartCoordinatesByServerEditText(); //find out current robot coordinates
        navigation.setStart(robotWrap.currentCellY,robotWrap.currentCellX);
        navigation.setFinish(fY,fX);
        finishX = fX;
        finishY = fY;

        Log.i(MainActivity.TAG, "Start coordinates: " + navigation.getStart()[0] + " " + navigation.getStart()[1]);
        Log.i(MainActivity.TAG, "Finish coordinates: " + navigation.finish[0] + " " + navigation.finish[1]);

        path = navigation.getPathCommandsForRobot(); //get pathCommandsForRobot's commands
        absolutePath = navigation.absolutePath;

        Log.i(MainActivity.TAG,"PATH");

        for(int i=0;i<path.size();i++)
            Log.i(MainActivity.TAG,path.get(i)+"");

   //     pathCommandsForRobot = new ArrayList<>();
   //     arrayInList();//TODO

        //TODO    //ДЛЯ ЕЗДЫ РОБОТА
        TaskThread taskThread = new TaskThread(); //start to run on the pathCommandsForRobot
        runningTaskThread = taskThread;
        taskThread.start();
    }

    /**
     * provide robot's movement on the pathCommandsForRobot in real-time
     */
    class TaskThread extends Thread {
        @Override
        public void run() {
            mainActivity.TTS.startMove();
            synchronized (this) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.setServerState(MainActivity.SERVER_EXECUTING_TASK);
                    }
                });
            }
            float startPath = mainActivity.robotWrap.odometryPath;
            Log.i(MainActivity.TAG, "setTask start odometryPath "+startPath);

            int straightLineCoefficient = 0; //number of cells, which lie on straight line
            for(int i=0;i<path.size();i++) {
                switch(path.get(i)) {
                    case 0: //right turn on PI/2
                        if (i!=0)
                            mainActivity.TTS.turnRight();
                        turnRight();
                    //    turn(-(float)Math.PI/2);
                        //than we think, that turn was successfully ended and change current robot's direction
                        if(robotWrap.currentDirection!=3)
                            robotWrap.currentDirection++;
                        else
                            robotWrap.currentDirection = 0;
                //        robotWrap.writeCurrentPositionOnServerDisplay();
                        mainActivity.displayRobotPosition();
                        break;
                    case 1: //straight movement, if in counting straight line finished, start to move
                        straightLineCoefficient++;
                        if (i == path.size() - 1) {
                            distanceForward(straightLineCoefficient); //start to move
                            straightLineCoefficient = 0;
                        } else
                            if (path.get(i + 1) != 1) {
                                distanceForward(straightLineCoefficient); //start to move
                                straightLineCoefficient = 0;
                            }
                        break;
                    case 2: //left turn on PI/2
                        if (i!=0)
                            mainActivity.TTS.turnLeft();
                        turnLeft();
                     //   turn((float)Math.PI/2);
                        //than we think, that turn was successfully ended and change current robot's direction
                        if(robotWrap.currentDirection!=0)
                            robotWrap.currentDirection--;
                        else
                            robotWrap.currentDirection = 3;
                       // robotWrap.writeCurrentPositionOnServerDisplay();
                        mainActivity.displayRobotPosition();
                        break;
                }
            }

            synchronized (this) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.setServerState(MainActivity.SERVER_WAITING_NEW_TASK);
                    }
                });
            }
            Log.i(MainActivity.TAG, "setTask finish odometryPath " + mainActivity.robotWrap.odometryPath);
            Log.i(MainActivity.TAG, "setTask finish difference " + (mainActivity.robotWrap.odometryPath - startPath));
            mainActivity.TTS.stopMove();
            mainActivity.stopRiding();
        }
    }

    /**
     * Starts moving on straight line with size straightLineCoefficient*SQUARE_WIDTH.
     * Control is passed to {@link space.klapeyron.robotmgok.TaskHandler.ForwardThread}.
     * Link to new Thread is created in TaskHandler.runningLowLevelThread
     * @param straightLineCoefficient number of squares in straight line
     */
    private void distanceForward(int straightLineCoefficient) {
    /*    if (straightLineCoefficient == 1) {
            ForwardThreadForSingleDistance forwardThreadForSingleDistance = new ForwardThreadForSingleDistance();
            forwardThreadForSingleDistance.start(); //acceleration on first SQUARE_WIDTH
            try {
                forwardThreadForSingleDistance.join();
            } catch (InterruptedException e) {}

        } else {

            StartingForwardThread startingForwardThread = new StartingForwardThread();
            startingForwardThread.start(); //acceleration on first SQUARE_WIDTH
            try {
                startingForwardThread.join();
            } catch (InterruptedException e) {}
            straightLineCoefficient--;*/
            if (straightLineCoefficient > 0) {
                ForwardThread forwardThread = new ForwardThread(straightLineCoefficient);
                runningLowLevelThread = forwardThread;
                forwardThread.start();
                try {
                    forwardThread.join();
                } catch (InterruptedException e) {}
                runningLowLevelThread = null;
            }
    //    }
    }

    private void turn(float angle) {
        TurnThread turnThread = new TurnThread(angle);
        runningLowLevelThread = turnThread;
        turnThread.start();
        try {
            turnThread.join();
        } catch (InterruptedException e) {}
        runningLowLevelThread = null;
    }

    private void turnLeft() {
        LeftThread leftThread = new LeftThread();
        runningLowLevelThread = leftThread;
        leftThread.start();
        try {
            leftThread.join();
        } catch (InterruptedException e) {}
        runningLowLevelThread = null;
    }

    private void turnRight() {
        RightThread rightThread = new RightThread();
        runningLowLevelThread = rightThread;
        rightThread.start();
        try {
            rightThread.join();
        } catch (InterruptedException e) {}
        runningLowLevelThread = null;
    }

    class StartingForwardThread extends Thread {
        private float startPath;

        StartingForwardThread() {
            startPath = mainActivity.robotWrap.odometryPath;
        }

        @Override
        public void run() {
            Log.i(MainActivity.TAG, "StartingForwardThread started");
            if( robotWrap.robot.isControllerAvailable( BodyController.class ) )
            {
                BodyController bodyController = null;
                try {
                    bodyController = (BodyController) robotWrap.robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) )
                    {
                        TwoWheelsBodyController wheelsController = null;
                        wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        float i = 0;
                        CheckThread checkThread;
                        while(true) {
                            if(i<10) //acceleration
                                i++;
                            wheelsController.setWheelsSpeeds(i, i);
                            checkThread = new CheckThread(startPath,wheelsController);
                            checkThread.setRunning(true);
                            checkThread.start();
                            sleep(500);
                            checkThread.setRunning(false);
                            if (checkThread.stopFlag)
                                return;
                        }
                    }
                } catch (ControllerException e) {} catch (InterruptedException e) {}
            }
        }

        class CheckThread extends Thread {
            private float startPath;
            private TwoWheelsBodyController wheelsController;
            private boolean running = false;
            public boolean stopFlag = false;
            CheckThread(float s,TwoWheelsBodyController w) {
                startPath = s;
                wheelsController = w;
            }
            @Override
            public void run() {
                while(running) {
                    if(!(mainActivity.robotWrap.odometryPath - startPath < TaskHandler.SQUARE_WIDTH)) {
                        stopFlag = true;
                        return;
                    }
                }
            }
            public void setRunning(boolean r) {
                running = r;
            }
        }
    }

    class ForwardThreadForSingleDistance extends Thread {
        private float startPath;

        ForwardThreadForSingleDistance() {
            startPath = mainActivity.robotWrap.odometryPath;
        }

        @Override
        public void run() {
            Log.i(MainActivity.TAG,"ForwardThreadForSingleDistance started");
            if( robotWrap.robot.isControllerAvailable( BodyController.class ) )
            {
                BodyController bodyController = null;
                try {
                    bodyController = (BodyController) robotWrap.robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) )
                    {
                        TwoWheelsBodyController wheelsController = null;
                        wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        float i = 0;
                        CheckThread checkThread;
                        while(true) {
                            if(i<10) //acceleration
                                i++;
                            wheelsController.setWheelsSpeeds(i, i);
                            checkThread = new CheckThread(startPath,wheelsController);
                            checkThread.setRunning(true);
                            checkThread.start();
                            sleep(500);
                            checkThread.setRunning(false);
                            if (checkThread.stopFlag)
                                return;
                        }
                    }
                } catch (ControllerException e) {} catch (InterruptedException e) {}
            }
        }

        class CheckThread extends Thread {
            private float startPath;
            private TwoWheelsBodyController wheelsController;
            private boolean running = false;
            public boolean stopFlag = false;
            CheckThread(float s,TwoWheelsBodyController w) {
                startPath = s;
                wheelsController = w;
            }
            @Override
            public void run() {
                while(running) {
                    if(!(mainActivity.robotWrap.odometryPath - startPath < TaskHandler.SQUARE_WIDTH)) {
                        wheelsController.setWheelsSpeeds(0.0f, 0.0f);
                        stopFlag = true;
                        return;
                    }
                }
            }
            public void setRunning(boolean r) {
                running = r;
            }
        }
    }

    /**
     * Forward movement
     */
    class ForwardThread extends Thread {
        private float startPath;
        private float purposePath;
        private float counterForChangeCoords = 0;
        private float standardSpeed = 7.0f;
        private float correctionSpeed = 0.2f;
        private float correctionSpeedCorrection = 0.1f;
        /**
         * Range of allowed deviation robot's angle from the direction.
         */
        private float rangeValidDeviation = 0.01f;
        private TwoWheelsBodyController wheelsController = null;
        private float dtAngle = mainActivity.robotWrap.odometryAngle; //odometryAngle with the last iteration (for derivative counting)
        private float corrSpeedLeft = 0;
        private float corrSpeedRight = 0;

        /**
         * @param straightLineCoefficient number of squares in straight line
         */
        ForwardThread(int straightLineCoefficient) {
            startPath = mainActivity.robotWrap.odometryPath;
            purposePath = straightLineCoefficient * TaskHandler.SQUARE_WIDTH;
        }

        @Override
        public void run() {
            Log.i(MainActivity.TAG, "ForwardThread started "+purposePath+" m;  direction "+robotWrap.currentDirection);
            if( robotWrap.robot.isControllerAvailable( BodyController.class ) ) {
                try {
                    BodyController bodyController = (BodyController) robotWrap.robot.getController( BodyController.class );
                    if( bodyController.isControllerAvailable( TwoWheelsBodyController.class ) ) {
                        wheelsController = (TwoWheelsBodyController) bodyController.getController( TwoWheelsBodyController.class );
                        CheckThread checkThread;
                        dtAngle = robotWrap.odometryAngle;
                        corrSpeedLeft = 0;
                        corrSpeedRight = 0;
                        while(true) {
                            correctionCode(robotWrap.currentDirection);
                            checkThread = new CheckThread(startPath,wheelsController);
                            checkThread.setRunning(true);
                            checkThread.start();
                            sleep(500);
                            checkThread.setRunning(false);
                            if (checkThread.stopFlag)
                                return;
                        }
                    }
                } catch (ControllerException e) {} catch (InterruptedException e) {}
            }
        }

        private void correctionCode(int currentDirection) {
            float angle = robotWrap.odometryAngle;
            //lead all directions to 0 direction
            switch(currentDirection) {
                case 1:
                    angle += Math.PI/2;
                    dtAngle += Math.PI/2;
                    break;
                case 2:
                    angle += -angle/Math.abs(angle)*Math.PI;
                    dtAngle += -angle/Math.abs(angle)*Math.PI;
                    break;
                case 3:
                    angle -= Math.PI/2;
                    dtAngle -= Math.PI/2;
                    break;
            }
            //now correct as 0 direction
            if (Math.abs(angle) < rangeValidDeviation) {
                wheelsController.setWheelsSpeeds(standardSpeed, standardSpeed);
                Log.i(MainActivity.TAG,"OK");
            } else
            if (!(angle > rangeValidDeviation)) {
                Log.i(MainActivity.TAG, "correct in LEFT "+mainActivity.robotWrap.odometryAngle);
                if (-angle + dtAngle > 0) {
                    corrSpeedLeft += correctionSpeedCorrection;
                    Log.i(MainActivity.TAG, "derivative > 0");
                } else {
                    if (corrSpeedLeft != 0)
                        corrSpeedLeft -= correctionSpeedCorrection;
                    Log.i(MainActivity.TAG, "derivative < 0");
                }
                wheelsController.setWheelsSpeeds(standardSpeed, standardSpeed + correctionSpeed + corrSpeedLeft);
                dtAngle = robotWrap.odometryAngle;
            } else {
                Log.i(MainActivity.TAG, "correct in RIGHT "+mainActivity.robotWrap.odometryAngle);
                if (-dtAngle + angle > 0) {
                    corrSpeedRight += correctionSpeedCorrection;
                    Log.i(MainActivity.TAG, "derivative > 0");
                } else {
                    if (corrSpeedRight != 0)
                        corrSpeedRight -= correctionSpeedCorrection;
                    Log.i(MainActivity.TAG, "derivative < 0");
                }
                wheelsController.setWheelsSpeeds(standardSpeed + correctionSpeed + corrSpeedRight, standardSpeed);
                dtAngle = robotWrap.odometryAngle;
            }
        }

        private class CheckThread extends Thread {
            private float startPath;
            private TwoWheelsBodyController wheelsController;
            private boolean running = false;
            public boolean stopFlag = false;
            CheckThread(float s,TwoWheelsBodyController w) {
                startPath = s;
                wheelsController = w;
            }
            @Override
            public void run() {
                while(running) {
                    if(!(mainActivity.robotWrap.odometryPath - startPath < purposePath)) {
                        wheelsController.setWheelsSpeeds(0.0f, 0.0f);
                        stopFlag = true;

                        int n = ((int)(2*(mainActivity.robotWrap.odometryPath - startPath)))/((int) (2* SQUARE_WIDTH));
                        if (n > counterForChangeCoords) {
                            switch (robotWrap.currentDirection) {
                                case 0:
                                    robotWrap.currentCellX++;
                                    break;
                                case 1:
                                    robotWrap.currentCellY++;
                                    break;
                                case 2:
                                    robotWrap.currentCellX--;
                                    break;
                                case 3:
                                    robotWrap.currentCellY--;
                                    break;
                            }
                            counterForChangeCoords++;
                        //    robotWrap.writeCurrentPositionOnServerDisplay();
                            mainActivity.displayRobotPosition();
                        }
                        return;
                    } else {
                        int n = ((int)(2*(mainActivity.robotWrap.odometryPath - startPath)))/((int) (2* SQUARE_WIDTH));
                        if (n > counterForChangeCoords) {
                            switch (robotWrap.currentDirection) {
                                case 0:
                                    robotWrap.currentCellX++;
                                    break;
                                case 1:
                                    robotWrap.currentCellY++;
                                    break;
                                case 2:
                                    robotWrap.currentCellX--;
                                    break;
                                case 3:
                                    robotWrap.currentCellY--;
                                    break;
                            }
                            counterForChangeCoords++;
                       //     robotWrap.writeCurrentPositionOnServerDisplay();
                            mainActivity.displayRobotPosition();
                        }
                    }
                }
            }
            public void setRunning(boolean r) {
                running = r;
            }
        }
    }

    /**
     * Turn robot on angle
     */
    class TurnThread extends Thread {
        private float startAngle;
        private float purposeDifferenceAngle;
        private float currentAngle;

        /**
         * @param angle difference between old and new angle, if > 0 - left turn, if < 0 - right turn
         */
        TurnThread(float angle) {
            startAngle = robotWrap.odometryAngle;
            purposeDifferenceAngle = (float) (Math.PI/2);
        /*    if (Math.abs(angle) < Math.PI)
                purposeDifferenceAngle = angle;
            else
                purposeDifferenceAngle = angle % (float)Math.PI;*/
            currentAngle = 0;
        }

        @Override
        public void run() {
            if (robotWrap.robot.isControllerAvailable(BodyController.class)) {
                try {
                    BodyController bodyController = (BodyController) robotWrap.robot.getController(BodyController.class);
                    if (bodyController.isControllerAvailable(TwoWheelsBodyController.class)) {
                        TwoWheelsBodyController wheelsController = (TwoWheelsBodyController) bodyController.getController(TwoWheelsBodyController.class);
                        wheelsController.turnAround(10f, purposeDifferenceAngle);
                        Log.i(MainActivity.TAG, "turnAround");
                        Log.i(MainActivity.TAG, "startAngle " + startAngle);
                        Log.i(MainActivity.TAG, "odometryAngle " + robotWrap.odometryAngle);
                        while (true) {
                            currentAngle = robotWrap.odometryAngle - startAngle;
                            if (currentAngle > purposeDifferenceAngle) {
                                wheelsController.setWheelsSpeeds(0.0f, 0.0f);
                                try {
                                    sleep(200);
                                } catch (InterruptedException e) {}
                                Log.i(MainActivity.TAG, "currentAngle "+ currentAngle);
                                Log.i(MainActivity.TAG, "TurnThread finished ------------>>>>> ");
                                return;
                            }
                        }
                    }
                } catch (ControllerException e) {}
            }
        }
    }

    /**
     * Left turn on PI/2 angle
     */
    class LeftThread extends Thread {
        private float startAngle;
        private float purposeAngle;

        LeftThread() {
            startAngle = mainActivity.robotWrap.odometryAngle;
            purposeAngle = (float) Math.PI / 2;
        }

        @Override
        public void run() {
            Log.i(MainActivity.TAG, "LeftThread started ----->>>>>>" + startAngle);

            int flagVariant;
            if ((startAngle >= 0) && (startAngle < Math.PI / 2)) {
                flagVariant = 1;
                Log.i(MainActivity.TAG, "flag 1");
            } else if (startAngle >= Math.PI / 2) {
                flagVariant = 2;
                Log.i(MainActivity.TAG, "flag 2");
            } else if (startAngle < -Math.PI / 2) {
                flagVariant = 3;
                Log.i(MainActivity.TAG, "flag 3");
            } else {
                flagVariant = 4;
                Log.i(MainActivity.TAG, "flag 4");
            }

            if (robotWrap.robot.isControllerAvailable(BodyController.class)) {
                Log.i(MainActivity.TAG,"BodyController available");
                BodyController bodyController = null;
                try {
                    bodyController = (BodyController) robotWrap.robot.getController(BodyController.class);
                    if (bodyController.isControllerAvailable(TwoWheelsBodyController.class)) {
                        Log.i(MainActivity.TAG,"TwoWheelsBodyController available");
                        TwoWheelsBodyController wheelsController = null;
                        wheelsController = (TwoWheelsBodyController) bodyController.getController(TwoWheelsBodyController.class);
                        wheelsController.turnAround(10f, (float) Math.PI / 2);
                        Log.i(MainActivity.TAG, "turnAround");
                        while (true) {
                            if (new FlagVariant(flagVariant).getFlag()) {
                            } else {
                                wheelsController.setWheelsSpeeds(0.0f, 0.0f);
                                try {
                                    sleep(200);
                                } catch (InterruptedException e) {}
                                Log.i(MainActivity.TAG, "LeftThread finished ------------>>>>> " + new FlagVariant(flagVariant).getDimension());
                                return;
                            }
                        }
                    }
                } catch (ControllerException e) {}
            }
        }

        class FlagVariant {
            private int variant = 0;

            FlagVariant(int v) {
                variant = v;
            }

            public boolean getFlag() {
                float currentAngle = mainActivity.robotWrap.odometryAngle;
                switch (this.variant) {
                    case 1:
                        if (currentAngle > 0)
                            return ((currentAngle - startAngle) < purposeAngle);
                        else
                            return false;
                    case 2:
                        if (currentAngle < 0)
                            currentAngle += 2 * Math.PI;
                        return ((currentAngle - startAngle) < purposeAngle);
                    case 3:
                        return ((currentAngle - startAngle) < purposeAngle);
                    case 4:
                        return ((currentAngle - startAngle) < purposeAngle);
                    default:
                        return true;
                }
            }

            public float getDimension() {
                float currentAngle = mainActivity.robotWrap.odometryAngle;
                switch (this.variant) {
                    case 1:
                        return (currentAngle - startAngle);
                    case 2:
                        if (currentAngle < 0)
                            currentAngle += 2 * Math.PI;
                        return (currentAngle - startAngle);
                    case 3:
                        return (currentAngle - startAngle);
                    case 4:
                        return (currentAngle - startAngle);
                    default:
                        return 0;
                }
            }
        }
    }

    /**
     * Right turn on PI/2 angle
     */
    class RightThread extends Thread {
        private float startAngle;
        private float purposeAngle;

        RightThread() {
            startAngle = mainActivity.robotWrap.odometryAngle;
            purposeAngle = (float) Math.PI / 2;
        }

        @Override
        public void run() {
            Log.i(MainActivity.TAG, "RightThread started ----->>>>>>" + startAngle);

            int flagVariant;
            if ((startAngle <= 0) && (startAngle > -Math.PI / 2)) {
                flagVariant = 1;
                Log.i(MainActivity.TAG, "flag 1");
            } else if (startAngle <= -Math.PI / 2) {
                flagVariant = 2;
                Log.i(MainActivity.TAG, "flag 2");
            } else if (startAngle > Math.PI / 2) {
                flagVariant = 3;
                Log.i(MainActivity.TAG, "flag 3");
            } else {
                flagVariant = 4;
                Log.i(MainActivity.TAG, "flag 4");
            }

            if (robotWrap.robot.isControllerAvailable(BodyController.class)) {
                Log.i(MainActivity.TAG,"BodyController available");
                BodyController bodyController = null;
                try {
                    bodyController = (BodyController) robotWrap.robot.getController(BodyController.class);
                    if (bodyController.isControllerAvailable(TwoWheelsBodyController.class)) {
                        Log.i(MainActivity.TAG,"TwoWheelsBodyController available");
                        TwoWheelsBodyController wheelsController = null;
                        wheelsController = (TwoWheelsBodyController) bodyController.getController(TwoWheelsBodyController.class);
                        wheelsController.turnAround(10f,(float)-Math.PI/2);
                        Log.i(MainActivity.TAG, "turnAround");
                        while (true) {
                            if (new FlagVariant(flagVariant).getFlag()) {
                            } else {
                                Log.i(MainActivity.TAG,"STOP");
                                wheelsController.setWheelsSpeeds(0.0f, 0.0f);
                                try {
                                    sleep(200);
                                } catch (InterruptedException e) {}
                                Log.i(MainActivity.TAG, "RightThread finished ------------>>>>> " + new FlagVariant(flagVariant).getDimension());
                                return;
                            }
                        }
                    }
                } catch (ControllerException e) {
                }
            }
        }

        class FlagVariant {
            private int variant = 0;

            FlagVariant(int v) {
                variant = v;
            }

            public boolean getFlag() {
                float currentAngle = mainActivity.robotWrap.odometryAngle;
                switch (this.variant) {
                    case 1:
                        if(currentAngle < 0)
                            return ((startAngle - currentAngle) < purposeAngle);
                        else
                            return false;
                    case 2:
                        if (currentAngle > 0)
                            currentAngle -= 2 * Math.PI;
                        return ((startAngle - currentAngle) < purposeAngle);
                    case 3:
                        return ((startAngle - currentAngle) < purposeAngle);
                    case 4:
                        return ((startAngle - currentAngle) < purposeAngle);
                    default:
                        return true;
                }
            }

            public float getDimension() {
                float currentAngle = mainActivity.robotWrap.odometryAngle;
                switch (this.variant) {
                    case 1:
                        return Math.abs(currentAngle - startAngle);
                    case 2:
                        if (currentAngle < 0)
                            currentAngle -= 2 * Math.PI;
                        return Math.abs(currentAngle - startAngle);
                    case 3:
                        return Math.abs(currentAngle - startAngle);
                    case 4:
                        return Math.abs(currentAngle - startAngle);
                    default:
                        return 0;
                }
            }
        }
    }

    //TODO
    private void arrayInList() {
        for(int i=0;i<arrayPath.length;i++)
            path.add(arrayPath[i]);
    }
}
