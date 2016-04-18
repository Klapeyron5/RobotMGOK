package space.klapeyron.robotmgok.mapping;

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
                measure();
                break;
            case "mapping clear file":
                clearFile();
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

    private void measure () {
        mainActivity.robotWrap.currentCellX = 3;
        mainActivity.robotWrap.currentCellY = 13;
        mainActivity.robotWrap.currentDirection = 0;
        mainActivity.startMeasure();
    }

    private void clearFile() {
    }
}
