package com.patels95.sanam.stormy.ui;

import android.content.Context;
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

import com.patels95.sanam.stormy.R;
import com.patels95.sanam.stormy.weather.Current;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private Current mCurrent;

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
    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mProgressBar.setVisibility(View.INVISIBLE);


        //get location from the user
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30, 1, locationListener);
        //log the latitude and longitude
        Log.d(TAG, "loc = " + latitude + " and " + longitude);

        //default forecast location - Alcatraz
//        final double latitude = 37.8267;
//        final double longitude = -122.423;


        //refresh forecast when the refresh button is pressed
        mRefreshImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                getForecast(latitude, longitude);
                //updateCity(getCityName(latitude, longitude));
            }
        });
        getForecast(latitude, longitude);
        //updateCity(getCityName(latitude, longitude));

        Log.d(TAG, "ui code is running");
    }

    private void updateCity(String[] cityState) {
        mLocationLabel.setText(cityState[0] + ", " + cityState[1]);
    }

    private String[] getCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        String[] cityState = new String[2];
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            //ERROR with geo data
//            String cityName = addresses.get(0).getAddressLine(0);
//            String stateName = addresses.get(0).getAddressLine(1);
//            cityState[0] = cityName;
//            cityState[1] = stateName;
//            Log.d(TAG, "city = " + cityState[0] + " and state =  " + cityState[1]);
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
                            mCurrent = getCurrentDetails(jsonData);
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
        mTemperatureLabel.setText(mCurrent.getTemperature() + "");
        mTimeLabel.setText("At " + mCurrent.getFormattedTime() + " it will be");
        mHumidityValue.setText(mCurrent.getHumidity() + "%");
        mPrecipValue.setText(mCurrent.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrent.getSummary());
        Drawable drawable = getResources().getDrawable(mCurrent.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private Current getCurrentDetails(String jsonData) throws JSONException{
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
}