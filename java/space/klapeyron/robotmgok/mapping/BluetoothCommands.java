package space.klapeyron.robotmgok.mapping;

import android.util.Log;

import space.klapeyron.robotmgok.MainActivity;

public class BluetoothCommands {

    private RobotMoveControl robotMoveControl;
    private MainActivity mainActivity;

    public BluetoothCommands(MainActivity m) {
        mainActivity = m;
        robotMoveControl = new RobotMoveControl(m.robotWrap);
    }

    public void runFromBluetoothCommands(String key) {
        switch(key) {
            case "mapping forward":
                moveForward();
                break;
            case "mapping half pi left":
                turnLeft();
                break;
            case "mapping half pi right":
                turnRight();
                break;
            case "mapping measure":
                try {
                    measure();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "mapping clear file":
                try {
                    clearFile();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void moveForward() {
        Log.i("TAG", "BluetoothCommands moveForward Start");
        robotMoveControl.moveForward();
        switch (mainActivity.robotWrap.currentDirection) {
            case 0:
                mainActivity.robotWrap.currentCellX++;
                break;
            case 1:
                mainActivity.robotWrap.currentCellY++;
                break;
            case 2:
                mainActivity.robotWrap.currentCellX--;
                break;
            case 3:
                mainActivity.robotWrap.currentCellY--;
                break;
        }
        Log.i("TAG", "BluetoothCommands moveForward Stop");
    }

    private void turnLeft() {
        Log.i("TAG", "BluetoothCommands turnLeft Start");
        robotMoveControl.turnLeft();
        if(mainActivity.robotWrap.currentDirection!=0)
            mainActivity.robotWrap.currentDirection--;
        else
            mainActivity.robotWrap.currentDirection = 3;
        Log.i("TAG", "BluetoothCommands turnLeft Stop");
    }

    private void turnRight() {
        Log.i("TAG", "BluetoothCommands turnRight Start");
        robotMoveControl.turnRight();
        if(mainActivity.robotWrap.currentDirection!=3)
            mainActivity.robotWrap.currentDirection++;
        else
            mainActivity.robotWrap.currentDirection = 0;
        Log.i("TAG", "BluetoothCommands turnRight Stop");
    }

    private void measure() throws InterruptedException {
        Log.i("TAG", "BluetoothCommands measure Start");
        mainActivity.robotWrap.currentCellX = 3;
        mainActivity.robotWrap.currentCellY = 13;
        mainActivity.robotWrap.currentDirection = 0;

        moveForward();
        for (int i = 0; i < 4; i++) {
            mainActivity.startMeasure();
            turnRight();
            Log.i("TAG", "WTF" + Integer.toString(i));
        }
        moveForward();
        for (int i = 0; i < 4; i++) {
            mainActivity.startMeasure();
            turnRight();
            Log.i("TAG", "WTF" + Integer.toString(i));
        }
        Log.i("TAG", "BluetoothCommands measure Stop");
    }

    private void clearFile() throws InterruptedException {
        mainActivity.clearFile();
    }
}
