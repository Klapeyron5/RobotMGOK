package space.klapeyron.robotmgok.mapping;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void measure() throws InterruptedException {
    //    mainActivity.robotWrap.currentCellX = 3;
    //    mainActivity.robotWrap.currentCellY = 13;
    //    mainActivity.robotWrap.currentDirection = 0;

        neckUp();
        mainActivity.startMeasure();
    //    handleMappingPath(new int[]{0,0,1,2,2,1,0,0});

    /*    measureTurn(4,"left");
        for (int i=0;i<3;i++) {
            moveForward();
            measureTurn(4,"left");
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {}
            }
        };
        thread.start();
        thread.join();*/

        File fileName = null;
        FileOutputStream os = null;
        if (isExternalStorageWritable()) {
            File sdDir = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(sdDir.getAbsolutePath() + "/Alarms/");
            dir.mkdir();
            fileName = new File(dir, "line13.txt");
            try {
                os = new FileOutputStream(fileName, true);
                os.write(mainActivity.dateMapping.getBytes());
                os.close();
            } catch (FileNotFoundException e) {} catch (IOException e) {}
        }


  /*      measureTurn(4,"left");
        moveForward();
        measureTurn(4,"left");

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

        if (Math.abs(mainActivity.robotWrap.currentDirection-absPath[0])>2) {
            turnRight();
        }

        for (int i=0;i<absPath.length;i++) {
            int dir = mainActivity.robotWrap.currentDirection;
            if (dir == absPath[i]) {
                Log.i("TAG","dir == absPath[i]");
                measureTurn(4, "right");
            }
            else {
                if (((dir!=3)&&(absPath[i]>dir))||((dir==3)&&(absPath[i]==0))) {
                    Log.i("TAG","if 1");
                    measureTurn(3, "left");
                }
                else {
                    Log.i("TAG","if 2");
                    measureTurn(3, "right");
                }
            }
            Log.i("TAG","move forward");
            moveForward();
        }
        measureTurn(4,"right");
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
