package space.klapeyron.robotmgok;

import android.text.style.AbsoluteSizeSpan;
import android.util.Log;

import java.util.ArrayList;

public class Navigation {
    public Navigation() {}
    Navigation(TaskHandler t) {
        taskHandler = t;
    }

    int[][] landscape = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,1,1,1,1,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,0,0,0,0,0,0,0,1,1,1},
            {1,1,1,0,0,0,0,0,0,0,1,1,1},
            {1,1,1,0,0,0,0,0,0,0,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1}};  //(y,x)
    // X to right; Y to down
    //   ========>
    //  ||       X
    //  ||
    //  ||
    //  \/ Y
    int m = landscape.length; //длина карты
    int n = landscape[0].length; // высота карты

    private int[] start = {5,3}; // Начальная точка (координаты вводим наоборот из-за косяка с индексами)
    public int[] finish = {16,9}; // Конечная точка
    int[] tick = new int[]{start[0],start[1]}; // Текущая точка (изначально равна начальной)
    int[][] neighbour = new int[4][3]; // Матрица координат (первые две строки) и значений эвристической функции (третья строка) четырех точек-соседей (столбец соответсвует номеру)
    int F; // Эвристическая функция
    int f;
    int i,j,k;

    int[][] consideredPoints = new int[m*n][4]; // Массив рассмотренных точек (максимально их количество может быть равно m*n)

    TaskHandler taskHandler;
    ArrayList<Integer> pathCommandsForRobot = new ArrayList<Integer>();
    public ArrayList<Integer> absolutePath;

    public ArrayList<Integer> getPathCommandsForRobot()
    {
        buildPath();
        int Path[][] = new int[k+1][2];
        for(i=0;i<k;i++)
        {
            Path[i][0] = consideredPoints[i][2];
            Path[i][1] = consideredPoints[i][3];
        }
        Path[k][0] = consideredPoints[k-1][0];
        Path[k][1] = consideredPoints[k-1][1];

        int Direction[] = new int[k];
        for(i=0;i<k;i++)
        {
            if ((Path[i+1][0]-Path[i][0])== 1)
            {
                Direction[i] = 1; // из-за косяка с индексами произведен сдвиг на 1 (Path[i][j], j=j+1)
            }
            else if ((Path[i+1][1]-Path[i][1])== 1)
            {
                Direction[i] = 0;
            }
            else if ((Path[i+1][0]-Path[i][0])== -1)
            {
                Direction[i] = 3;
            }
            else if ((Path[i+1][1]-Path[i][1])== -1)
            {
                Direction[i] = 2;
            }
        }
        absolutePath = new ArrayList<Integer>();
        for(i=0;i<k;i++) {
            Log.i(MainActivity.TAG, Direction[i] + "");
            absolutePath.add(Direction[i]);
        }

        //TODO //correct start odometryAngle
        pathCommandsForRobot.clear();
        if (taskHandler.robotWrap.currentDirection != Direction[0]) {
            int currDir = taskHandler.robotWrap.currentDirection;
            if     (((currDir == 0) && (Direction[0] == 1)) ||
                    ((currDir == 1) && (Direction[0] == 2)) ||
                    ((currDir == 2) && (Direction[0] == 3)) ||
                    ((currDir == 3) && (Direction[0] == 0))) {
                pathCommandsForRobot.add(0);
                pathCommandsForRobot.add(1);
            } else
                if (((currDir == 0) && (Direction[0] == 3)) ||
                    ((currDir == 3) && (Direction[0] == 2)) ||
                    ((currDir == 2) && (Direction[0] == 1)) ||
                    ((currDir == 1) && (Direction[0] == 0))) {
                pathCommandsForRobot.add(2);
                pathCommandsForRobot.add(1);
            } else
                if (Math.abs(currDir - Direction[0]) == 2) {
                    Log.i(MainActivity.TAG,"PI turn");
                    pathCommandsForRobot.add(2); //TODO //not work
                    pathCommandsForRobot.add(2);
                    pathCommandsForRobot.add(1);
            }
        } else {
            pathCommandsForRobot.add(1);
        }

        for(int i=1;i<Direction.length;i++) {
            if(Direction[i] == Direction[i-1])
                pathCommandsForRobot.add(1);
            else {
                if     (((Direction[i - 1] == 0) && (Direction[i] == 1)) ||
                        ((Direction[i - 1] == 1) && (Direction[i] == 2)) ||
                        ((Direction[i - 1] == 2) && (Direction[i] == 3)) ||
                        ((Direction[i - 1] == 3) && (Direction[i] == 0))) {
                    pathCommandsForRobot.add(0);
                    pathCommandsForRobot.add(1);
                }
                else
                if     (((Direction[i-1]==0)&&(Direction[i]==3))||
                        ((Direction[i-1]==3)&&(Direction[i]==2))||
                        ((Direction[i-1]==2)&&(Direction[i]==1))||
                        ((Direction[i-1]==1)&&(Direction[i]==0))) {
                    pathCommandsForRobot.add(2);
                    pathCommandsForRobot.add(1);
                }
            }
        }
        return pathCommandsForRobot;
    }

    public void buildPath()
    {
        tick = new int[]{start[0], start[1]};
        k = 0;
        //for (k=0;k<4;k++)
        while(true)
        {
            getPoint();
            consideredPoints[k][2] = tick[0];
            consideredPoints[k][3] = tick[1];
            tick[0] = neighbour[j][0];
            tick[1] = neighbour[j][1];
            consideredPoints[k][0] = tick[0];
            consideredPoints[k][1] = tick[1];
            k++;
            if (tick[0] == finish[0])
            {
                if (tick[1] == finish[1])
                {
                    break;
                }
            }
            if (k > m*n-1)
            {
                System.out.println("k="+k+" Зациклено");
                break;
            }
        }
    }

    public void getPoint() //Нахождение оптимальной точки продолжения пути из текущей
    {
        for (i=0;i<4;i++)
        {

            neighbour[i][2] = getFunction(); // Для каждого "соседа" узнаем функцию F
            /*System.out.println("Function(getPoint):");
            System.out.println(neighbour[i][2]);*/

        }

        f = neighbour[0][2]; //здесь и далее находим минимальное значение f функции F
        //System.out.println("f*="+f);
        for(i=1;i<4;i++)
        {
            if (neighbour[i][2]<f)
            {
                f = neighbour[i][2]; // Номер соседа с наименьшим значением функции.
                j = i;
                //System.out.println("f="+f);
            }
            else j = 0;
        }
        for(i=3;i>j;i--)
        {
            if (neighbour[i][2]==f) // Если находим двух соседей с наименьшим значением функции, то выбираем исходя из того, у кого номер больше.
            {
                if(i>j)
                {
                    f = neighbour[i][2]; // Таким образом, сосед под номером j имеет наименьшее значение f функции F.
                    j = i;
                    //System.out.println("f="+f);
                }
            }
        }
        /*System.out.println("Minimum(F):");
        System.out.println("j="+j);
        System.out.println(neighbour[j][2]+"("+neighbour[j][0]+";"+neighbour[j][1]+")");*/
    }

    public int getFunction() //Нахождение эвристической функции оптимального продолжения маршрута
    {
        getNeighbours();
        F = Math.abs(neighbour[i][0]-finish[0]) + Math.abs(neighbour[i][1] - finish[1]) + (m+n+1)*(landscape[neighbour[i][0]][neighbour[i][1]]);
        return F;

        /*System.out.println("Functions:");
        for (i=0;i<4;i++)
        {
        F = Math.abs(neighbour[i][0]-finish[0]) + Math.abs(neighbour[i][1] - finish[1]) + (m+n+1)*(landscape[neighbour[i][0]][neighbour[i][1]]);
        System.out.println(F);
        }*/
    }

    public void getNeighbours() //Нахождение соседей текущей точки
    {
        neighbour[0][0] = tick[0]+1; neighbour[0][1] = tick[1];
        neighbour[1][0] = tick[0]; neighbour[1][1] = tick[1]+1;
        neighbour[2][0] = tick[0]-1; neighbour[2][1] = tick[1];
        neighbour[3][0] = tick[0]; neighbour[3][1] = tick[1]-1;

        /*System.out.println("Neighbours:");
        for (i=0;i<4;i++)
        System.out.println(i+"("+neighbour[i][0]+";"+neighbour[i][1]+")");*/
    }


    public void setStart(int Y,int X) {
        start = new int[] {Y,X};
    }

    public void setFinish(int Y,int X) {
        finish = new int[] {Y,X};
    }

    public int[] getStart() {
        return start;
    }
}
