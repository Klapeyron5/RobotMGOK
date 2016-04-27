package space.klapeyron.robotmgok.mapping;

import android.util.Log;

import java.util.ArrayList;

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
            case "mapping measure distribution":
                Log.i("TAG","mapping measure distribution started");
                measureDistrubution();
                Log.i("TAG", "mapping measure distribution finished");
                break;
        }
    }

    private void moveForward() {
        robotMoveControl.moveForward();
    }

    private void turnLeft() {
        robotMoveControl.turnLeft();
    }

    private void turnRight() {
        robotMoveControl.turnRight();
    }

    private void measure() throws InterruptedException {
        mainActivity.robotWrap.currentCellX = 3;
        mainActivity.robotWrap.currentCellY = 13;
        mainActivity.robotWrap.currentDirection = 0;

     //   handleMappingPath(new int[]{0,0,0,1,2,2,2,1,0,0,0});

        neckUp();
        measureTurn(4,"left");

        //Rare shit:
        /*moveForward();
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
        measureTurn(4,"left");*/
    }

    private void neckUp() {
        robotMoveControl.neckUp();
    }

    private void clearFile() throws InterruptedException {
        mainActivity.clearFile();
    }

    private void measureDistrubution() {
        neckUp();
        mainActivity.startMeasureDistribution();
        turnRight();
    }

    private void handleMappingPath(int[] absPath) {
        neckUp();

        ArrayList<Integer> path = new ArrayList<>();

        //обработка четверичного массива в троичный (старт)
        //обработка начального положения (старт)
        if (Math.abs(mainActivity.robotWrap.currentDirection-absPath[0])>2) {
            turnRight();
        }

        for (int i=0;i<absPath.length;i++) {

        }
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
