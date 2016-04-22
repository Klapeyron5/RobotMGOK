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
        Log.i("TAG","Robot coords: "+mainActivity.robotWrap.currentCellX+" "+mainActivity.robotWrap.currentCellY+" "+mainActivity.robotWrap.currentDirection);
        Log.i("TAG", "BluetoothCommands moveForward Stop");
    }

    private void turnLeft() {
        Log.i("TAG", "BluetoothCommands turnLeft Start");
        robotMoveControl.turnLeft();
        Log.i("TAG","Robot coords: "+mainActivity.robotWrap.currentCellX+" "+mainActivity.robotWrap.currentCellY+" "+mainActivity.robotWrap.currentDirection);
        Log.i("TAG", "BluetoothCommands turnLeft Stop");
    }

    private void turnRight() {
        Log.i("TAG", "BluetoothCommands turnRight Start");
        robotMoveControl.turnRight();
        Log.i("TAG","Robot coords: "+mainActivity.robotWrap.currentCellX+" "+mainActivity.robotWrap.currentCellY+" "+mainActivity.robotWrap.currentDirection);
        Log.i("TAG", "BluetoothCommands turnRight Stop");
    }

    private void measure() throws InterruptedException {
        Log.i("TAG", "BluetoothCommands measure Start");
        mainActivity.robotWrap.currentCellX = 3;
        mainActivity.robotWrap.currentCellY = 13;
        mainActivity.robotWrap.currentDirection = 0;

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

    private void clearFile() throws InterruptedException {
        mainActivity.clearFile();
    }

    private void measureTurn (int k, String side) {
        for (int i = 0; i < k; i++) {
            //mainActivity.startMeasure();
            if (side.equals("left"))
                turnLeft();
            if (side.equals("right"))
                turnRight();
            Log.i("TAG", "measureTurn: " + Integer.toString(i));
        }
    }
}
