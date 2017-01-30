package com.algonquincollege.smartmomscanada_android.MainPages;


import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.algonquincollege.smartmomscanada_android.R;
import com.algonquincollege.smartmomscanada_android.Util.APIHelper;
import com.algonquincollege.smartmomscanada_android.Util.DataHelper;
import com.algonquincollege.smartmomscanada_android.Util.JSONMAPConverter;
import com.algonquincollege.smartmomscanada_android.Util.MyApp;
import com.github.lzyzsd.circleprogress.DonutProgress;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.R.attr.delay;
import static com.algonquincollege.smartmomscanada_android.R.id.weekLayout;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.CURRENT_MAX_WEIGHT;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.CURRENT_MIN_WEIGHT;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.CURRENT_WEIGHT;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.LAST_ACTIVITY_DATA_DATE;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.LAST_WEIGHT_DATA_DATE;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.LOG_TAG;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.POUNDS;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.SMARTMOM_API_KEY;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.UNITS;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_CALORIES_COUNT;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_HEART_RATE;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_HEAVY_ACTIVITY;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_LIGHT_ACTIVITY;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_MODERATE_ACTIVITY;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_SLEEP_COUNT;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_STEPS_COUNT;
import static com.algonquincollege.smartmomscanada_android.Util.Constants.USER_STEPS_GOAL;

/**
 * Author: Matt Young
 * Date: 2016-08-18
 *
 * Description: Fragment that runs off of the main activity and controls the health page. The page uses a static arc gauge image
 * along with a moving white arrow that rotates on its axis to display the users weight in "the zone". The page displays a variety
 * of data that comes from the user's fitbit account. An external library was used for the activity circle progress bar
 *
 * Resources: https://github.com/lzyzsd/CircleProgress
 */

public class HealthFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    //GLOBAL VARIABLE DECLARATIONS
    private View view = null;
    private boolean unitBool = true;
    private boolean firstLoad = false;//true=kgs false=lbs
    private DataHelper dataHelper;
    private DonutProgress lightWheel, modWheel, heavyWheel;
    private ImageView arrow;
    private int lightEx, modEx, heavyEx, modExAdd, stepsVal, stepsGoalVal, hrVal, caloriesVal, sleepVal;
    private float totalEx, progress;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView totalPercentText, lightExText, modExText, heavyExText, stepsText, stepsGoalText, hrTxt, caloriesText, sleepText;
    private float minWeight, maxWeight, currentWeight;
    private String currentDay;


    public HealthFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        //inflate the health fragment layout and refresh the toolbar options menu
        view = inflater.inflate(R.layout.fragment_health, container, false);
        setHasOptionsMenu(true);

        //init and set up the swipe to refresh layout
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        //set and init view elements and global variables
        arrow = (ImageView) view.findViewById(R.id.arrowImage);
        //lightWheel = (DonutProgress) view.findViewById(R.id.light_progress);
        modWheel = (DonutProgress) view.findViewById(R.id.moderate_progress);
        heavyWheel = (DonutProgress) view.findViewById(R.id.heavy_progress);
        totalPercentText = (TextView) view.findViewById(R.id.exPercentText);
        //lightExText = (TextView) view.findViewById(R.id.lightExNum);
        modExText = (TextView) view.findViewById(R.id.modExNum);
        heavyExText = (TextView) view.findViewById(R.id.heavyExNum);

        stepsText = (TextView) view.findViewById(R.id.stepsCount);
        stepsGoalText = (TextView) view.findViewById(R.id.stepsCountMax);
        hrTxt = (TextView) view.findViewById(R.id.hrCount);
        caloriesText = (TextView) view.findViewById(R.id.calCount);
        sleepText = (TextView) view.findViewById(R.id.sleepCount);

        //lightEx = 0;
        modEx = 10;
        heavyEx = 0;
        stepsVal = 0;
        sleepVal = 0;
        caloriesVal = 0;
        stepsGoalVal = 0;
        hrVal = 0;

        //create a calendar instance for today's date
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        currentDay = sdf.format(cal.getTime());

        //init data help and check for desired metric or imperial data
        dataHelper = new DataHelper(getActivity());
        String units = dataHelper.getData(UNITS);

        //if units settings is equal to imperial/pounds, flip bool and set text accordingly
        if(units.equals(POUNDS)){
            unitBool = false;
            ((TextView)view.findViewById(R.id.minUnit)).setText(getResources().getString(R.string.pounds));
            ((TextView)view.findViewById(R.id.maxUnit)).setText(getResources().getString(R.string.pounds));
            ((TextView)view.findViewById(R.id.userUnit)).setText(getResources().getString(R.string.pounds));
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        final RefreshApiCall refreshApiCall = new RefreshApiCall();

        swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                    //refresh api data
                    refreshApiCall.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            // Code executes ONLY THE FIRST TIME fragment is viewed.
    }

    @Override
    public void onPause(){
        super.onPause();

        if (swipeRefreshLayout!=null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.destroyDrawingCache();
            swipeRefreshLayout.clearAnimation();
        }
    }

    @Override
    public void onRefresh() {

        Menu menu = ((MainActivity) getActivity()).getMenu();
        menu.findItem(R.id.action_graph).setVisible(false);

        RefreshApiCall refreshApiCall = new RefreshApiCall();

        swipeRefreshLayout.setRefreshing(true);
        //refresh api call method
        refreshApiCall.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    class RefreshApiCall extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {


        }

        @Override
        protected Void doInBackground(Void... unused) {
            //check connection: get new or old data
            if (APIHelper.checkConnection(MyApp.getContext())) {
               getActivityData();
                getWeightData();
                // showing refresh animation before making http call
                //swipeRefreshLayout.setRefreshing(true);
            } else {
                //load stored data.
                loadStoredWeightData();
                loadStoredActivityData();
                // showing refresh animation before making http call
                //swipeRefreshLayout.setRefreshing(false);
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void v) {

            //reset fields
            //lightWheel.setProgress(0);
            modWheel.setProgress(0);
            heavyWheel.setProgress(0);
            //lightExText.setText("0 min");
            modExText.setText("0 min");
            heavyExText.setText("0 min");
            totalPercentText.setText("0%");

            //set the data and start animations
            setWeightData();
            setActivityData();
            startActivityWheel();
            moveArrow();

            swipeRefreshLayout.setRefreshing(false);

            Menu menu = ((MainActivity) getActivity()).getMenu();
            menu.findItem(R.id.action_graph).setVisible(true);

        }


        private void getWeightData() {

            //set up context for API helper call to get the weekly weight data
            Map<String, String> requestBody = new HashMap<>();
            String key = dataHelper.getData(SMARTMOM_API_KEY);
            int currentWeek = dataHelper.getCurrentWeek();

            requestBody.put("week", Integer.toString(currentWeek));

            Map<String, String> result = APIHelper.getWeeklyWeight(requestBody, key, MyApp.getContext());

            //verify that result has required fields
            if (result.containsKey("actual")) {

                //store the string weight data to shared preferences for offline accessibility
                dataHelper.setData(CURRENT_WEIGHT, result.get("actual"));
                dataHelper.setData(CURRENT_MIN_WEIGHT, result.get("lower"));
                dataHelper.setData(CURRENT_MAX_WEIGHT, result.get("upper"));

                dataHelper.setData(LAST_WEIGHT_DATA_DATE, currentDay);

                //set weight variables as either imperial or metric depending on bool status
                if (unitBool) {
                    minWeight = Float.parseFloat(result.get("lower"));
                    maxWeight = Float.parseFloat(result.get("upper"));
                    currentWeight = Float.parseFloat(result.get("actual"));
                } else {
                    minWeight = dataHelper.kgToLbs(Float.parseFloat(result.get("lower")));
                    maxWeight = dataHelper.kgToLbs(Float.parseFloat(result.get("upper")));
                    currentWeight = dataHelper.kgToLbs(Float.parseFloat(result.get("actual")));
                }

            } else if (result.containsKey("error") || (result.containsKey("HTTP_ERROR"))) {
                // HANDLE ERROR RESPONSE FROM API/SERVER
                Log.i("ERROR", "Get Weight Data returned error.");
                //call to load stored weight data
                loadStoredWeightData();
            }
        }

        private void loadStoredWeightData() {
            Log.i("CONN", "Grabbed Stored Data");
            //the result failed, grab latest current day from shared pref and see if stored data is fresh
            String day = dataHelper.getData(LAST_WEIGHT_DATA_DATE);

            // Handle the error, pull from shared preferences if data is fresh from current day
            if (day.equals(currentDay)) {
                //set weight variables as either imperial or metric depending on bool status
                if (unitBool) {
                    minWeight = Float.parseFloat(dataHelper.getData(CURRENT_MIN_WEIGHT));
                    maxWeight = Float.parseFloat(dataHelper.getData(CURRENT_MAX_WEIGHT));
                    currentWeight = Float.parseFloat(dataHelper.getData(CURRENT_WEIGHT));
                } else {
                    minWeight = dataHelper.kgToLbs(Float.parseFloat(dataHelper.getData(CURRENT_MIN_WEIGHT)));
                    maxWeight = dataHelper.kgToLbs(Float.parseFloat(dataHelper.getData(CURRENT_MAX_WEIGHT)));
                    currentWeight = dataHelper.kgToLbs(Float.parseFloat(dataHelper.getData(CURRENT_WEIGHT)));
                }
            } else {
                //no connection and no recent data to display, show toast message
                Toast toast = Toast.makeText(getActivity(), "Failed Connection: No recent data to display.", Toast.LENGTH_SHORT);
                TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                if (v != null) v.setGravity(Gravity.CENTER);
                toast.show();
            }
        }

        private void getActivityData() {

            //grab the activity data from server using the data helper
            Map<String, String> result = APIHelper.getDailyActivity(dataHelper.getData(SMARTMOM_API_KEY), getActivity().getApplicationContext());
            Log.d(LOG_TAG, result.toString());

            //check to make sure the data came through with no error message
            if (!result.containsKey("error")) {

                //create a json converter and transfer the string data into use-able map objects
                JSONMAPConverter conv = new JSONMAPConverter();
                Map<String, String> activity = conv.convertToMap(result.get("activity"));
                Map<String, String> steps = conv.convertToMap(result.get("steps"));
                Map<String, String> hr = conv.convertToMap(result.get("heart_rate"));
                Map<String, String> calories = conv.convertToMap(result.get("calories"));
                Map<String, String> sleep = conv.convertToMap(result.get("sleep"));

                //check to make sure no data element is null and set variables if not null
                if (activity.get("lightlyActiveMin") != "null")
                    lightEx = Integer.parseInt(activity.get("lightlyActiveMin"));
                if (activity.get("fairlyActiveMinutes") != "null")
                    modEx = Integer.parseInt(activity.get("fairlyActiveMinutes"));
                if (activity.get("veryActiveMinutes") != "null")
                    heavyEx = Integer.parseInt(activity.get("veryActiveMinutes"));
                if (steps.get("total") != "null") stepsVal = Integer.parseInt(steps.get("total"));
                if (steps.get("goal") != "null") stepsGoalVal = Integer.parseInt(steps.get("goal"));
                if (hr.get("resting") != "null") hrVal = Integer.parseInt(hr.get("resting"));
                if (calories.get("in") != "null")
                    caloriesVal = Integer.parseInt(calories.get("in"));
                if (sleep.get("length") != "null") sleepVal = Integer.parseInt(sleep.get("length"));

                //store all this data in shared pref
                dataHelper.setData(USER_LIGHT_ACTIVITY, Integer.toString(lightEx));
                dataHelper.setData(USER_MODERATE_ACTIVITY, Integer.toString(modEx));
                dataHelper.setData(USER_HEAVY_ACTIVITY, Integer.toString(heavyEx));
                dataHelper.setData(USER_STEPS_COUNT, Integer.toString(stepsVal));
                dataHelper.setData(USER_STEPS_GOAL, Integer.toString(stepsGoalVal));
                dataHelper.setData(USER_HEART_RATE, Integer.toString(hrVal));
                dataHelper.setData(USER_SLEEP_COUNT, Integer.toString(sleepVal));
                dataHelper.setData(USER_CALORIES_COUNT, Integer.toString(caloriesVal));

                //set the current day in shared pref
                dataHelper.setData(LAST_ACTIVITY_DATA_DATE, currentDay);

            } else {
                // HANDLE ERROR RESPONSE FROM API/SERVER
                Log.i("ERROR", "Get Activity Data returned error.");
                //call to load stored activity data
                loadStoredActivityData();

            }
        }

        private void loadStoredActivityData() {
            Log.i("CONN", "Grabbed Stored ACtivity Data");
            //the result failed, grab latest current day from shared pref and see if stored data is fresh
            String day = dataHelper.getData(LAST_ACTIVITY_DATA_DATE);

            // Handle the error, pull from shared preferences if data is fresh from current day
            if (day.equals(currentDay)) {

                lightEx = Integer.parseInt(dataHelper.getData(USER_LIGHT_ACTIVITY));
                modEx = Integer.parseInt(dataHelper.getData(USER_MODERATE_ACTIVITY));
                heavyEx = Integer.parseInt(dataHelper.getData(USER_HEAVY_ACTIVITY));
                stepsVal = Integer.parseInt(dataHelper.getData(USER_STEPS_COUNT));
                stepsGoalVal = Integer.parseInt(dataHelper.getData(USER_STEPS_GOAL));
                hrVal = Integer.parseInt(dataHelper.getData(USER_HEART_RATE));
                sleepVal = Integer.parseInt(dataHelper.getData(USER_SLEEP_COUNT));
                caloriesVal = Integer.parseInt(dataHelper.getData(USER_CALORIES_COUNT));
            } else {
                //no connection and no recent data to display, show toast message
                Toast toast = Toast.makeText(getActivity(), "Failed Connection: No recent data to display.", Toast.LENGTH_SHORT);
                TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                if (v != null) v.setGravity(Gravity.CENTER);
                toast.show();
            }
        }

    }

    private void setWeightData() {

        //display the appropriate zone message and color depending on data
        if (currentWeight < minWeight) {
            ((TextView) view.findViewById(R.id.zoneText)).setText(getResources().getString(R.string.underZoneText));
            ((TextView) view.findViewById(R.id.zoneText)).setTextColor(getResources().getColor(R.color.lightPurple));
        } else if (currentWeight > maxWeight) {
            ((TextView) view.findViewById(R.id.zoneText)).setText(getResources().getString(R.string.overZoneText));
            ((TextView) view.findViewById(R.id.zoneText)).setTextColor(getResources().getColor(R.color.errorRed));
        } else {
            ((TextView) view.findViewById(R.id.zoneText)).setText(getResources().getString(R.string.inZoneText));
            ((TextView) view.findViewById(R.id.zoneText)).setTextColor(getResources().getColor(R.color.gaugeBlue));
        }

        ((TextView) view.findViewById(R.id.zoneText)).setVisibility(View.VISIBLE);

        //insert the weight data into text fields
        setMinWeight(minWeight);
        setMaxWeight(maxWeight);
        setCurrentWeight(currentWeight);
    }

    private void setActivityData(){

        //set the activity data into view fields
        stepsText.setText(Integer.toString(stepsVal) + ' ' + getResources().getString(R.string.steps));
        stepsGoalText.setText('/' + Integer.toString(stepsGoalVal));
        hrTxt.setText(Integer.toString(hrVal) + ' ' + getResources().getString(R.string.bpm));
        caloriesText.setText(Integer.toString(caloriesVal) + ' ' + getResources().getString(R.string.calories_short));

        int hours = sleepVal/60;
        int minutes = sleepVal % 60;
        sleepText.setText(Integer.toString(hours) + ' ' + getResources().getString(R.string.hours_short) + ' ' + Integer.toString(minutes) + ' ' + getResources().getString(R.string.min));
    }

    private void setMinWeight(float minWeight) {
        this.minWeight = minWeight;
        TextView min = (TextView) view.findViewById(R.id.minWeight);
        min.setText(String.format("%.1f", minWeight));
    }

    private void setMaxWeight(float maxWeight) {
        this.maxWeight = maxWeight;
        TextView max = (TextView) view.findViewById(R.id.maxWeight);
        max.setText(String.format("%.1f", maxWeight));
    }

    private void setCurrentWeight(float currentWeight) {
        this.currentWeight = currentWeight;
        TextView current = (TextView) view.findViewById(R.id.userWeight);
        current.setText(String.format("%.1f", currentWeight));
    }

    private void moveArrow(){

        //calculate the degrees rotation needed to move the weight gauge arrow
        float mid = (minWeight + maxWeight) / 2;
        float x = currentWeight - mid;
        float y = 71 / (maxWeight - minWeight);
        float rot = x * y;

        if(rot < -80){rot = -80;}
        else if(rot > 80) {rot = 80;}

        //create rotation animation for arrow
        final RotateAnimation rotateAnim = new RotateAnimation(0,rot,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 1f);

        //start rotation animation
        rotateAnim.setDuration(5);
        rotateAnim.setFillAfter(true);
        arrow.startAnimation(rotateAnim);
        rotateAnim.start();
    }

    private void startActivityWheel(){

        //get the accumulated activity numbers
        //totalEx = (lightEx + modEx + heavyEx);
        totalEx = (modEx + heavyEx);
        progress = 0;

        //if the total activity is more then required 30, extend the graphs maximums
        if (totalEx > (30)){
            //lightWheel.setMax((int)totalEx);
            modWheel.setMax((int)totalEx);
            heavyWheel.setMax((int)totalEx);
        }

        //start the progress wheel animation
        delayWheelProgress(1);
    }

    private void delayWheelProgress(int delay){

        //handler to hold delayed loop animation
        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {

                progress++;

//                if (progress <=lightEx){
//                    lightWheel.setProgress((int)progress);
//                    lightExText.setText(Integer.toString((int)(progress)) + "min");
//                }

                if (progress <= modEx){
                    modWheel.setProgress((int)progress);
//                    if (progress - lightEx > 0) {
//                    modExText.setText(Integer.toString((int) (progress) - lightEx) + "min");
                    modExText.setText(Integer.toString((int) (progress)) + " min");
//
//                    }
                }

                if (progress <= totalEx ){
                    heavyWheel.setProgress((int)progress);
//                    if (progress - (lightEx + modEx) > 0) {
                    if (progress - modEx > 0) {
//                        heavyExText.setText(Integer.toString((int) ((progress) - lightEx - modEx)) + "min");
                        heavyExText.setText(Integer.toString((int) ((progress) - modEx)) + " min");
                    }
                }


                if(totalEx != 0) {

                    if (totalEx > 30) {
                        totalPercentText.setText((Math.round((progress / totalEx) * 100)) + "%");
                    }else{
                        totalPercentText.setText((Math.round((progress / 30) * 100)) + "%");
                    }

                    if (totalEx != progress) {
                        delayWheelProgress(1 * ((int) progress / 2));
                    }
                }
            }
        }, delay);

    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();

        view = null;
    }

}
