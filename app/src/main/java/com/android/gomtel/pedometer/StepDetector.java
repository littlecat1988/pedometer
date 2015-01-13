/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.gomtel.pedometer;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 * @author Levente Bagi
 * @todo REFACTOR: SensorListener is deprecated
 */
@TargetApi(Build.VERSION_CODES.CUPCAKE)
@SuppressWarnings("deprecation")
public class StepDetector implements SensorListener
{
    private static final String TAG = "Pedometer";
    private int stepCounts1 = 0;
    private int stepCounts2 = 0;
    private int     mLimit = 30;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;
    private static long end = 0;
    private static long start = 0;
    //    public int forBegin = 1;
    FileOutputStream fos = null;
    FileOutputStream fos1 = null;
    FileOutputStream fos2 = null;
    FileOutputStream fos3 = null;
    long timetamp = System.currentTimeMillis();
    String fileName = "axis-" + "-" + timetamp + ".txt";
    String fileName1 = "convoleBegin-" + "-" + timetamp + ".txt";
    String fileName2 = "convole-" + "-" + timetamp + ".txt";
    String fileName3 = "peak_pre-" + "-" + timetamp + ".txt";
    public int buffer_count = -1;
    public int sample_count = 0;
    public int accdata_count = 1000;
    public int start_end_flag = 0;
    public int N = 15;
    public int end_flag;
    public int forBegin = 1;

    public float NN = 1 / (N - 1);
    public double Pn = 0.0001;


    public int first_flag = 0;
    public int acc_pos_flag = 1;
    public int acc_pos_x_mean = 0;
    public int acc_pos_pre = 1;
    public int acc_pos_flag_axis = 1;
    public int step_count = 0;
    public int step_index_temp = 2 * N;
    public int peak_flag = 1;
    public double peak_pre = 0;
    public int step_pre_index = 5;

    public int peak_valley_flag = 0;
    public int peak_pre_index_temp = 5;
    public float peak_pre_temp = 0;
    public int peak_pre_index = 5;

    public int valley_pre_index = 5;
    public double valley_pre = 0;
    public int step_count_pre = 0;
    public int step_pre = 0;
    public int step_output;

    public int peak_valley_index_interval = 34;
    public int peak_valley_index_interval_2 = 20;



    public int start_buffer_count = 0;

    private float[] mean_acc_ini_pos = new float[3];
    private float[] mean_acc_ini_pos_abs = new float[3];
    private float[] var_acc_ini_pos_abs = new float[2];
    public float acc_pos_xy_temp = 0;
    private int axis = 0;
    private double[] Num =
            { 0.00148677636193634, 0.00341083259879214, 0.00683339294739916, 0.0119027184654288, 0.0187472007260031, 0.0272773808885607, 0.0371444092497878, 0.0477466271669650, 0.0582716496249448, 0.0677967148270777, 0.0754049362379048, 0.0803182489401661, 0.0820176529416717, 0.0803182489401661, 0.0754049362379048, 0.0677967148270777, 0.0582716496249448, 0.0477466271669650, 0.0371444092497878, 0.0272773808885607, 0.0187472007260031, 0.0119027184654288, 0.00683339294739916, 0.00341083259879214, 0.00148677636193634 };

    public float[][] accdata_buffer = new float[1000][3];
    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();
    private int old_step_count = 0;
    private int step_count_z = 0;
    private float data_max = 0;
    private boolean isUp;

    public StepDetector() {
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        try
        {
            File dir = new File("/mnt/sdcard/log");
            if (!dir.exists())
                dir.mkdir();
            fos = new FileOutputStream(dir + "/" + fileName);
            fos1 = new FileOutputStream(dir + "/" + fileName1);
            fos2 = new FileOutputStream(dir + "/" + fileName2);
            fos3 = new FileOutputStream(dir + "/" + fileName3);
        } catch (Exception e)
        {
            // TODO: handle exception
        }
    }

    public void setSensitivity(int sensitivity) {
        mLimit = sensitivity;
    }

    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }

    @Override
    public void onSensorChanged(int sensor, float[] values) {
        // TODO Auto-generated method stub
        try
        {
            Log.e(TAG,"lixiang---values[0]= "+values[0]+"  values[1]= "+values[1]+"   values[2]= "+values[2]+"  sensor= "+sensor);
            if (sensor == SensorManager.SENSOR_ACCELEROMETER && forBegin == 1)
            {


                String b =values[0] + "\t\n" + values[1] + "\t\n" + values[2] + "\r\n";
//                try
//                {
////                    fos.write(b.getBytes());
//
//                } catch (IOException e1)
//                {
//                    // TODO Auto-generated catch block
//                    e1.printStackTrace();
//
//                }
                buffer_count = buffer_count + 1;

                if (buffer_count < 1000)
                {
                    accdata_buffer[buffer_count][0] = values[0];
                    accdata_buffer[buffer_count][1] = values[1];
//					accdata_buffer[buffer_count][2] = event.values[2];

                    if (buffer_count == 29)
                    {
//						mean_acc_ini_pos[2] = this.mean(accdata_buffer, 2);
                        mean_acc_ini_pos[1] = this.mean(accdata_buffer, 1);
                        mean_acc_ini_pos[0] = this.mean(accdata_buffer, 0);
                        acc_pos_xy_temp=mean_acc_ini_pos[1]*mean_acc_ini_pos[1]+mean_acc_ini_pos[0]*mean_acc_ini_pos[0];
                        if (acc_pos_xy_temp<100)
                        {
                            mean_acc_ini_pos[2] = (float) Math.sqrt(100-acc_pos_xy_temp);
                        }
                        else
                        {
                            mean_acc_ini_pos[2] =0;
                        }


                        mean_acc_ini_pos_abs[2] = mean_acc_ini_pos[2];
                        mean_acc_ini_pos_abs[1] = Math.abs(this.mean(accdata_buffer, 1));
                        mean_acc_ini_pos_abs[0] = Math.abs(this.mean(accdata_buffer, 0));

                        acc_pos_flag_axis = this.argMax(mean_acc_ini_pos_abs);
                        acc_pos_pre = acc_pos_flag_axis;
                        if (mean_acc_ini_pos[acc_pos_flag_axis] < 0)
                        {
                            acc_pos_flag = -1;

                        }
                    }
                    if (buffer_count % 50 == 0 && buffer_count > 99)
                    {
//						mean_acc_ini_pos[2] = this.mean2(accdata_buffer, buffer_count - 100, 2);
                        mean_acc_ini_pos[1] = this.mean2(accdata_buffer, buffer_count - 100, 1);
                        mean_acc_ini_pos[0] = this.mean2(accdata_buffer, buffer_count - 100, 0);
                        acc_pos_xy_temp=mean_acc_ini_pos[1]*mean_acc_ini_pos[1]+mean_acc_ini_pos[0]*mean_acc_ini_pos[0];
                        if (acc_pos_xy_temp<100)
                        {
                            mean_acc_ini_pos[2] = (float) Math.sqrt(100-acc_pos_xy_temp);
                        }
                        else
                        {
                            mean_acc_ini_pos[2] =0;
                        }
                        mean_acc_ini_pos_abs[2] = mean_acc_ini_pos[2];
                        mean_acc_ini_pos_abs[1] = Math.abs(this.mean2(accdata_buffer, buffer_count - 100, 1));
                        mean_acc_ini_pos_abs[0] = Math.abs(this.mean2(accdata_buffer, buffer_count - 100, 0));

                        acc_pos_flag_axis = this.argMax(mean_acc_ini_pos_abs);
//						textView1.setText("start_buffer_count:" + "\r\n" + start_buffer_count);
                        if (acc_pos_flag_axis==0 || acc_pos_flag_axis==1)
                        {
                            acc_pos_flag_axis=1;
                        }
                        if ((acc_pos_pre != acc_pos_flag_axis && acc_pos_pre==2)|| (acc_pos_pre != acc_pos_flag_axis && acc_pos_flag_axis==2) )
                        {
                            start_buffer_count = 0;
                            acc_pos_pre = acc_pos_flag_axis;
                        }
                        if (mean_acc_ini_pos[acc_pos_flag_axis] > 0)
                        {
                            acc_pos_flag = 1;
                        } else
                            acc_pos_flag = -1;
                        if (acc_pos_flag_axis == 2)
                        {
                            if (mean_acc_ini_pos[0] > 0)
                            {
                                acc_pos_flag = 1;
                            } else
                            {
                                acc_pos_flag = -1;
                            }
                        }

                    } else if (buffer_count > 49 && buffer_count % 20 == 0)
                    {
                        end_flag = this.Var_Threshold_End(accdata_buffer, buffer_count - 25, 0.05);

                        if (end_flag == 1)
                            start_buffer_count = 0;
                    }

                    // 计步
                    if (buffer_count % 10 == 0 && buffer_count > 28)
                    {
                        Log.e(TAG,"lixiang---acc_pos_flag_axis= "+acc_pos_flag_axis);
                        if (acc_pos_flag_axis == 1 || acc_pos_flag_axis == 0)
                        {

                            step_output = this.step_diff_simulate_initial(accdata_buffer, peak_valley_index_interval);
                        } else if (acc_pos_flag_axis == 2)
                        {

                            step_output = this.step_diff_simulate_highfreq_initial(accdata_buffer);

                            // step_output =
                            // this.step_diff_simulate_initial(accdata_buffer,
                            // peak_valley_index_interval);
                        }
                        if (step_count != step_pre)
                        {
                            step_pre = step_count;

                        }
//                        textView.setText("step cout:" + "\r\n" + step_count);
                        for (StepListener stepListener : mStepListeners) {
                            stepListener.onStep(step_count);
                        }
                    }

                } else if (buffer_count > 999)
                {
//					Toast.makeText(MainActivity.this, "buffer_count > 999！", Toast.LENGTH_LONG).show();

                    for (int i = 0; i < 999; i++)
                    {
                        accdata_buffer[i][0] = accdata_buffer[i + 1][0];
                        accdata_buffer[i][1] = accdata_buffer[i + 1][1];
//						accdata_buffer[i][2] = accdata_buffer[i + 1][2];
                    }
                    accdata_buffer[accdata_count - 1][0] = values[0];
                    accdata_buffer[accdata_count - 1][1] = values[1];
//					accdata_buffer[accdata_count - 1][2] = event.values[2];

                    if (buffer_count % 50 == 0)
                    {
//						mean_acc_ini_pos[2] = this.mean3(accdata_buffer, accdata_count - 100, 2);
                        mean_acc_ini_pos[1] = this.mean3(accdata_buffer, accdata_count - 100, 1);
                        mean_acc_ini_pos[0] = this.mean3(accdata_buffer, accdata_count - 100, 0);
                        acc_pos_xy_temp=mean_acc_ini_pos[1]*mean_acc_ini_pos[1]+mean_acc_ini_pos[0]*mean_acc_ini_pos[0];
                        if (acc_pos_xy_temp<100)
                        {
                            mean_acc_ini_pos[2] = (float) Math.sqrt(100-acc_pos_xy_temp);
                        }
                        else
                        {
                            mean_acc_ini_pos[2] =0;
                        }
                        mean_acc_ini_pos_abs[2] = mean_acc_ini_pos[2];
//						mean_acc_ini_pos_abs[2] = Math.abs(this.mean3(accdata_buffer, accdata_count - 100, 2));
                        mean_acc_ini_pos_abs[1] = Math.abs(this.mean3(accdata_buffer, accdata_count - 100, 1));
                        mean_acc_ini_pos_abs[0] = Math.abs(this.mean3(accdata_buffer, accdata_count - 100, 0));

                        acc_pos_flag_axis = this.argMax(mean_acc_ini_pos_abs);
                        if (acc_pos_flag_axis==0 || acc_pos_flag_axis==1)
                        {
                            acc_pos_flag_axis=1;
                        }
                        if ((acc_pos_pre != acc_pos_flag_axis && acc_pos_pre==2)|| (acc_pos_pre != acc_pos_flag_axis && acc_pos_flag_axis==2) )
                        {
                            start_buffer_count = 0;
                            acc_pos_pre = acc_pos_flag_axis;
                        }

                        if (mean_acc_ini_pos[acc_pos_flag_axis] > 0)
                        {
                            acc_pos_flag = 1;
                        } else
                            acc_pos_flag = -1;

                        if (acc_pos_flag_axis == 2)
                        {
                            if (mean_acc_ini_pos[0] > 0)
                            {
                                acc_pos_flag = 1;

                            } else
                            {
                                acc_pos_flag = -1;
                            }
                            if (mean_acc_ini_pos_abs[0] < 1)
                            {
                                acc_pos_x_mean = 0;
                            } else
                            {
                                acc_pos_x_mean = 1;
                            }
                        }

                    }
                    if (buffer_count % 20 == 0)
                    {
                        end_flag = this.Var_Threshold_End(accdata_buffer, accdata_count - 25, 0.05);
                        if (end_flag == 1)
                            start_buffer_count = 0;
                    }

                    // 计步
                    if (buffer_count % 10 == 0)
                    {
                        // int len_buffer = accdata_buffer.length;
                        if (acc_pos_flag_axis == 1 || acc_pos_flag_axis == 0)
                        {
                            step_output = this.step_diff_simulate(accdata_buffer, buffer_count + 1, peak_valley_index_interval);

                        } else if (acc_pos_flag_axis == 2)
                        {
                            data_max = values[2];

                            if(data_max < -10.1){
                                isUp = true;
                            }else if(isUp){
                                if(step_count_z <9) {
                                    step_count_z = step_count_z + 1;
                                }else if(step_count_z == 9){
                                    step_count = step_count+10;
                                    step_count_z = step_count_z + 1;
                                }else {
                                    step_count = step_count + 1;
                                }
                                isUp = false;
                            }
                            step_output = step_diff_simulate_highfreq(accdata_buffer, buffer_count + 1);

//                            step_output =
//                            this.step_diff_simulate(accdata_buffer,
//                            buffer_count + 1, peak_valley_index_interval);
                        }

                        if (step_count != step_pre)
                        {
                            step_pre = step_count;

                        }
//                        textView.setText("step cout:" + "\r\n" + step_count);
                        for (StepListener stepListener : mStepListeners) {
                            stepListener.onStep(step_count);
                        }
                    }

                }
//                textView.setText("step cout:" + "\r\n" + step_count);
                for (StepListener stepListener : mStepListeners) {
                    stepListener.onStep(step_count);
                }

                // 作图

//                String b2 = this.convolve(accdata_buffer, buffer_count, 0) + "\t\n" + this.convolve(accdata_buffer, buffer_count, 1) + "\t\n" + this.convolve(accdata_buffer, buffer_count, 2) + "\r\n";
//                try
//                {
//                    fos2.write(b2.getBytes());
//
//                } catch (IOException e1)
//                {
//                    // TODO Auto-generated catch block
//                    e1.printStackTrace();
//
//                }

            }

        } catch (Exception e)
        {
            // // TODO: handle exception
//			Toast.makeText(MainActivity.this, "wrong！", Toast.LENGTH_LONG).show();
            e.printStackTrace();
//			Log.d("memsic","memsic yong djaksdhflkajsf " + e.getMessage());
        }
//        synchronized (this) {
////            Log.e(TAG,"sensor = "+sensor);
//
//
//
//                int j = (sensor == SensorManager.SENSOR_MAGNETIC_FIELD) ? 1 : 0;
//
//                if (j == 0) {
//
////                    int k = 0;
////                    float v = (float)Math.sqrt((double)(values[0]*values[0]+values[1]*values[1]+values[2]*values[2]));
////                        Log.e(TAG, "lixiang---v= " + v);
////                    Log.e(TAG, "lixiang---= " + System.currentTimeMillis());
//                    Log.e(TAG, "lixiang---values[0]= " + values[0]+"  values[1]= "+values[1]+"  values[2]= "+values[2]);
//                    float vSum = 0;
//                    for (int i = 0; i < 3; i++) {
//                        final float v = mYOffset + values[i] * mScale[j];
//                        vSum += v;
//                    }
//                    int k = 0;
//                    float v = vSum / 3;
//
//                    float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
//
//                    if (direction == - mLastDirections[k]) {
//
//
//                        int extType = (direction > 0 ? 0 : 1);
//                        mLastExtremes[extType][k] = mLastValues[k];
//                        float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);
//                        if (diff > mLimit) {
//                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
//                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
//                            boolean isNotContra = (mLastMatch != 1 - extType);
//
//                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
//                                    start = System.currentTimeMillis();
//                                    if(start-end > 300) {
//                                        for (StepListener stepListener : mStepListeners) {
//                                             stepListener.onStep();
//                                        }
//                                        mLastMatch = extType;
//                                        end = start;
//                                    }
//                            }
//                            else {
//                                mLastMatch = -1;
//                            }
//                        }
//                        mLastDiff[k] = diff;
//                    }
//                    mLastDirections[k] = direction;
//                    mLastValues[k] = v;
//                }
//
//            }
    }


    @Override
    public void onAccuracyChanged(int i, int i2) {

    }
    // 需要用到的数学函数
    private float mean(float f[][], int x)
    {
        // TODO Auto-generated method stub

        float sumNum1 = 0;
        for (int i = 0; i < 30; i++)
        {
            sumNum1 += f[i][x];
        }
        return sumNum1 / 30;
    }

    private float mean2(float f[][], int a, int x)
    {
        // TODO Auto-generated method stub
        float sumNum2 = 0;
        for (int i = a; i < a + 100; i++)
        {
            sumNum2 += f[i][x];
        }
        return sumNum2 / 100;
    }

    private float mean3(float f[][], int a, int x)
    {
        // TODO Auto-generated method stub

        float sumNum2 = 0;
        for (int i = a; i < 1000; i++)
        {
            sumNum2 += f[i][x];
        }
        return sumNum2 / (1000 - a);
    }

    private float mean4(float f[][], int a, int b, int x)
    {
        // TODO Auto-generated method stub

        float sumNum2 = 0;
        for (int i = a; i < b; i++)
        {
            sumNum2 += f[i][x];
        }
        return sumNum2 / (b - a);
    }

    private int argMax(float f[])
    {
        // TODO Auto-generated method stub

        float sumNum1 = 0;

        int a;
        sumNum1 = Math.max(f[0], f[1]);
        float sumNum2 = Math.max(sumNum1, f[2]);
        if (f[0] == sumNum2)
        {
            a = 0;
        } else if (f[1] == sumNum2)
        {
            a = 1;
        } else
        {
            a = 2;
        }
        return a;

    }





    private float var_axis(float accdata_end[][], int start, int end,int i)
    {
        float varNum = 0;
        float sumNum = 0;
        float sumEvnNum = 0;
        float eveNum = 0;
        int len=end-start;
        for (int j = start; j < start + len; j++)
        {
            sumNum += accdata_end[j][i];
        }

        eveNum = sumNum / len;
        for (int j = start; j < start + len; j++)
        {
            sumEvnNum += (accdata_end[j][i] - eveNum) * (accdata_end[j][i] - eveNum);
        }
        varNum = sumEvnNum / len;
        return varNum;
    }
    private float var(float accdata_end[][], int b, int i)
    {
        float varNum = 0;
        float sumNum = 0;
        float sumEvnNum = 0;
        float eveNum = 0;
        // int len_buff = accdata_end.length;
        if (b < 975)
        {
            for (int j = b; j < b + 25; j++)
            {
                sumNum += accdata_end[j][i];
            }

            eveNum = sumNum / 25;
            for (int j = b; j < b + 25; j++)
            {
                sumEvnNum += (accdata_end[j][i] - eveNum) * (accdata_end[j][i] - eveNum);
            }
            varNum = sumEvnNum / 25;
        } else
        {
            for (int j = b; j < 1000; j++)
            {
                sumNum += accdata_end[j][i];
            }

            eveNum = sumNum / (1000 - b);
            for (int j = b; j < 1000; j++)
            {
                sumEvnNum += (accdata_end[j][i] - eveNum) * (accdata_end[j][i] - eveNum);
            }
            varNum = sumEvnNum / (1000 - b);
        }
        return varNum;

    }

    // 卷积函数
    private float convolve(float accdata_temp[][], int x, int y)
    {
        float accdata_convolve;
        int len_num = Num.length;

        float temp = 0;
        if (x < len_num)
        {
            for (int j = 0; j < x + 1; j++)
            {
                temp += accdata_temp[x - j][y] * Num[j];
            }
            accdata_convolve = temp;
        } else
        {

            for (int j = 0; j < len_num; j++)
            {
                temp += accdata_temp[x - j][y] * Num[j];
            }
            accdata_convolve = temp;
        }

        return accdata_convolve;

    }

    // 微分函数/////////////////////////////////////////////
    private float diff(float accdata[][], int x, int y)
    {
        float diffNum;

        diffNum = accdata[x + 1][y] - accdata[x][y];

        return diffNum;

    }

    // copy已定函数
    private int Var_Threshold_End(float accdata_end[][], int b, double d)
    {
        float temp = 0;
        int flag = 0;
        for (int i = 0; i < 2; i++)
        {
            temp += this.var(accdata_end, b, i);
        }
        if (temp < d)
        {
            flag = 1;
        }
        return flag;

    }

    private int step_diff_simulate(float accdata[][], int len_buffer, int peak_valley_index_interval) throws IOException
    {
        // textView6.setText("len_buffer:" + step_index_temp);
        int len_acc = 1000;

        float[][] acc3_data_nodiff_temp = new float[len_acc][3];
        float[][] acc3_data_nodiff = new float[len_acc][3];
        float[][] accdata_temp = new float[len_acc][3];

        if (len_buffer%50==0)
        {
            var_acc_ini_pos_abs[1]=this.var_axis(accdata, len_acc-100, len_acc-1, 1);
            var_acc_ini_pos_abs[0]=this.var_axis(accdata, len_acc-100, len_acc-1, 0);
            axis=this.argMax(var_acc_ini_pos_abs);
//			textView1.setText("axis:" + "\r\n" + axis);
        }
        if (acc_pos_flag == -1)
        {
            for (int i = 0; i < 1000; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = -accdata[i][j];
                }
            }
        }

        else
        {
            for (int i = 0; i < 1000; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = accdata[i][j];

                }
            }
        }

        for (int j = 0; j < 2; j++)
        {
            for (int i = 0; i < acc3_data_nodiff_temp.length; i++)
            {
                acc3_data_nodiff_temp[i][j] = this.convolve(accdata_temp, i, j);

            }
        }
        for (int i = 0; i < len_acc - 1; i++)
        {
//			acc3_data_nodiff[i][2] = acc3_data_nodiff_temp[i][2];
            acc3_data_nodiff[i][1] = acc3_data_nodiff_temp[i][1];
            acc3_data_nodiff[i][0] = acc3_data_nodiff_temp[i][0];
        }
        if (len_buffer - valley_pre_index > 999)
        {
            valley_pre_index = len_buffer - 10;
            step_index_temp = len_buffer - 5;
        }

        for (int i = step_index_temp; i < len_buffer - 2; i++)
        {

            if (peak_valley_flag == 0)
            {
                for (int j_peak = valley_pre_index; j_peak < i; j_peak++)
                {

                    if ((acc3_data_nodiff[j_peak + 1000 - len_buffer][axis] > acc3_data_nodiff[j_peak + 1000 - len_buffer - 1][axis]) && (acc3_data_nodiff[j_peak + 1000 - len_buffer][axis] > acc3_data_nodiff[j_peak + 1000 - len_buffer + 1][axis]))
                    {
                        peak_valley_flag = 1;
                        peak_pre_index_temp = j_peak;
                        peak_pre_temp = acc3_data_nodiff[j_peak + 1000 - len_buffer][axis];
                    }
                }

            } else if (peak_valley_flag == 1)
            {
                for (int j_valley = peak_pre_index_temp; j_valley < i; j_valley++)
                {

                    if ((acc3_data_nodiff[j_valley + 1000 - len_buffer][axis] < acc3_data_nodiff[j_valley + 1000 - len_buffer - 1][axis]) && (acc3_data_nodiff[j_valley + 1000 - len_buffer][axis] < acc3_data_nodiff[j_valley + 1000 - len_buffer + 1][axis]))
                    {
                        float peak_to_valley = peak_pre_temp - acc3_data_nodiff[j_valley + 1000 - len_buffer][axis];
                        if (peak_pre_index_temp - valley_pre_index > 50)
                            valley_pre = this.mean4(acc3_data_nodiff, j_valley + 1000 - len_buffer - 100, j_valley + 1000 - len_buffer, axis) - 0.2;
                        else if (peak_pre_index_temp - valley_pre_index > 20 && peak_to_valley < 0 && start_buffer_count > 0)

                        {
                            valley_pre = this.mean4(acc3_data_nodiff, j_valley + 1000 - len_buffer - peak_pre_index_temp + valley_pre_index, j_valley + 1000 - len_buffer, axis) - 1;
                        }
                        peak_valley_flag = 0;
                        int peak_index_diff = peak_pre_index_temp - peak_pre_index;
                        int valley_index_diff = j_valley - valley_pre_index;
                        int peak_valley_index_diff=peak_pre_index_temp-valley_pre_index;
                        if (peak_pre_temp - acc3_data_nodiff[j_valley + 1000 - len_buffer][axis] > 0.8 && peak_pre_temp - valley_pre > 0.3 && valley_index_diff > peak_valley_index_interval && peak_index_diff > peak_valley_index_interval)
                        {
                            peak_pre_index = peak_pre_index_temp;


                            peak_pre = peak_pre_temp;
                            if (start_buffer_count < 8)
                            {
                                start_buffer_count = start_buffer_count + 2;
                            } else if (start_buffer_count == 8)
                            {
                                step_count = step_count + 10;
                                start_buffer_count = start_buffer_count + 2;
                                Log.e(TAG,"lixiang---start_buffer_count001= "+start_buffer_count);

                            } else
                            {
                                if (peak_pre_index - valley_pre_index > 100)
                                {
                                    Log.e(TAG,"lixiang---step_count002= "+start_buffer_count);
                                    step_count = step_count + 1;
                                } else
                                {
                                    step_count = step_count + 2;
                                }
                            }
                            String b3 = peak_pre + "\t\n" + peak_pre_index + "\t\n" + valley_pre + "\t\n" + valley_pre_index + "\r\n";
//                            try
//                            {
//                                fos3.write(b3.getBytes());
//
//                            } catch (IOException e1)
//                            {
//                                // TODO Auto-generated catch block
//                                e1.printStackTrace();
//
//                            }
                            valley_pre_index = j_valley;
                            valley_pre = acc3_data_nodiff[j_valley + 1000 - len_buffer][axis];
                        }
                        step_pre_index = i;
                        step_index_temp = i + 5;
//                        textView.setText("step cout:" + "\r\n" + step_count);
//                        for (StepListener stepListener : mStepListeners) {
//                            stepListener.onStep(step_count);
//                        }
                        return step_count;

                    }

                }

            }

        }
//        textView.setText("step cout:" + "\r\n" + step_count);
        for (StepListener stepListener : mStepListeners) {
            stepListener.onStep(step_count);
        }
        return step_count;
    }

    private int step_diff_simulate_initial(float accdata[][], int peak_valley_index_interval) throws IOException
    {
        int len_acc = buffer_count + 1;


        if (len_acc<99)
        {
            axis = acc_pos_flag_axis;
        }else {
            if (len_acc%50==0)
            {
                var_acc_ini_pos_abs[1]=this.var_axis(accdata, buffer_count-99, buffer_count, 1);
                var_acc_ini_pos_abs[0]=this.var_axis(accdata, buffer_count-99, buffer_count, 0);
                axis=this.argMax(var_acc_ini_pos_abs);
//				textView1.setText("axis:" + "\r\n" + axis);
            }
        }
        float temp;
        float[][] acc3_data_nodiff_temp = new float[len_acc][3];
        float[][] acc3_data_nodiff = new float[len_acc][3];
        float[][] accdata_temp = new float[len_acc][3];

        if (acc_pos_flag == -1)
        {
            for (int i = 0; i < buffer_count; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = -accdata[i][j];
                }
            }
        } else
        {
            for (int i = 0; i < buffer_count; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = accdata[i][j];

                }
            }

        }

        for (int j = 0; j < 2; j++)
        {
            for (int i = 0; i < buffer_count; i++)
            {
                acc3_data_nodiff_temp[i][j] = this.convolve(accdata_temp, i, j);

            }
        }
        for (int i = 0; i < len_acc - 1; i++)
        {
//			acc3_data_nodiff[i][2] = acc3_data_nodiff_temp[i][2];
            acc3_data_nodiff[i][1] = acc3_data_nodiff_temp[i][1];
            acc3_data_nodiff[i][0] = acc3_data_nodiff_temp[i][0];
        }

        for (int i = step_index_temp; i < len_acc - 2; i++)
        {
            if (peak_valley_flag == 0)
            {
                for (int j_peak = valley_pre_index; j_peak < i; j_peak++)
                {
                    if ((acc3_data_nodiff[j_peak][axis] > acc3_data_nodiff[j_peak - 1][axis]) && (acc3_data_nodiff[j_peak][axis] > acc3_data_nodiff[j_peak + 1][axis]))
                    {
                        peak_valley_flag = 1;
                        peak_pre_index_temp = j_peak;
                        peak_pre_temp = acc3_data_nodiff[j_peak][axis];
                    }
                }
            } else if (peak_valley_flag == 1)
            {
                for (int j_valley = peak_pre_index_temp; j_valley < i; j_valley++)
                {
                    if ((acc3_data_nodiff[j_valley][axis] < acc3_data_nodiff[j_valley - 1][axis]) && (acc3_data_nodiff[j_valley][axis] < acc3_data_nodiff[j_valley + 1][axis]))
                    {
                        float peak_to_valley = peak_pre_temp - acc3_data_nodiff[j_valley][axis];
                        if (peak_pre_index_temp - valley_pre_index > 50 && len_acc > 100)
                            valley_pre = this.mean4(acc3_data_nodiff, len_acc - 100, len_acc, axis) - 0.2;
                        else if (peak_pre_index_temp - valley_pre_index > 20 && peak_to_valley < 0 && start_buffer_count > 0)

                        {
                            valley_pre = this.mean4(acc3_data_nodiff, len_acc - peak_pre_index_temp + valley_pre_index, len_acc, axis) - 1;
                        }
                        peak_valley_flag = 0;
                        int peak_index_diff = peak_pre_index_temp - peak_pre_index;
                        int valley_index_diff = j_valley - valley_pre_index;
                        int peak_valley_index_diff=peak_pre_index_temp-valley_pre_index;
                        if (peak_pre_temp - acc3_data_nodiff[j_valley][axis] > 0.8 && peak_pre_temp - valley_pre > 0.3 && valley_index_diff > peak_valley_index_interval && peak_index_diff > peak_valley_index_interval)
                        {


                            // textView4.setText("start_buffer_count:" + "\r\n"
                            // + start_buffer_count);
                            peak_pre_index = peak_pre_index_temp;
                            peak_pre = peak_pre_temp;
                            if (start_buffer_count < 8)
                            {
                                start_buffer_count = start_buffer_count + 2;
                            } else if (start_buffer_count == 8)
                            {
                                step_count = step_count + 10;
                                start_buffer_count = start_buffer_count + 2;
                                Log.e(TAG,"lixiang---start_buffer_count002= "+start_buffer_count);
                            } else
                            {
                                if (peak_pre_index - valley_pre_index > 100)
                                {
                                    Log.e(TAG,"lixiang---step_count002= "+start_buffer_count);
                                    step_count = step_count + 1;
                                } else
                                {
                                    step_count = step_count + 2;
                                }
                            }
                            valley_pre_index = j_valley;
                            valley_pre = acc3_data_nodiff[j_valley][axis];
                            String b3 = peak_pre + "\t\n" + peak_pre_index + "\t\n" + valley_pre + "\t\n" + valley_pre_index + "\r\n";
                            try
                            {
                                fos3.write(b3.getBytes());

                            } catch (IOException e1)
                            {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();

                            }

                        }
                        step_pre_index = i;
                        step_index_temp = i + 5;
//                        textView.setText("step cout:" + "\r\n" + step_count);
//                        for (StepListener stepListener : mStepListeners) {
//                            stepListener.onStep(step_count);
//                        }
                        return step_count;

                    }
                }
            }
        }
//        textView.setText("step cout:" + "\r\n" + step_count);
        for (StepListener stepListener : mStepListeners) {
            stepListener.onStep(step_count);
        }
        return step_count;

    }

    private int step_diff_simulate_highfreq(float accdata[][], int len_buffer) throws IOException
    {
        float sum_acc_energy = 0;
        double step_threshold = 0;
        int len_acc = 1000;
        float temp;

        float[][] acc3_diff = new float[len_acc - 1][3];
        float[][] acc3_data = new float[len_acc - 1][2];
        float[][] acc3_data_filter = new float[len_acc - 1][1];
        float[][] acc3_data_lowpass = new float[len_acc - 1][3];
        float[][] acc3_data_temp = new float[len_acc][3];
        float[][] accdata_temp = new float[len_acc][3];

        if (acc_pos_flag == -1)
        {
            for (int i = 0; i < 1000; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = -accdata[i][j];
                    if (acc_pos_x_mean == 0)
                    {
                        accdata_temp[i][j] = accdata_temp[i][j] + 1;
                    }
                }
            }
        } else
        {
            for (int i = 0; i < 1000; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = accdata[i][j];
                    if (acc_pos_x_mean == 0)
                    {
                        accdata_temp[i][j] = accdata_temp[i][j] + 1;
                    }

                }
            }
        }

        for (int j = 0; j < 2; j++)
        {
            for (int i = 0; i < acc3_diff.length; i++)
            {
                // ////////////////////////////////////////
                acc3_diff[i][j] = (float) (this.diff(accdata_temp, i, j) / 0.01);

            }
            for (int i = 0; i < acc3_data_temp.length; i++)
            {
                acc3_data_temp[i][j] = this.convolve(accdata_temp, i, j);

            }
        }
        for (int i = 0; i < 2; i++)
        {
            for (int m = 0; m < len_acc - 1; m++)
            {
                acc3_data_lowpass[m][i] = acc3_data_temp[m][i];
                if (acc3_data_lowpass[m][i] < 0)
                {
                    acc3_data_lowpass[m][i] = 0;
                }
            }
        }

        for (int m = 0; m < len_acc - 1; m++)
        {
            acc3_data[m][1] = acc3_data_temp[m][1];
            acc3_data[m][0] = acc3_data_temp[m][0];

        }
        for (int i = 0; i < acc3_data_filter.length; i++)
        {
            acc3_data_filter[i][0] = acc3_data_lowpass[i][0];
        }

        if (len_buffer - step_index_temp > 999)
        {
            step_index_temp = len_buffer - 5;
        }

        if (first_flag == 0)
        {
            first_flag = first_flag + 1;
        } else
        {
            for (int i = step_index_temp; i < len_buffer - 2; i++)
            {
                if ((acc3_data_filter[i + 1000 - len_buffer][0] > acc3_data_filter[i + 1000 - len_buffer - 1][0]) && (acc3_data_filter[i + 1000 - len_buffer][0] > acc3_data_filter[i + 1000 - len_buffer + 1][0]))
                {
                    for (int j = i - N; j < i; j++)
                    {
                        sum_acc_energy = sum_acc_energy + acc3_data_filter[j + 1000 - len_buffer][0] * acc3_data_filter[j + 1000 - len_buffer][0];

                    }
                    step_threshold = sum_acc_energy / N;

                    step_threshold = Math.sqrt(step_threshold);
                    Log.e(TAG,"lixiang---sum_acc_energy= "+Math.pow(Pn, NN));
                    sum_acc_energy = 0;
                    if (acc3_data_filter[i + 1000 - len_buffer][0] > step_threshold + 1)
                    {
                        if (peak_flag == 1)
                        {
                            peak_pre = acc3_data_filter[i + 1000 - len_buffer][0];
                            step_pre_index = i;
                            step_index_temp = i + 1;

                            step_index_temp = step_index_temp + 10;
                            valley_pre_index = step_index_temp;
                            // textView4.setText("start_buffer_count:" + "\r\n"
                            // + start_buffer_count);
                            if (start_buffer_count < 9)
                            {
                                start_buffer_count = start_buffer_count + 1;
                            } else if (start_buffer_count == 9)
                            {
                                step_count = step_count + 10;
                                start_buffer_count = start_buffer_count + 1;
                                Log.e(TAG,"lixiang---start_buffer_count003= "+start_buffer_count);
                            } else
                            {
                                Log.e(TAG,"lixiang---step_count003= "+step_count);
                                step_count = step_count + 1;
                            }
                            step_threshold = 0;
                        } else if (peak_flag == 0)
                        {
                            peak_pre = acc3_data_filter[i + 1000 - len_buffer][0];

                            step_pre_index = i;
                            step_index_temp = i + 10;
                            step_threshold = 0;
                        }
//                        textView.setText("step cout:" + "\r\n" + step_count);
//                        for (StepListener stepListener : mStepListeners) {
//                            stepListener.onStep(step_count);
//                        }
                        return step_count;
                    }
                }

            }
        }
//        textView.setText("step cout:" + "\r\n" + step_count);
        for (StepListener stepListener : mStepListeners) {
            stepListener.onStep(step_count);
        }
        return step_count;
    }

    private int step_diff_simulate_highfreq_initial(float accdata[][]) throws IOException
    {

        float sum_acc_energy = 0;
        double step_threshold = 0;
        int len_acc = buffer_count + 1;

        float[][] acc3_diff = new float[len_acc - 1][3];
        float[][] acc3_data = new float[len_acc - 1][2];
        float[][] acc3_data_filter = new float[len_acc - 1][1];
        float[][] acc3_data_lowpass = new float[len_acc - 1][3];
        float[][] acc3_data_temp = new float[len_acc][3];
        float[][] accdata_temp = new float[len_acc][3];

        if (acc_pos_flag == -1)
        {
            for (int i = 0; i < len_acc; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = -accdata[i][j];
                    if (acc_pos_x_mean == 0)
                    {
                        accdata_temp[i][j] = accdata_temp[i][j] + 1;
                    }
                }
            }
        }

        else
        {
            for (int i = 0; i < len_acc; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    accdata_temp[i][j] = accdata[i][j];
                    if (acc_pos_x_mean == 0)
                    {
                        accdata_temp[i][j] = accdata_temp[i][j] + 1;
                    }

                }
            }
        }

        for (int j = 0; j < 2; j++)
        {
            for (int i = 0; i < acc3_diff.length; i++)
            {
                acc3_diff[i][j] = (float) (this.diff(accdata_temp, i, j) / 0.01);

            }
            for (int i = 0; i < acc3_data_temp.length; i++)
            {
                acc3_data_temp[i][j] = this.convolve(accdata_temp, i, j);

            }
        }
        for (int i = 0; i < 2; i++)
        {
            for (int m = 0; m < len_acc - 1; m++)
            {
                acc3_data_lowpass[m][i] = acc3_data_temp[m][i];
                if (acc3_data_lowpass[m][i] < 0)
                {
                    acc3_data_lowpass[m][i] = 0;
                }
            }
        }

        for (int m = 0; m < len_acc - 1; m++)
        {
            acc3_data[m][1] = acc3_data_temp[m][1];
            acc3_data[m][0] = acc3_data_temp[m][0];

        }
        for (int i = 0; i < acc3_data_filter.length; i++)
        {
            acc3_data_filter[i][0] = acc3_data_lowpass[i][0];
        }
        if (first_flag == 0)
        {
            first_flag = first_flag + 1;
        } else
        {
            Log.e(TAG,"lixiang---step_count2");
            for (int i = step_index_temp; i < len_acc - 2; i++)
            {
                Log.e(TAG,"lixiang---acc3_data_filter[i][0]= "+acc3_data_filter[i][0]+"   "+acc3_data_filter[i - 1][0]+"  "+acc3_data_filter[i + 1][0]);
                if ((acc3_data_filter[i][0] > acc3_data_filter[i - 1][0]) && (acc3_data_filter[i][0] > acc3_data_filter[i + 1][0]))
                {

                    for (int j = i - N; j < i; j++)
                    {
                        sum_acc_energy = sum_acc_energy + acc3_data_filter[j][0] * acc3_data_filter[j][0];
                    }
                    step_threshold = sum_acc_energy / N;

                    step_threshold = Math.sqrt(step_threshold);
                    sum_acc_energy = 0;

                    if (acc3_data_filter[i][0] > step_threshold + 1)
                    {
                        if (peak_flag == 1)
                        {
                            peak_pre = acc3_data_filter[i][0];
                            step_pre_index = i;
                            step_index_temp = i + 1;
                            // textView4.setText("start_buffer_count:" + "\r\n"
                            // + start_buffer_count);
                            step_index_temp = step_index_temp + 10;
                            valley_pre_index = step_index_temp;
                            if (start_buffer_count < 9)
                            {
                                start_buffer_count = start_buffer_count + 1;
                            } else if (start_buffer_count == 9)
                            {
                                step_count = step_count + 10;
                                start_buffer_count = start_buffer_count + 1;
                            } else
                            {
                                step_count = step_count + 1;
                            }
                            step_threshold = 0;
                        } else if (peak_flag == 0)
                        {
                            peak_pre = acc3_data_filter[i][0];

                            step_pre_index = i;
                            step_index_temp = i + 10;
                            step_threshold = 0;
                        }
//                        textView.setText("step cout:" + "\r\n" + step_count);
//                        for (StepListener stepListener : mStepListeners) {
//                            stepListener.onStep(step_count);
//                        }
                        return step_count;
                    }
                }
            }
        }
//        textView.setText("step cout:" + "\r\n" + step_count);
//        for (StepListener stepListener : mStepListeners) {
//            stepListener.onStep(step_count);
//        }
        return step_count;

    }


//    @Override
//    public void onSensorChanged(SensorEvent sensorEvent) {
//        float x= sensorEvent.values[SensorManager.DATA_X];
//        float y= sensorEvent.values[SensorManager.DATA_Y];
//        float z= sensorEvent.values[SensorManager.DATA_Z];
//        Log.e(TAG,"x= "+x+"   y= "+y+"  z= "+z);
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int i) {
//
//    }
}