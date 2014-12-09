package com.abc.weathernow;

import java.util.Date;

import org.json.JSONException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.location.Location;
import android.util.Log;


public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
	protected Context context;
	private TextView weatherIcon;
	private TextView temp;
	private TextView condition;
	private TextView cityName;
    private TextView sunriseTime;
    private TextView sunsetTime;
	Typeface weatherFont;

    private final String TAG = "MyAwesomeApp";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        
        weatherIcon = (TextView) findViewById(R.id.weather_icon);
        temp = (TextView) findViewById(R.id.info_text);
        condition = (TextView) findViewById(R.id.condition);
        cityName = (TextView) findViewById(R.id.city_name);
        
        weatherIcon.setTypeface(weatherFont);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
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
		String city = String.valueOf(loc.getLatitude()) + "@" + String.valueOf(loc.getLongitude());
        JSONWeatherTask task = new JSONWeatherTask();
		task.execute(new String[]{city});
	}
	
	private void setWeatherIcon(int actualId, long sunrise, long sunset){
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
	    weatherIcon.setText(icon);
	}
    
private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {
		
		@Override
		protected Weather doInBackground(String... params) {
			Weather weather = new Weather();
			String data = ( (new WeatherHttpClient()).getWeatherData(params[0]));

			try {
				weather = JSONWeatherParser.getWeather(data);
				
			} catch (JSONException e) {				
				e.printStackTrace();
			}
			return weather;
		
	}
		
	@Override
		protected void onPostExecute(Weather weather) {			
			super.onPostExecute(weather);
			temp.setText("" + Math.round((weather.temperature.getTemp() - 273.15)) + "\u00b0 C"+"   ("+weather.currentCondition.getCondition()+")");
			condition.setText(weather.wind.getSpeed()+" mph");
			cityName.setText(weather.location.getCity()+", "+weather.location.getCountry());
			setWeatherIcon(weather.currentCondition.getWeatherId(),weather.location.getSunrise(),weather.location.getSunset());
		}
	
  }
}
