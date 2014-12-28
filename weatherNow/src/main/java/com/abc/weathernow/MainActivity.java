package com.abc.weathernow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
    private final String TAG = "WeatherNow";
	Typeface weatherFont;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    RecyclerView recList;
	private TextView weatherIcon;
	private TextView temp;
	private TextView windSpeed;
	private TextView cityName;
    private TextView weatherStatus;
    private LinearLayout headerProgress;
    private AlertDialog.Builder alertDialog;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList = (RecyclerView) findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        recList.setLayoutManager(llm);

        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        headerProgress = (LinearLayout) findViewById(R.id.headerProgress);
        weatherIcon = (TextView) findViewById(R.id.weather_icon);
        temp = (TextView) findViewById(R.id.info_text);
        windSpeed = (TextView) findViewById(R.id.wind_speed);
        cityName = (TextView) findViewById(R.id.city_name);
        weatherStatus = (TextView) findViewById(R.id.weather_status);
        weatherIcon.setTypeface(weatherFont);

        headerProgress.setVisibility(View.VISIBLE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mLocationRequest.setInterval(1000); // Update location every second

        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        updateUI(loc);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        updateUI(location);
    }
	
	private void updateUI(Location loc){
        if(isConnected())
        {
            String city = getCityName(loc);
            if(city != "")
            {
                JSONWeatherTask task = new JSONWeatherTask();
                task.execute(new String[]{city});
                JSONForecastTask forecastTask = new JSONForecastTask();
                forecastTask.execute(new String[]{city});
            }
        }
        else{
            showAlertDialog();
        }
	}

    private boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

	private String setWeatherIcon(int actualId, long sunrise, long sunset){
	    int id = actualId / 100;
	    String icon = "";
	    if(actualId == 800){
	        long currentTime = new Date().getTime();
	        if(currentTime>=sunrise && currentTime<sunset) {
	            icon = getResources().getString(R.string.weather_sunny);
	        } else {
	            icon = getResources().getString(R.string.weather_clear_night);
	        }
	    } else {
	        switch(id) {
	        case 2 : icon = getResources().getString(R.string.weather_thunder);
	                 break;         
	        case 3 : icon = getResources().getString(R.string.weather_drizzle);
	                 break;     
	        case 7 : icon = getResources().getString(R.string.weather_foggy);
	                 break;
	        case 8 : icon = getResources().getString(R.string.weather_cloudy);
	                 break;
	        case 6 : icon = getResources().getString(R.string.weather_snowy);
	                 break;
	        case 5 : icon = getResources().getString(R.string.weather_rainy);
	                 break;
	        }
	    }
	    return icon;
	}

    private String getCityName(Location location)
    {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressString = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addressList.size() > 0) {
                Address address = addressList.get(0);
                addressString = address.getLocality().toString();
                Toast.makeText(this, " Your Location is " + addressString, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            showAlertDialog();
            e.printStackTrace();
        }
        return addressString;
    }

    private String getDay(long timestamp){
        return new SimpleDateFormat("EEEE").format(new Date(timestamp * 1000));
    }

    private int getRoundedValue(float num)
    {
        return (int)Math.round(num - 273.15);
    }

    private void showAlertDialog()
    {
        if( alertDialog != null ) return;
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Oops!");
        alertDialog.setMessage("Unable to connect. Please try again later.");
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }
    
    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {
		
		@Override
		protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
			try {
                String data = ( (new WeatherHttpClient()).getWeatherData(params[0]));
				weather = JSONWeatherParser.getWeather(data);
			} catch (Exception e) {
                showAlertDialog();
				e.printStackTrace();
			}
			return weather;
		
	}
		
	@Override
		protected void onPostExecute(Weather weather) {
			super.onPostExecute(weather);
        try{
            temp.setText("" + getRoundedValue(weather.temperature.getTemp()) + "\u00b0 C ("+
                    getRoundedValue(weather.temperature.getMaxTemp()) + "°/" + getRoundedValue(weather.temperature.getMinTemp()) + "°)");
            windSpeed.setText("Wind "+weather.wind.getSpeed()+"mph" + "/Precip. " + weather.clouds.getPerc() +"%");
            cityName.setText(weather.location.getCity()+", "+weather.location.getCountry());
            weatherStatus.setText(weather.currentCondition.getCondition());
            weatherIcon.setText(setWeatherIcon(weather.currentCondition.getWeatherId(), weather.location.getSunrise(), weather.location.getSunset()));
        }
        catch(Exception e){
            Log.i("MainActivity", "Caught Exception " + e);
        }

		}
  }

    private class JSONForecastTask extends AsyncTask<String, Void, List<Forecast>> {

        @Override
        protected List<Forecast> doInBackground(String... params) {
            List<Forecast> forecastList = new ArrayList<Forecast>();
            try {
                String forecast = ( (new WeatherHttpClient()).getForecast(params[0]));
                forecastList = JSONWeatherParser.getForecastData(forecast);
            } catch (Exception e) {
                showAlertDialog();
                e.printStackTrace();
            }
            return forecastList;

        }

        @Override
        protected void onPostExecute(List<Forecast> forecastList) {
            super.onPostExecute(forecastList);
            try{
                List<WeatherInfo> result = new ArrayList<WeatherInfo>();
                for (int i=0; i < forecastList.size(); i++) {
                    WeatherInfo ci = new WeatherInfo();
                    ci.weatherIcon = setWeatherIcon(forecastList.get(i).weather.currentCondition.getWeatherId(), 0, 0);
                    ci.infoText = getRoundedValue(forecastList.get(i).weather.temperature.getMaxTemp()) + "°/" + getRoundedValue(forecastList.get(i).weather.temperature.getMinTemp()) + "°";
                    ci.windSpeed =forecastList.get(i).weather.wind.getSpeed()+"mph";
                    ci.status = forecastList.get(i).weather.currentCondition.getCondition();
                    ci.day = getDay(forecastList.get(i).date);
                    result.add(ci);

                }
                InfoCardAdapter ca = new InfoCardAdapter(result,weatherFont);
                recList.setAdapter(ca);
                headerProgress.setVisibility(View.GONE);
            }
            catch(Exception e){
                Log.i("MainActivity", "Caught Exception " + e);
            }

        }

    }
}
