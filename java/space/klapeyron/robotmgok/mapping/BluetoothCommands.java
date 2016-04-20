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
    }

    private void turnLeft() {
        robotMoveControl.turnLeft();
        if(mainActivity.robotWrap.currentDirection!=0)
            mainActivity.robotWrap.currentDirection--;
        else
            mainActivity.robotWrap.currentDirection = 3;
    }

    private void turnRight() {
        robotMoveControl.turnRight();
        if(mainActivity.robotWrap.currentDirection!=3)
            mainActivity.robotWrap.currentDirection++;
        else
            mainActivity.robotWrap.currentDirection = 0;
    }

    private void measure() throws InterruptedException {
        mainActivity.robotWrap.currentCellX = 3;
        mainActivity.robotWrap.currentCellY = 13;
        mainActivity.robotWrap.currentDirection = 0;

        for (int i = 0; i < 4; i++) {
            mainActivity.startMeasure();
            turnRight();
            Log.i("TAG", "WTF" + Integer.toString(i));
        }
    }

    private void clearFile() throws InterruptedException {
        mainActivity.clearFile();
    }
}
