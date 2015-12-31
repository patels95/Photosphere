package com.patels95.sanam.stormy.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.patels95.sanam.stormy.R;
import com.patels95.sanam.stormy.util.States;
import com.patels95.sanam.stormy.weather.Current;
import com.patels95.sanam.stormy.weather.Day;
import com.patels95.sanam.stormy.weather.Forecast;
import com.patels95.sanam.stormy.weather.Hour;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.lang.Math;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";
    public static final String USER_LOCATION_LABEL = "USER_LOCATION_LABEL";


    private Forecast mForecast;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String mUserLocationLabel;

    @InjectView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @InjectView(R.id.timeLabel) TextView mTimeLabel;
    @InjectView(R.id.humidityValue) TextView mHumidityValue;
    @InjectView(R.id.precipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.refreshImageView) ImageView mRefreshImageView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.locationLabel) TextView mLocationLabel;


    //declare latitude and longitude
    double mLatitude;
    double mLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mProgressBar.setVisibility(View.INVISIBLE);

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        //refresh forecast when the refresh button is pressed
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "loc = " + mLatitude + " and lat = " + mLongitude);
                getForecast(mLatitude, mLongitude);
                updateCity(getCityName(mLatitude, mLongitude));
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void updateCity(String[] cityState) {
        String location = cityState[0] + ", " + cityState[1];
        mUserLocationLabel = location;
        mLocationLabel.setText(location);
    }

    private String[] getCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        String[] cityState = new String[2];
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            String cityName = addresses.get(0).getLocality();
            States states = new States();
            String stateName = states.getStateAbbreviation(addresses.get(0).getAdminArea());
            cityState[0] = cityName;
            cityState[1] = stateName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityState;
    }

    private void getForecast(double latitude, double longitude) {
        //all api variables
        String apiKey = "037097f3d37b822a082c9b6e5b609271";
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + latitude + "," + longitude;

        if(isNetworkAvailable()) {
            toggleRefresh();

            //OkHttp
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            //asynchronous call
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    }
                    catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        }
        else{
            Toast.makeText(this, getString(R.string.network_error_msg), Toast.LENGTH_LONG).show();
        }
    }
    //toggle the visibility of the refresh button and progress bar
    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE){
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else{
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }

    }

    private void updateDisplay() {
        Current current = mForecast.getCurrent();
        mTemperatureLabel.setText(current.getTemperature() + "");
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(Math.round(current.getHumidity()) + "%");
        mPrecipValue.setText(current.getPrecipChance() + "%");
        mSummaryLabel.setText(current.getSummary());
        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        Forecast forecast = new Forecast();
        // get weather from json data (forecast api) and set forecast
        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));

        return forecast;
    }

    private Day[] getDailyForecast(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");

        Day[] days = new Day[data.length()];

        for(int i = 0;i < data.length();i++) {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();
            day.setSummary(jsonDay.getString("summary"));
            day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            day.setIcon(jsonDay.getString("icon"));
            day.setTime(jsonDay.getLong("time"));
            day.setTimezone(timezone);

            days[i] = day;
        }
        return days;
    }

    private Hour[] getHourlyForecast(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        for(int i = 0;i < data.length();i++) {
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();
            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimezone(timezone);

            hours[i] = hour;
        }
        return hours;
    }

    private Current getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        JSONObject current = forecast.getJSONObject("currently");

        Current currentWeather = new Current();
        currentWeather.setHumidity(current.getDouble("humidity"));
        currentWeather.setTime(current.getLong("time"));
        currentWeather.setIcon(current.getString("icon"));
        currentWeather.setPrecipChance(current.getDouble("precipProbability"));
        currentWeather.setSummary(current.getString("summary"));
        currentWeather.setTemperature(current.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG,  currentWeather.getFormattedTime());
        return currentWeather;
    }

    //check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    //display an error dialog
    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    // ButterKnife onClick for daily weather button
    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view) {
        Intent intent = new Intent(this, DailyForecastActivity.class);
        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        intent.putExtra(USER_LOCATION_LABEL, mUserLocationLabel);
        startActivity(intent);
    }

    // ButterKnife onClick for hourly weather button
    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view){
        Intent intent = new Intent(this, HourlyForecastActivity.class);
        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = mLastLocation.getLatitude();
            mLongitude = mLastLocation.getLongitude();
            updateCity(getCityName(mLatitude, mLongitude));
            getForecast(mLatitude, mLongitude);
        }
        else{
            // mLastLocation is null
            Toast.makeText(MainActivity.this, R.string.location_error, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
