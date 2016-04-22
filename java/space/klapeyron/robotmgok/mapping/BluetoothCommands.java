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
        Log.i("TAG", "BluetoothCommands moveForward Stop");
    }

    private void turnLeft() {
        robotMoveControl.turnLeft();
    }

    private void turnRight() {
        robotMoveControl.turnRight();
    }

    private void measure() throws InterruptedException {
        Log.i("TAG", "BluetoothCommands measure Start");
        mainActivity.robotWrap.currentCellX = 3;
        mainActivity.robotWrap.currentCellY = 13;
        mainActivity.robotWrap.currentDirection = 0;

        neckUp();
        measureTurn(4,"left");
        moveForward();
        measureTurn(4, "left");
        moveForward();
        measureTurn(3, "left");
        moveForward();

        measureTurn(3, "left");
        moveForward();
        measureTurn(4, "left");
        moveForward();
        measureTurn(3, "right");
        moveForward();

        measureTurn(3,"right");
        moveForward();
        measureTurn(4,"left");
        moveForward();
        measureTurn(4,"left");

        Log.i("TAG", "BluetoothCommands measure Stop");
    }

    private void neckUp() {
        robotMoveControl.neckUp();
    }

    private void clearFile() throws InterruptedException {
        mainActivity.clearFile();
    }

    private void measureTurn (int k, String side) {
        for (int i = 0; i < k; i++) {
            mainActivity.startMeasure();
            if (side.equals("left"))
                turnLeft();
            if (side.equals("right"))
                turnRight();
        }
        if (k==3)
            mainActivity.startMeasure();
    }
}
