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


/**
 * Calculates and displays the distance walked.  
 * @author Levente Bagi
 */
public class DistanceNotifier implements StepListener {

    public interface Listener {
        public void valueChanged(float value);
        public void passValue();
    }
    private Listener mListener;

    float mDistance = 0;

    PedometerSettings mSettings;
    
    boolean mIsMetric;
    float mStepLength;

    public DistanceNotifier(Listener listener, PedometerSettings settings) {
        mListener = listener;
        mSettings = settings;
        reloadSettings();
    }
    public void setDistance(float distance) {
        mDistance = distance;
        notifyListener();
    }
    

    public void reloadSettings() {
        mIsMetric = mSettings.isMetric();
        mStepLength = mSettings.getStepLength();
//        mStepLength = 70;
        notifyListener();
    }
    
    public void onStep(int stepcount) {
        
//        if (mIsMetric) {
            mDistance = (float)(// kilometers
                mStepLength * stepcount// centimeters
                /100.0); // centimeters/kilometer
//        }
//        else {
//            mDistance = (float)(// miles
//                mStepLength * stepcount // inches
//                / 63360.0); // inches/mile
//        }
        
        notifyListener();
    }
    
    private void notifyListener() {
        mListener.valueChanged(mDistance);
    }
    
    public void passValue() {
        // Callback of StepListener - Not implemented
    }

    

}

