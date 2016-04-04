package space.klapeyron.robotmgok;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;
import space.klapeyron.robotmgok.InteractiveMap.InteractiveMapView;

public class MainActivity extends Activity {
    public static final String TAG = "TAG";

    public String serverState;
    public static final String SERVER_WAITING_ROBOT = "waiting robot";
    public static final String SERVER_WAITING_NEW_TASK = "waiting new task";
    public static final String SERVER_EXECUTING_TASK = "executing task";

    private String serverActivityState;
    private static final  String ACTIVITY_STATE_MAIN_XML = "main.xml";
    private static final  String ACTIVITY_STATE_INTERACTIVE_MAP = "interactive map";

    public String robotConnectionState;

    public String clientConnectionState;



    private static final int REQUEST_ENABLE_BT = 0; //>=0 for run onActivityResult from startActivityForResult
    private static final String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";
    private static final String SERVICE_NAME = "RBotApp"; //имя приложения сервера (для проверки входящих блютуз-запросов)

    BluetoothAdapter bluetoothAdapter; //локальный БТ адаптер
    private Set<BluetoothDevice> pairedDevices; //спаренные девайсы
    private BluetoothDevice clientDevice; //девайс клиента (для восстановления связи при потере сокета)
    private BluetoothSocket clientSocket; //канал соединения с последним клиентом
    private ReadIncomingMessage readIncomingMessage;

    ru.rbot.android.bridge.service.robotcontroll.robots.Robot robot;
    RobotWrap robotWrap;
    MainActivity link = this;
    InteractiveMapView interactiveMapView;
    TaskHandler taskHandler;
    TTSManager TTS = new TTSManager() ;


    //customizing server interface
    private TextView textViewServerState;
    private TextView textViewRobotConnectionState;
    private TextView textViewClientConnectionState;
    public TextView textViewCountedPath;
    public TextView textViewOdometryPath;
    public TextView textViewOdometryAngle;
    public TextView textViewOdometryX;
    public TextView textViewOdometryY;
    public TextView textViewOdometrySpeedL;
    public TextView textViewOdometrySpeedR;
    public EditText editTextFinishX;
    public EditText editTextFinishY;
    public EditText editTextStartX;
    public EditText editTextStartY;
    public EditText editTextDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        serverActivityState = ACTIVITY_STATE_MAIN_XML;

        Log.i(TAG, "OnCreate()");

        initConstructor();
        TTS.init(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = bluetoothAdapter.getBondedDevices(); //получаем список сопряженных устройств
        AcceptIncomingConnection acceptIncomingConnection = new AcceptIncomingConnection();
        acceptIncomingConnection.start(); //запускаем серверную прослушку входящих БТ запросов

        robotWrap = new RobotWrap(this);
        taskHandler = new TaskHandler(link);

        setClientConnectionState("hasn't been connected");

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                Log.i(TAG, "Bluetooth.isEnable");
                initConstructor();
            } else {
                //start BT
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            //TODO device does not support BT
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            robotWrap = new RobotWrap(this);
            taskHandler = new TaskHandler(link);
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            pairedDevices = bluetoothAdapter.getBondedDevices(); //получаем список сопряженных устройств
            AcceptIncomingConnection acceptIncomingConnection = new AcceptIncomingConnection();
            acceptIncomingConnection.start(); //запускаем серверную прослушку входящих БТ запросов
        }
        if (resultCode == RESULT_CANCELED) {
            robotWrap = new RobotWrap(this);
            taskHandler = new TaskHandler(link);
            setServerState("bluetooth off");
            setRobotConnectionState("bluetooth off");
            setClientConnectionState("bluetooth off");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initConstructor() {
        registerReceiver(incomingPairRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));

        textViewServerState = (TextView) findViewById(R.id.textViewServerState);
        textViewClientConnectionState = (TextView) findViewById(R.id.textViewClientConnectionState);
        textViewRobotConnectionState = (TextView) findViewById(R.id.textViewRobotConnectionState);
        textViewCountedPath = (TextView) findViewById(R.id.textViewCountedPath);
        textViewOdometryPath = (TextView) findViewById(R.id.textViewOdometryPath);
        textViewOdometryX = (TextView) findViewById(R.id.textViewOdometryX);
        textViewOdometryY = (TextView) findViewById(R.id.textViewOdometryY);
        textViewOdometrySpeedL = (TextView) findViewById(R.id.textViewOdometrySpeedL);
        textViewOdometrySpeedR = (TextView) findViewById(R.id.textViewOdometrySpeedR);
        textViewOdometryAngle = (TextView) findViewById(R.id.textViewOdometryAngle);

        editTextFinishX = (EditText) findViewById(R.id.editTextFinishX);
        editTextFinishY = (EditText) findViewById(R.id.editTextFinishY);
        editTextStartX = (EditText) findViewById(R.id.editTextStartX);
        editTextStartY = (EditText) findViewById(R.id.editTextStartY);
        editTextDirection = (EditText) findViewById(R.id.editTextStartDirection);

        Button buttonMap = (Button) findViewById(R.id.buttonMap);
        buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
         //       Intent intent = new Intent(MainActivity.this, InteractiveMapActivity.class);
         //       startActivity(intent);
                interactiveMapView = new InteractiveMapView(link,robotWrap.currentCellX,robotWrap.currentCellY);
                setContentView(interactiveMapView);
                serverActivityState = ACTIVITY_STATE_INTERACTIVE_MAP;
            }
        });

        Button buttonReconnectToRobot = (Button) findViewById(R.id.buttonReconnectToRobot);
        buttonReconnectToRobot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                robotWrap.reconnect();
            }
        });

        Button buttonBTOpen = (Button) findViewById(R.id.buttonBtOpen);
        buttonBTOpen.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                makeDiscoverable(v);
            }
        });

        Button buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO //current state is wrong (coordinates)
                stopRiding();
            }
        });

        Button buttonSetStart = (Button) findViewById(R.id.buttonSetTask);
        buttonSetStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                robotWrap.setStartCoordinatesByServerEditText();
            }
        });

        Button buttonSetTask = (Button) findViewById(R.id.buttonSetTask);
        buttonSetTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.i(TAG,"1");
                    int fY = Integer.parseInt(editTextFinishY.getText().toString());
                    int fX = Integer.parseInt(editTextFinishX.getText().toString());
                    Log.i(TAG,"2");
                    taskHandler.setTask(fX, fY);
                } catch (ControllerException e) {e.printStackTrace();}
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //replaces the default 'Back' button action
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            switch(serverActivityState) {
                case ACTIVITY_STATE_MAIN_XML:
                    break;
                case ACTIVITY_STATE_INTERACTIVE_MAP:
                    setContentView(R.layout.main);
                    initConstructor();
                    setServerState(serverState);
                    setRobotConnectionState(robotConnectionState);
                    setClientConnectionState(clientConnectionState);
                    editTextFinishX.setText(Integer.toString(taskHandler.finishX));
                    editTextFinishY.setText(Integer.toString(taskHandler.finishY));
                    robotWrap.writeCurrentPositionOnServerDisplay();
                    serverActivityState = ACTIVITY_STATE_MAIN_XML;
                    break;
            }
        }
        return false;
    }

    public void displayRobotPosition() {
        switch(serverActivityState) {
            case ACTIVITY_STATE_MAIN_XML:
                Log.i(TAG,"displayRobotPosition() ACTIVITY_STATE_MAIN_XML");
                synchronized (this) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editTextStartX.setText(Integer.toString(robotWrap.currentCellX));
                            editTextStartY.setText(Integer.toString(robotWrap.currentCellY));
                            editTextDirection.setText(Integer.toString(robotWrap.currentDirection));
                        }
                    });
                }
                break;
            case ACTIVITY_STATE_INTERACTIVE_MAP:
                Log.i(TAG,"displayRobotPosition() ACTIVITY_STATE_INTERACTIVE_MAP");
                synchronized (this) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            interactiveMapView = new InteractiveMapView(link,robotWrap.currentCellX,robotWrap.currentCellY);
                            setContentView(interactiveMapView);
                        }
                    });
                }
                break;
        }
        sendMessage("currentXY", robotWrap.currentCellX, robotWrap.currentCellY);
    }

    public void stopRiding() {
        if (taskHandler.runningLowLevelThread != null)
            taskHandler.runningLowLevelThread.interrupt();
        if(taskHandler.runningTaskThread != null)
            taskHandler.runningTaskThread.interrupt();
        //TODO
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setServerState(SERVER_WAITING_NEW_TASK);
            }
        });
        finishedWorkWithBtClient();
    }

    public void makeDiscoverable(View view) {
        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(i);
    }

    public void setServerState(String state) {
        serverState = state;
        textViewServerState.setText(state);
    }

    public void setRobotConnectionState(String state) {
        robotConnectionState = state;
        textViewRobotConnectionState.setText(state);
    }

    public void setClientConnectionState(String state) {
        clientConnectionState = state;
        textViewClientConnectionState.setText(state);
    }


    private final BroadcastReceiver incomingPairRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("TAG", "incomingPairRequestReceiver");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                Log.i("TAG", "запрос на сопряжение");
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("TAG", "запрос с устройства: " + dev.getName());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    dev.setPairingConfirmation(true);
                    Log.i("TAG", "спаривание успешно");
                } else {
                    Log.i("TAG", "невозможно автоматически произвести спаривание, версия ниже КИТКАТа");
                }
            }
        }
    };

    /**
     * Send message to client, if you not send current coordinates, set X = 0, Y = 0.
     * @param key "ready": robot found client and wait target X,Y;
     *            "path": ideal path;
     *            "currentXY":  X and Y is current robot coordinates;
     *            "target": robot reached target
     * @param X current robot X coordinate
     * @param Y current robot Y coordinate*/
    private void sendMessage(String key, int X, int Y) {
        String str = new String("/"+key+"/"+Integer.toString(X)+"/"+Integer.toString(Y)+"/");
        if (key.equals("path")) {
            for(int i = 0;i<taskHandler.absolutePath.size();i++) {
                str = str + Integer.toString(taskHandler.absolutePath.get(i));
            }
            str = str + "/";
            Log.i(TAG,"str");
        }
        byte[] b = str.getBytes();
        try {
            Log.i(TAG, "send 1");
            (clientSocket.getOutputStream()).write(b);
            Log.i(TAG, "send 2");
            Log.i(TAG, new String(b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class AcceptIncomingConnection extends Thread {
        @Override
        public void run() {
            Log.i(TAG,"AcceptIncomingMessage");
            acceptIncomingConnectionMethod();
        }
    }

    private class ReadIncomingMessage extends Thread {
        @Override
        public void run() {
            readIncomingMessageMethod();
        }
    }

    private void acceptIncomingConnectionMethod() {
        BluetoothServerSocket serverSocket = null;
        try {
            Log.i(TAG, "1");
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, java.util.UUID.fromString(UUID));
            clientSocket = serverSocket.accept();
            Log.i(TAG, "2");
        } catch (IOException e) {
            Log.i(TAG, "3");
        }
        Log.i(TAG, "4");
        if (clientSocket != null)
            clientDevice = clientSocket.getRemoteDevice(); //запоминаем клиента
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setClientConnectionState("Connected");
            }
        });
        sendMessage("ready",robotWrap.currentCellX,robotWrap.currentCellY);
     //   ReadIncomingMessage readIncomingMessage = new ReadIncomingMessage();
        readIncomingMessage = new ReadIncomingMessage();
        readIncomingMessage.start();
        try {
            Log.i(TAG, "5");
            serverSocket.close();
        } catch (IOException e) {
            Log.i(TAG, "6");
        }
    }

    private void readIncomingMessageMethod() {
        InputStream inputStream = null;
        try {
            inputStream = clientSocket.getInputStream();
        } catch (IOException e) {}

        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        try {
            bytes = inputStream.read(buffer);
        } catch (IOException e) {}
        String str = null;
        try {
            try {
                str = new String(buffer, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            Log.i(TAG, "in reading " + readIncomingMessage.getId());
            Log.i(TAG, "reading:  " + str);
            String[] a = str.split("/");
            Log.i(TAG, "!" + a[0] + "!" + a[1] + "!" + a[2] + "!" + a[3]);
            final String key = a[1];



            if ((key.equals("task"))&&(serverState == SERVER_WAITING_NEW_TASK)) {
                final String X = a[2];
                final String Y = a[3];
                int fX = Integer.parseInt(X.toString());
                int fY = Integer.parseInt(Y.toString());
                try {
                    taskHandler.setTask(fX, fY);
                    sendMessage("path",robotWrap.currentCellX,robotWrap.currentCellY);
                } catch (ControllerException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setServerState(SERVER_EXECUTING_TASK);
                        setClientConnectionState("task: " + X + " " + Y);
                    }
                });
            }
            if (!key.equals("stop")) {
            //   ReadIncomingMessage readIncomingMessage = new ReadIncomingMessage();
                readIncomingMessage = new ReadIncomingMessage();
                readIncomingMessage.start();
            }
            if (key.equals("stop")) {
                Log.i(TAG, "key == stop");
                stopRiding();
            }
        } catch(IndexOutOfBoundsException e) {
            Log.i(TAG,"IndexOutOfBoundsException");
        }
    }

    private void finishedWorkWithBtClient() {
        sendMessage("target", 0, 0);
        //start listening new client task
        Log.i(TAG, "Close readIncomingMessage 1 "+ readIncomingMessage.getId());
        if ((readIncomingMessage != null)&&(readIncomingMessage.isAlive())) {
            readIncomingMessage.interrupt();
            Log.i(TAG, "Close readIncomingMessage 2 "+ readIncomingMessage.getId());
        }
        try {
            Log.i(TAG, "1f");
            if (clientSocket.isConnected()) {
                Log.i(TAG, "2f");
                clientSocket.close();

            }
        } catch (IOException e) {
            Log.i(TAG, "3f");
            e.printStackTrace();
        }
        Log.i(TAG, "4f");
        AcceptIncomingConnection acceptIncomingConnection = new AcceptIncomingConnection();
        acceptIncomingConnection.start();
    }
}
