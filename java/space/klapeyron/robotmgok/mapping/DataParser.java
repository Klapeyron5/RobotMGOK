package space.klapeyron.robotmgok.mapping;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import space.klapeyron.robotmgok.MainActivity;

/**
 * Created by Krasavchik Vova on 25.04.2016.
 */
public class DataParser {

    int parsedData[][][][] = new int[13][18][4][6]; //[X][Y][dir][mac] <-> [power]

    public String mac[] = {
            "F4:B8:5E:DE:BA:55",
            "F4:B8:5E:DE:CA:B4",
            "F4:B8:5E:DE:CD:F5",
            "F4:B8:5E:DE:9D:0D",
            "F4:B8:5E:DE:CD:DD",
            "F4:B8:5E:DE:D5:E7"};

    public int[] coordinates(int[] power, String[] MAC){
        int coords[] = {0,0,0};
        int D = 10;
        for(int i = 0; i < parsedData.length; i++){
            for(int j = 0; j < parsedData[0].length; j++){
                for(int k = 0; k < parsedData[0][0].length; k++){
                    int f = 1;
                    for(int m = 0; m < mac.length; m++){
                        if(power[m] > (parsedData[i][j][k][m] + D)||(power[m] < (parsedData[i][j][k][m] - D)))
                        {
                            f = 0;
                        }
                    }
                    if (f == 1) {
                        coords[0] = i;
                        coords[1] = j;
                        coords[2] = k;
                    }
                }
            }
        }
        return coords;
    }

    public void parse(){
        String strings[] = FileToString().split("\n");

        for(int i = 0; i < strings.length; i++){
            if (strings[i].split(",")[0] == "coords"){
                for(int j = 1; j < 5; j++) {
                    for(int k = 0; k < mac.length; k++) {
                        parsedData[Integer.parseInt(strings[i].split(",")[1])][Integer.parseInt(strings[i].split(",")[2])][Integer.parseInt(strings[i + j].split(",")[0])][indexOfArray(strings[i + j].split(",")[2*k+1])] = Integer.parseInt(strings[i + j].split(",")[2*k+2]);
                    }
                }
            }
        }
        Log.i("TAG","parse()");
    }

    public String FileToString(){
        File fileName = null;
        StringBuilder text = new StringBuilder();

        if (isExternalStorageReadable()) {
            File sdDir = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(sdDir.getAbsolutePath() + "/Coords/");
            dir.mkdir();
            fileName = new File(dir, "example.txt");

            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();
            }
            catch (IOException e) {
                Log.i("TAG", "File_not_readable");
            }
        }
        else {
            Log.i("TAG", "SD_not_available");
        }
        return text.toString();
    }

    public int indexOfArray(String str){
        int i;
        for (i = 0; i < mac.length; i++){
            if (mac[i].equals(str)) break;
        }
        return i;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
