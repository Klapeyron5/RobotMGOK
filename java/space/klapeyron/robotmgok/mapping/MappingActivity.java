package space.klapeyron.robotmgok.mapping;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import space.klapeyron.robotmgok.R;

public class MappingActivity extends Activity {
    private RobotWrapMapping robotWrap;
    private RobotMoveControl robotMoveControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mapping);
        initConstructor();
    }

    private void initConstructor() {
        robotWrap = new RobotWrapMapping(this);
        robotMoveControl = new RobotMoveControl(robotWrap);
    }
}
