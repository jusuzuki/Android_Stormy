package com.epicodus.stormy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private double mLatitude;
    private double mLongitude;
    private CurrentWeather mCurrentWeather;
    private ArrayList<CurrentWeather> mForecast;

    @Bind(R.id.locationLabel) TextView mLocationLabel;
    @Bind(R.id.timeLabel) TextView mTimeLabel;
    @Bind(R.id.temperatureLabel) TextView mTemperatureLabel;
    @Bind(R.id.humidityValue) TextView mHumidityValue;
    @Bind(R.id.precipValue) TextView mPrecipValue;
    @Bind(R.id.summaryLabel) TextView mSummaryLabel;
    @Bind(R.id.iconImageView) ImageView mIconImageView;
    @Bind(R.id.refreshImageView) ImageView mRefreshImageView;
    @Bind(R.id.progressBar) ProgressBar mProgressBar;

    @Bind(R.id.forecastBox) LinearLayout mForecastBox;
    @Bind(R.id.searchLayout) RelativeLayout mSearchLayout;
    @Bind(R.id.newCityButton) Button mNewCityButton;
    @Bind(R.id.newCitySearch) EditText mNewCitySearch;
    @Bind(R.id.searchButton) Button mSearchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Portland, OR as default location
        mLatitude = 45.5200;
        mLongitude = -122.6819;
        mLocationLabel.setText("Portland, OR");

        mProgressBar.setVisibility(View.INVISIBLE);

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(mLatitude, mLongitude);
            }
        });

        mNewCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchLayout.setVisibility(View.VISIBLE);
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newCity = mNewCitySearch.getText().toString();
                getLatLong(newCity, new Runnable() {
                    @Override
                    public void run() {
                        getForecast(mLatitude, mLongitude);
                    }
                });
                mSearchLayout.setVisibility(View.INVISIBLE);
            }
        });

        getForecast(mLatitude, mLongitude);

    }

    private void getLatLong(String newCity, final Runnable runnable) {
        String apiKey = "AIzaSyBxH81TQrhNFsIAgv-3a-JJ4O1IwmDMIcQ";
        String searchUrl = "https://maps.googleapis.com/maps/api/geocode/json?address="+newCity+"&key="+apiKey;
        mLocationLabel.setText(newCity);

        if(isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(searchUrl)
                    .build();

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
                        if (response.isSuccessful()) {
                            JSONObject newCityData = new JSONObject(jsonData);
                            JSONArray newCityResults = newCityData.getJSONArray("results");
                            JSONObject newCityJSON = newCityResults.getJSONObject(0);
                            JSONObject location = newCityJSON.getJSONObject("geometry").getJSONObject("location");
                            mLatitude = location.getDouble("lat");
                            mLongitude = location.getDouble("lng");
                            runOnUiThread(runnable);
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }

            });

        } else {
            Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show();
        }
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "a9c399acb6d543e0f5c23df50089b367";

        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + latitude + "," + longitude;


        if(isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

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
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            mForecast = getForecast(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });

                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
        mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it will be ");
        mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        Drawable drawable = ContextCompat.getDrawable(this, mCurrentWeather.getIconId());
        //Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);

        for (int index = 0; index < mForecastBox.getChildCount(); index++) {
            LinearLayout hour = (LinearLayout) mForecastBox.getChildAt(index);
            TextView time = (TextView) hour.getChildAt(0);
            time.setText(mForecast.get(index).getShortTime());
            ImageView icon = (ImageView) hour.getChildAt(1);
            Drawable forecastDrawable = ContextCompat.getDrawable(this, mForecast.get(index).getIconId());
            icon.setImageDrawable(forecastDrawable);
            TextView temp = (TextView) hour.getChildAt(2);
            temp.setText(mForecast.get(index).getTemperature() + "");
        }
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        return currentWeather;
    }

    private ArrayList<CurrentWeather> getForecast(String jsonData) throws JSONException {
//        JSONArray forecast = new JSONObject(jsonData)
//                .getJSONObject("hourly")
//                .getJSONArray("data");

        JSONObject bigForecast = new JSONObject(jsonData);
        String timezone = bigForecast.getString("timezone");
        JSONObject hourly = bigForecast.getJSONObject("hourly");
        JSONArray forecast = hourly.getJSONArray("data");

        ArrayList<CurrentWeather> weatherForecast = new ArrayList<CurrentWeather>();

        for (int index = 1; index <= 5; index++) {

            JSONObject nextHourJSON = forecast.getJSONObject(index);
            CurrentWeather nextHour = new CurrentWeather();

            nextHour.setTimeZone(timezone);
            nextHour.setTime(nextHourJSON.getLong("time"));
            nextHour.setIcon(nextHourJSON.getString("icon"));
            nextHour.setTemperature(nextHourJSON.getDouble("temperature"));

            weatherForecast.add(nextHour);
        }

        return weatherForecast;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }


}
