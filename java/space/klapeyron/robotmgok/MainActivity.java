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
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

import ru.rbot.android.bridge.service.robotcontroll.exceptions.ControllerException;
import space.klapeyron.robotmgok.mapping.BluetoothCommands;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;

        setContentView(R.layout.main);
        initConstructor();

        setClientConnectionState(CLIENT_HASNT_BEEN_CONNECTED);

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothConstructor();
            } else {
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
            bluetoothConstructor();
        }
        if (resultCode == RESULT_CANCELED) {
            setServerState(SERVER_BLUETOOTH_OFF);
            setRobotConnectionState(SERVER_BLUETOOTH_OFF);
            setClientConnectionState(SERVER_BLUETOOTH_OFF);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }




///////////VARIABLES///////////(start)

    /**Temporary for manually debugging.*/
    public static final String TAG = "TAG";

    /**Current server state.*/
    private String serverState;
    public static final String SERVER_WAITING_ROBOT = "waiting robot";
    public static final String SERVER_WAITING_NEW_TASK = "waiting new task";
    public static final String SERVER_EXECUTING_TASK = "executing task";
    public static final String SERVER_CALIBRATING_BY_BEACONS = "calibrating by beacons"; //TODO
    public static final String SERVER_CALIBRATING_BY_QR = "calibrating by QR"; //TODO
    public static final String SERVER_BLUETOOTH_OFF = "bluetooth off";

    /**Current state of connection server android device with robot. State description from RobotWrap.*/
    public String robotConnectionState;

    /**Current state of connection server android device with client mobile device.*/
    private String clientConnectionState;
    private static final String CLIENT_HASNT_BEEN_CONNECTED = "hasn't been connected";
    private static final String CLIENT_CONNECTED = "connected";
    private static final String CLIENT_NO_CONNECTION = "no connection";
    private static final String CLIENT_GOT_NEW_TARGET = "new target";

    private static final int REQUEST_ENABLE_BT = 0; //>=0 for run onActivityResult from startActivityForResult
    private static final String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";
    private static final String SERVICE_NAME = "RobotServerMGOK"; //name this app for incoming bluetooth requests

    BluetoothAdapter bluetoothAdapter; //локальный БТ адаптер
    private Set<BluetoothDevice> pairedDevices; //спаренные девайсы
    private BluetoothDevice clientDevice; //девайс клиента (для восстановления связи при потере сокета)
    private BluetoothSocket clientSocket; //канал соединения с последним клиентом
    private ReadIncomingMessage readIncomingMessage;
    public Measurement measurement;


    ru.rbot.android.bridge.service.robotcontroll.robots.Robot robot;
    public RobotWrap robotWrap;
    private MainActivity link = this;
    private TaskHandler taskHandler;
    public TTSManager TTS;

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
    ArrayList<String> average = new ArrayList<>();
 //   ArrayAdapter<String> adapter;
 //   ArrayAdapter<String> adapter2;

    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 500;
    private boolean isScanning = false;

    //Данные выборки для сглаживания значений мощности сигнала и расстояния
    int P = 0;
    int f = 0; // флажок

    //Доступные маячки
    ArrayList<String> MAC = new ArrayList<>();
    ArrayList<Integer> power = new ArrayList<>();

    ArrayList<Integer> averpower = new ArrayList<>();
    //TextView status;
    //мау-адреса
    String mac[] = {
            "F4:B8:5E:DE:BA:55",
            "F4:B8:5E:DE:CA:B4",
            "F4:B8:5E:DE:CD:F5",
            "F4:B8:5E:DE:9D:0D",
            "F4:B8:5E:DE:CD:DD",
            "F4:B8:5E:DE:D5:E7"};
    //"F4:B8:5E:DE:C2:8E", без этикетки
    //"F4:B8:5E:DE:BD:1C", без этикетки
    //"F4:B8:5E:DD:EB:77", не робит
    //"F4:B8:5E:DE:D5:B5", без этикетки
    //coords

    String data;
    String fuckingDataFromFile;
    String parsedData[][][][] = new String[13][18][mac.length][4];
    int X = 0;
    int Y = 0;
    int measure_counter;

//////***BLE***(end)

///////////VARIABLES///////////(end)






    private void initConstructor() {
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

        Button buttonSetStart = (Button) findViewById(R.id.buttonSetStart);
        buttonSetStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                robotWrap.setStartCoordinatesByServerEditText();
            }
        });


        robotWrap = new RobotWrap(this);
        taskHandler = new TaskHandler(link);
        TTS = new TTSManager();
        TTS.init(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void bluetoothConstructor() {
        Log.i(TAG, "bluetoothConstructor");
        scanHandler = new Handler();
        scanHandler.post(scanRunnable);
        measurement = new Measurement();

        registerReceiver(incomingPairRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)); //pair request listener (disposable)
        pairedDevices = bluetoothAdapter.getBondedDevices(); //получаем список сопряженных устройств
        AcceptIncomingConnection acceptIncomingConnection = new AcceptIncomingConnection();
        acceptIncomingConnection.start(); //запускаем серверную прослушку входящих БТ запросов (disposable)
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
        sendMessage("ready", robotWrap.currentCellX, robotWrap.currentCellY, robotWrap.currentDirection);
        setClientConnectionState(CLIENT_CONNECTED);
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
                        setClientConnectionState(CLIENT_GOT_NEW_TARGET);
                    }
                });
            }
            if (key.indexOf("mapping") != -1) {
                BluetoothCommands bluetoothCommands = new BluetoothCommands(link);
                bluetoothCommands.runFromBluetoothCommands(key);
            }
            if (key.equals("stop")) {
                Log.i(TAG, "key == stop");
                stopRiding();
                setServerState(SERVER_WAITING_NEW_TASK);
                setClientConnectionState(CLIENT_NO_CONNECTION);
            }
            if (!key.equals("stop")) {
                readIncomingMessage = new ReadIncomingMessage();
                readIncomingMessage.start();
            }
        } catch(IndexOutOfBoundsException e) {
            Log.i(TAG,"IndexOutOfBoundsException");
            readIncomingMessage = new ReadIncomingMessage();
            readIncomingMessage.start();
        }
    }

    private void finishedWorkWithBtClient() {
        sendMessage("target", 0, 0, 0);
        setClientConnectionState(CLIENT_NO_CONNECTION);
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

                int i;

                String macBuf = device.toString();
                f = 0;
                if (MAC.size() != 0) {
                    for (i = 0; i < MAC.size(); i++) {
                        if (macBuf.equals(MAC.get(i))) {
                            P = Integer.parseInt(Byte.toString(txPower)) - rssi;
                            power.set(i, P);
                            f = 1;
                        }
                    }

                    if (f == 0) {
                        if (include(macBuf) == 1){
                            MAC.add(macBuf);
                            P = Integer.parseInt(Byte.toString(txPower)) - rssi;
                            power.add(P);
                        }
                    }
                }
                else{
                    if (include(macBuf) == 1){
                        MAC.add(macBuf);
                        P = Integer.parseInt(Byte.toString(txPower)) - rssi;
                        power.add(P);
                    }
                }

                //Запись в beacons
                if (MAC.size() != 0) {
                    beacons.clear();
                    for (i = 0; i < MAC.size(); i++) {
                        beacons.add("MAC: " + MAC.get(i) + "      power: " + power.get(i).toString());
                    }
                }
                //adapter.notifyDataSetChanged();
            }
        }
    };

    public class Measurement extends Thread {
        @Override
        public void run(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //status = (TextView) findViewById(R.id.textView2);
                    Log.i("TAG", "run");
                    //первичное заполнение
                    for (int j = 0; j < MAC.size(); j++) {
                        averpower.add(0);
                    }
                    //усреднение
                    int i;
                    for (i = 0; i < 10; i++) {
                        for (int j = 0; j < MAC.size(); j++) {
                            averpower.set(j, averpower.get(j) + power.get(j));
                        }
                        try {
                            Measurement.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    for (int j = 0; j < MAC.size(); j++) {
                        averpower.set(j, (averpower.get(j) / i));
                    }
                    //запись в average
                    average.clear();
                    for (i = 0; i < MAC.size(); i++) {
                        average.add("MAC: " + MAC.get(i) + "      average_power: " + averpower.get(i).toString());
                    }
             //       adapter2.notifyDataSetChanged();

                    //WRITE FILE
                    //FILE
                    File fileName = null;
                    FileOutputStream os = null;
                    if (isExternalStorageWritable()) {
                        File sdDir = android.os.Environment.getExternalStorageDirectory();
                        File dir = new File(sdDir.getAbsolutePath() + "/Coords/");
                        dir.mkdir();
                        fileName = new File(dir, "example.txt");
                        try {
                            os = new FileOutputStream(fileName, true);
                            data = "";
                            X = robotWrap.currentCellX;
                            Y = robotWrap.currentCellY;
                        //    int dir = robotWrap.currentDirection;
                            if (measure_counter == 0) data +="Coords," + X + "," + Y + "\n";
                            for (i = 0; i < MAC.size(); i++) {
                                data += MAC.get(i) + "," + averpower.get(i).toString() + ",";
                            }
                            data += "\n";
                            measure_counter = measure_counter + 1;
                            if (measure_counter == 4) measure_counter = 0;
                            os.write(data.getBytes());
                            os.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("TAG", "Write_exception");
                        }
                    } else {
                        Log.i("TAG", "SD_not_available");
                    }
                    //FILE
                    //status.setText("measurment has been ended.. code: " + (measure_counter));
                    Log.i("TAG", "ui_end");
                }
            });
        }
    };

    int include(String macBuf) {
        int f = 0;
        for (int i = 0; i < mac.length; i++){
            if (macBuf.equals(mac[i])) f = 1;
        }
        return f;
    }

    public void startMeasure(){
        //status.setText("is measuring.. ");
        Measurement measurement = new Measurement();
        measurement.start();
    }

    public void clearFile(){
        measure_counter = 0;
        File fileName = null;
        FileOutputStream os = null;
        if (isExternalStorageWritable()) {
            File sdDir = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(sdDir.getAbsolutePath() + "/Coords/");
            dir.mkdir();
            fileName = new File(dir, "example.txt");
            try {
                os = new FileOutputStream(fileName);
                data = "";
                os.write(data.getBytes());
                os.close();
                //status.setText("datafile has been cleared..");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("TAG", "Write_exception");
            }
        } else {
            Log.i("TAG", "SD_not_available");
        }
    }

    /*private void showAverage() {
        adapter2 = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, average);
        ListView listView2 = (ListView)findViewById(R.id.listView2);
        listView2.setAdapter(adapter2);
    }*/
    /*private void showBeacons() {

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, beacons);
        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }*/

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
//////***BLE***(end)
}
