/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *	Download by http://www.codefans.net
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gomtel.pedometer.R;
import com.android.gomtel.pedometer.layout.ScrollLayout;

import java.text.DecimalFormat;

//import com.google.tts.TTS;


public class Pedometer extends Activity {

    private static final String TAG = "Pedometer";
    public static final String HEIGHT = "body_height";
    public static final String WEIGHT = "body_weight";
    public static final String LENGTH = "step_length";
    private static final int DEFALT_PAGE = 1;
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    
    private TextView mStepValueView;
//    private TextView mPaceValueView;
    private TextView mDistanceValueView;
//    private TextView mSpeedValueView;
    private TextView mCaloriesValueView;
    private Chronometer timer;
    private int mStepValue;
    private int mPaceValue;
    private int mDistanceValue;
    private float mSpeedValue;
    private int mCaloriesValue;
    private float mDesiredPaceOrSpeed;
    private int mMaintain;
    private boolean mIsMetric;
    private float mMaintainInc;
    private DecimalFormat df = new DecimalFormat(".#");
    private boolean mIsRunning;
    private ScrollLayout mScrollLayout;
    private TextView mSpeedValueView;
    private long startTimeStamp;
    private long startTime = 0L;
    private EditText bodyHeight;
    private EditText bodyWeight;
    private EditText stepLength;
    public Object mLock = new Object();
    private SharedPreferences sp;
    private TextWatcher mHeightText = new TextWatcher(){


        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String height = bodyHeight.getText().toString();
            synchronized (mLock) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(HEIGHT, height);
                editor.commit();
            }
            if(mService != null)
                mService.reloadSettings();
        }
    };

    private TextWatcher mWeightText = new TextWatcher(){
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String weight = bodyWeight.getText().toString();
            synchronized (mLock) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(WEIGHT, weight);
                editor.commit();
            }
            if(mService != null)
                mService.reloadSettings();
        }
    };

    private TextWatcher mLengthText = new TextWatcher(){


        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String length = stepLength.getText().toString();
            synchronized (mLock) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(LENGTH, length);
                editor.commit();
            }
            if(mService != null)
                mService.reloadSettings();
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mStepValue = 0;
        mPaceValue = 0;
        
        setContentView(R.layout.main);
        startStepService();
        initView();

    }

    private void initView() {

        mScrollLayout = (ScrollLayout) findViewById(R.id.sliding_page);
        mStepValueView     = (TextView) findViewById(R.id.step_value);
        mDistanceValueView = (TextView) findViewById(R.id.distance_value);
        mCaloriesValueView = (TextView) findViewById(R.id.calories_value);
        timer = (Chronometer) findViewById(R.id.chronometer);
        mScrollLayout.setToScreen(DEFALT_PAGE);
        sp = getSharedPreferences("settings_pedometer", MODE_PRIVATE);

        bodyHeight = (EditText)findViewById(R.id.enter_height);
        bodyHeight.setText(sp.getString(HEIGHT,"170"));
        bodyHeight.addTextChangedListener(mHeightText);

        bodyWeight = (EditText)findViewById(R.id.enter_weight);
        bodyWeight.setText(sp.getString(WEIGHT,"60"));
        bodyWeight.addTextChangedListener(mWeightText);

        stepLength = (EditText)findViewById(R.id.enter_step_length);
        stepLength.setText(sp.getString(LENGTH,"70"));
        stepLength.addTextChangedListener(mLengthText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
//        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
//        mPedometerSettings = new PedometerSettings(mSettings);
        
        if (mIsRunning) {
            bindStepService();
        }

        Display mDisplay = getWindowManager().getDefaultDisplay();
        int W = mDisplay.getWidth();
        int H = mDisplay.getHeight();
        Log.e("Main", "Width = " + W);
        Log.e("Main", "Height = " + H);
//        mIsMetric = mPedometerSettings.isMetric();
        ((TextView) findViewById(R.id.distance_units)).setText(getString(
                mIsMetric
                ? R.string.kilometers
                : R.string.meters
        ));
//        ((TextView) findViewById(R.id.speed_units)).setText(getString(
//                mIsMetric
//                ? R.string.kilometers_per_hour
//                : R.string.miles_per_hour
//        ));

    }

    
    @Override
    protected void onPause() {
        if (mIsRunning) {
            unbindStepService();
        }
        if(mService.getStartTime() == 0)
            mService.setStartTime(startTimeStamp);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this)
                .setMessage(R.string.quit_msg)
                .setPositiveButton(R.string.quit_btn,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                stopStepService();
                                finish();
                            }
                        })
                .setNegativeButton(R.string.run_background,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).create().show();
//        super.onBackPressed();
    }


    private StepService mService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();
            Log.e(TAG,"lixiang---mService.getStartTime()= "+mService.getStartTime());
            if(mService.getStartTime() == 0) {
                startTime = SystemClock.elapsedRealtime();
                startTimeStamp = startTime;
            }else{
                startTime = mService.getStartTime();}
            timer.setBase(startTime);
            timer.setFormat("%s");
            timer.start();
            mService.registerCallback(mCallback);
            mService.reloadSettings();
            
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    

    private void startStepService() {
        mIsRunning = true;
        startService(new Intent(Pedometer.this,
                StepService.class));
    }
    
    private void bindStepService() {
        bindService(new Intent(Pedometer.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindStepService() {
        unbindService(mConnection);
    }
    
    private void stopStepService() {
        mIsRunning = false;
        if (mService != null) {
            stopService(new Intent(Pedometer.this,
                  StepService.class));
        }
    }
    
    private void resetValues(boolean updateDisplay) {
        if (mService != null && mIsRunning) {
            mService.resetValues();                    
        }
        else {
            mStepValueView.setText("0");
            mDistanceValueView.setText("0");
            mCaloriesValueView.setText("0");
            SharedPreferences state = getSharedPreferences("state", 0);
            SharedPreferences.Editor stateEditor = state.edit();
            if (updateDisplay) {
                stateEditor.putInt("steps", 0);
                stateEditor.putInt("pace", 0);
                stateEditor.putFloat("distance", 0);
                stateEditor.putFloat("speed", 0);
                stateEditor.putFloat("calories", 0);
                stateEditor.commit();
            }
        }
    }

    private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT     = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;
    
    /* Creates the menu items */
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        menu.clear();
//        if (mIsRunning) {
//            menu.add(0, MENU_PAUSE, 0, R.string.pause)
//            .setIcon(android.R.drawable.ic_media_pause)
//            .setShortcut('1', 'p');
//        }
//        else {
//            menu.add(0, MENU_RESUME, 0, R.string.resume)
//            .setIcon(android.R.drawable.ic_media_play)
//            .setShortcut('1', 'p');
//        }
//        menu.add(0, MENU_RESET, 0, R.string.reset)
//        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
//        .setShortcut('2', 'r');
//        menu.add(0, MENU_SETTINGS, 0, R.string.settings)
//        .setIcon(android.R.drawable.ic_menu_preferences)
//        .setShortcut('8', 's')
//        .setIntent(new Intent(this, Settings.class));
//        menu.add(0, MENU_QUIT, 0, R.string.quit)
//        .setIcon(android.R.drawable.ic_lock_power_off)
//        .setShortcut('9', 'q');
//        return true;
//    }

    /* Handles item selections */
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case MENU_PAUSE:
//                unbindStepService();
//                stopStepService();
//                return true;
//            case MENU_RESUME:
//                startStepService();
//                bindStepService();
//                return true;
//            case MENU_RESET:
//                resetValues(true);
//                return true;
//            case MENU_QUIT:
//                resetValues(false);
//                stopStepService();
//                finish();
//                return true;
//        }
//        return false;
//    }
 
    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
        public void paceChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }
        public void distanceChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(DISTANCE_MSG, (int)(value), 0));
        }
        public void speedChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(SPEED_MSG, (int)(value*1000), 0));
        }
        public void caloriesChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(CALORIES_MSG, (int)(value), 0));
        }
    };
    
    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;
    private static final int DISTANCE_MSG = 3;
    private static final int SPEED_MSG = 4;
    private static final int CALORIES_MSG = 5;
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int)msg.arg1;
                    Log.e(TAG,"lixiang---mStepValue = "+mStepValue);
                    mStepValueView.setText("" + mStepValue);
                    break;
                case PACE_MSG:
                    mPaceValue = msg.arg1;
                    break;
                case DISTANCE_MSG:

                    mDistanceValue = (msg.arg1);
                    Log.e("lixiang","lixiang---mDistanceValue= "+mDistanceValue);
//                    mDistanceValue = 1234;
                    if (mDistanceValue <= 0) { 
                        mDistanceValueView.setText("0");
                    }else  if(mDistanceValue > 1000) {
                        mIsMetric = true;

                        mDistanceValueView.setText(
                                "" + df.format(((double)mDistanceValue)/1000)
                        );
                    }else {
                        mDistanceValueView.setText(
                                "" + mDistanceValue
                        );

                    }
                    ((TextView) findViewById(R.id.distance_units)).setText(getString(
                            mIsMetric
                                    ? R.string.kilometers
                                    : R.string.meters
                    ));
                    break;
                case SPEED_MSG:
                    mSpeedValue = ((int)msg.arg1)/1000f;
                    break;
                case CALORIES_MSG:
                    mCaloriesValue = msg.arg1;
                    if (mCaloriesValue <= 0) { 
                        mCaloriesValueView.setText("0");
                    }
                    else {
                        mCaloriesValueView.setText("" + (int)mCaloriesValue);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };

    
}