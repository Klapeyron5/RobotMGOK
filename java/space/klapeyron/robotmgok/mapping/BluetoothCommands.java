package space.klapeyron.robotmgok.mapping;

import space.klapeyron.robotmgok.MainActivity;

public class BluetoothCommands {

    private RobotMoveControl robotMoveControl;

    public BluetoothCommands(MainActivity m) {
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

    private void measure () {

    }
}
