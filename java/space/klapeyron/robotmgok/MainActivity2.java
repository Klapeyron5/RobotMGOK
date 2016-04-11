package space.klapeyron.robotmgok;

import android.annotation.TargetApi;
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
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;
import space.klapeyron.robotmgok.InteractiveMap.InteractiveMapView;

public class MainActivity2 extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;

        setContentView(R.layout.main2);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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





///////////VARIABLES///////////(start)
    /**Temporary for manually debugging.*/
    public static final String TAG = "TAG";

    /**Current server state.*/
    private String serverState;
    private static final String SERVER_WAITING_ROBOT = "waiting robot";
    private static final String SERVER_WAITING_NEW_TASK = "waiting new task";
    private static final String SERVER_EXECUTING_TASK = "executing task";
    private static final String SERVER_CALIBRATING_BY_BEACONS = "calibrate by beacons"; //TODO
    private static final String SERVER_CALIBRATING_BY_QR = "calibrate by QR"; //TODO

    /**Current state of connection server android device with robot.*/
    public String robotConnectionState;

    /**Current state of connection server android device with client mobile device.*/
    public String clientConnectionState;

    private static final int REQUEST_ENABLE_BT = 0; //>=0 for run onActivityResult from startActivityForResult
    private static final String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";
    private static final String SERVICE_NAME = "RobotServerMGOK"; //name this app for incoming bluetooth requests

    BluetoothAdapter bluetoothAdapter; //локальный БТ адаптер
    private Set<BluetoothDevice> pairedDevices; //спаренные девайсы
    private BluetoothDevice clientDevice; //девайс клиента (для восстановления связи при потере сокета)
    private BluetoothSocket clientSocket; //канал соединения с последним клиентом
    private ReadIncomingMessage readIncomingMessage;

    ru.rbot.android.bridge.service.robotcontroll.robots.Robot robot;
    RobotWrap robotWrap;
    MainActivity2 link = this;
    InteractiveMapView interactiveMapView;
    TaskHandler taskHandler;
    TTSManager TTS = new TTSManager() ;

    //customizing server interface
    public int screenWidth;
    public int screenHeight;
    private TextView textViewServerState;
    private TextView textViewRobotConnectionState;
    private TextView textViewClientConnectionState;
    public TextView textViewCountedPath;
    public TextView textViewOdometryPath;
    public TextView textViewOdometryAngle;
    public EditText editTextStartX;
    public EditText editTextStartY;
    public EditText editTextDirection;

//////***BLE***
    ArrayList<String> beacons = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 200;
    private boolean isScanning = false;

    //Данные выборки для сглаживания значений мощности сигнала и расстояния
    int[][] rssiData = new int[10][2];
    double[][] distanceData = new double[10][2];
    double d = 0;
    int f = 0; // флажок

    //Доступные маячки
    ArrayList<String> MAC = new ArrayList<>();
    ArrayList<Double> distance = new ArrayList<>();

    //mac-addresses
    String mac[] = {
            //Test-MACs:
            /*"F4:B8:5E:DE:9D:0D",
            "F4:B8:5E:DE:BA:55",
            "F4:B8:5E:DE:D5:E7"*/

            //Control MACs:
            "F4:B8:5E:DE:CA:B4",
            "F4:B8:5E:DE:CD:F5",
            "F4:B8:5E:DE:CD:DD"
            /*"F4:B8:5E:DE:9D:0D",
            "F4:B8:5E:DE:C2:8E",
            "F4:B8:5E:DE:BD:1C",
            "F4:B8:5E:DE:D5:E7",
            "F4:B8:5E:DE:BA:55",
            "F4:B8:5E:DE:CA:B4",
            "F4:B8:5E:DE:CD:F5",
            "F4:B8:5E:DD:EB:77",
            "F4:B8:5E:DE:CD:DD",
            "F4:B8:5E:DE:D5:B5"*/};

    double Distance[] = new double[3];
    String Mac[] = new String[3];
//////***BLE***(end)

///////////VARIABLES///////////(end)






    private void initConstructor() {
        //    new IntentIntegrator(this).initiateScan();
        registerReceiver(incomingPairRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));

        textViewServerState = (TextView) findViewById(R.id.textViewServerState);
        textViewClientConnectionState = (TextView) findViewById(R.id.textViewClientConnectionState);
        textViewRobotConnectionState = (TextView) findViewById(R.id.textViewRobotConnectionState);
        textViewCountedPath = (TextView) findViewById(R.id.textViewCountedPath);
        textViewOdometryPath = (TextView) findViewById(R.id.textViewOdometryPath);
        textViewOdometryAngle = (TextView) findViewById(R.id.textViewOdometryAngle);

        editTextStartX = (EditText) findViewById(R.id.editTextStartX);
        editTextStartY = (EditText) findViewById(R.id.editTextStartY);
        editTextDirection = (EditText) findViewById(R.id.editTextStartDirection);

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

        Button buttonCalibrate = (Button) findViewById(R.id.buttonCalibrate);
        buttonCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "START_THREAD");
                MoveToBeacons MoveToBeacons = new MoveToBeacons(link);
                MoveToBeacons.searchAndGo.start();
            }
        });

        Button buttonSetStart = (Button) findViewById(R.id.buttonSetStart);
        buttonSetStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                robotWrap.setStartCoordinatesByServerEditText();
            }
        });
    }

    public void displayRobotPosition() {
        Log.i(TAG, "displayRobotPosition() ACTIVITY_STATE_MAIN_XML");
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
        sendMessage("currentXY", robotWrap.currentCellX, robotWrap.currentCellY, robotWrap.currentDirection);
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
        synchronized (this) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewServerState.setText(serverState);
                }
            });
        }
    }

    public void setRobotConnectionState(String state) {
        robotConnectionState = state;
        synchronized (this) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewRobotConnectionState.setText(robotConnectionState);
                }
            });
        }
    }

    public void setClientConnectionState(String state) {
        clientConnectionState = state;
        synchronized (this) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewClientConnectionState.setText(clientConnectionState);
                }
            });
        }
    }




//////BLUETOOTH//////////(start)
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
    private void sendMessage(String key, int X, int Y, int dir) {
        String str = new String("/"+key+"/"+Integer.toString(X)+"/"+Integer.toString(Y)+"/"+Integer.toString(dir)+"/");
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
        sendMessage("ready",robotWrap.currentCellX,robotWrap.currentCellY,robotWrap.currentDirection);
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
                    sendMessage("path",robotWrap.currentCellX,robotWrap.currentCellY,robotWrap.currentDirection);
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
        sendMessage("target", 0, 0, 0);
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
//////BLUETOOTH//////////(end)










//////***BLE***(start)
    private Runnable scanRunnable = new Runnable()
    {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if (isScanning)
            {
                if (bluetoothAdapter != null)
                {
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }
            else
            {
                if (bluetoothAdapter != null)
                {
                    bluetoothAdapter.startLeScan(leScanCallback);
                }
            }

            isScanning = !isScanning;

            scanHandler.postDelayed(this, scan_interval_ms);
        }
    };

    public BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5)
            {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 &&
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15)
                {
                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound)
            {
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);

                byte txPower = scanRecord[29];

                int i,j,k = 0;

                String macBuf = device.toString();

                //Обновление маячков
                f = 0;
                if (MAC.size() != 0) {
                    for (i = 0; i < MAC.size(); i++) {
                        if (macBuf.equals(MAC.get(i))) {
                            optimizeRssiData(i,rssi);
                            d = distance(Integer.parseInt(Byte.toString(txPower)) - rssiData[i][0]);
                            optimizeDistanceData(i);
                            distanceData[i][0] = Math.round(distanceData[i][0]*100.00)/100.00;
                            distance.set(i, distanceData[i][0]);
                            f = 1;
                        }
                    }

                    if (f == 0) {
                        optimizeRssiData(i,rssi);
                        d = distance(Integer.parseInt(Byte.toString(txPower)) - rssiData[i][0]);
                        optimizeDistanceData(i);
                        distanceData[i][0] = Math.round(distanceData[i][0]*100.00)/100.00;
                        MAC.add(macBuf);
                        distance.add(distanceData[i][0]);
                    }
                }
                else {
                    MAC.add(macBuf);
                    distance.add(distanceData[0][0]);
                }

                //Запись в beacons
                if (MAC.size() != 0) {
                    beacons.clear();
                    for (i = 0; i < MAC.size(); i++) {
                        beacons.add("MAC: " + MAC.get(i) + "      distance:  " + distance.get(i).toString() + "m");
                    }
                }
                Log.i("MAC.size", Integer.toString(MAC.size()));

                if (MAC.size() > 2) {
                    String[] tmpMac = new String[MAC.size()];
                    for (i = 0; i < MAC.size(); i++){
                        tmpMac[i] = MAC.get(i);
                    }
                    double[] tmpDistance = new double[distance.size()];
                    for (i = 0; i < distance.size(); i++){
                        tmpDistance[i] = distance.get(i);
                    }
                    sort(tmpDistance, tmpMac);
                    for (i = 0; i < 3; i++) {
                        for (j = 0; j < mac.length; j++){
                            if (tmpMac[i].equals(mac[j])){
                                Distance[k] = tmpDistance[i];
                                Mac[k] = tmpMac[i];
                                k++;
                            }
                        }
                    }
                    Log.i("Distance(MA)", Double.toString(Distance[0]) + "  " + Double.toString(Distance[1]) + "  " + Double.toString(Distance[2]));
                    Log.i("Mac(MA)", Mac[0] + "  " + Mac[1] + "  " + Mac[2]);
                }
            }
        }
    };
    double distance(int power){
        if (power < 5) return (0.0399 * power + 0.7951);
        else return (Math.pow(10,((power - 1.6) / 20)));
    }

    void optimizeRssiData(int i, int rssi){
        rssiData[i][1] = rssiData[i][0];
        rssiData[i][0] = rssi;

        while ((rssiData[i][0] - rssiData[i][1]) > 2)
            rssiData[i][0] = (rssiData[i][0] + rssiData[i][1])/2;
    }

    void optimizeDistanceData(int i){
        distanceData[i][1] = distanceData[i][0];
        distanceData[i][0] = d;

        if (distanceData[i][0] < 0) distanceData[i][0] = 0;
        while ((distanceData[i][0] - distanceData[i][1]) > 1.5)
            distanceData[i][0] = (distanceData[i][0] + distanceData[i][1])/2;
    }

    public void sort(double[] arr, String[] str){
        for(int i = arr.length-1 ; i > 0 ; i--){
            for(int j = 0 ; j < i ; j++){
                if( arr[j] > arr[j+1] ){

                    double tmp = arr[j];
                    arr[j] = arr[j+1];
                    arr[j+1] = tmp;

                    String string = str[j];
                    str[j] = str[j+1];
                    str[j+1] = string;
                }
            }
        }
    }
//////***BLE***(end)





}
