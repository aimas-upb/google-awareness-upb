package cs.ai.upbassistant;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.StringBuilderPrinter;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;


public class AwarenessBackgroundService extends IntentService {

    private GoogleApiClient googleApiClient;
    private final String TAG = "TAGAwareness";
    public static Object objHeadphone = new Object();
    public static Object objWeather = new Object();
    public static Object objLocation = new Object();
    public AwarenessBackgroundService() {
        super("AwarenessBackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        setupGoogleApiClient();
        writeHeadphoneState();
        writeWeather();
        writeLocation();
    }

    public void setupGoogleApiClient() {
        Context context = this.getApplicationContext();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        googleApiClient.connect();
    }

    public void writeLocation() {
        Awareness.SnapshotApi.getLocation(googleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (locationResult.getStatus().isSuccess()) {
                            Location location = locationResult.getLocation();

                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            double altitude = location.getAltitude();
                            float accuracy = location.getAccuracy();

                            Log.e(TAG, "The Latitude is " + latitude);
                            Log.e(TAG, "And The Longitude is " + longitude);
                            Log.e(TAG, "And The Altitude is " + altitude);
                            Log.e(TAG, "And The Accuracy is " + accuracy);

                            synchronized (AwarenessBackgroundService.objLocation) {
                                AwarenessBackgroundService.objLocation.notifyAll();
                            }

                        }

                    }
                });
        synchronized (this.objLocation) {
            try {
                this.objLocation.wait();
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    public void writeHeadphoneState() {
        PendingResult<HeadphoneStateResult> result1 = Awareness.SnapshotApi
                .getHeadphoneState(googleApiClient);
        result1.setResultCallback(new ResultCallback<HeadphoneStateResult>() {
            @Override
            public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                if (!headphoneStateResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Could not get headphone state.");
                    return;
                }
                HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {
                    Log.i(TAG, "Headphones are plugged in.\n");
                }
                else {
                    Log.i(TAG, "Headphones are NOT plugged in.\n");
                }
                synchronized (AwarenessBackgroundService.objHeadphone) {
                    AwarenessBackgroundService.objHeadphone.notifyAll();
                }
            }
        });
        synchronized (this.objHeadphone) {
            try {
                this.objHeadphone.wait();
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    public void writeWeather() {

        Awareness.SnapshotApi.getWeather(googleApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if (weatherResult.getStatus().isSuccess()) {
                            Weather weather = weatherResult.getWeather();

                            int[] conditions = weather.getConditions();
                            StringBuilder stringBuilder = new StringBuilder();
                            if (conditions.length > 0) {
                                for (int i = 0; i < conditions.length; i++) {
                                    if (i > 0)
                                        stringBuilder.append(", ");
                                    stringBuilder.append(conditionToString(conditions[i]));
                                }
                                Log.e(TAG, "The weather is = "+stringBuilder);
                            }

                            float humidity = weather.getHumidity();
                            Log.e(TAG, "The Humidity is = " + humidity);

                            float dewPoint = weather.getDewPoint(Weather.CELSIUS);
                            Log.e(TAG, "And the dew point is "+ dewPoint + " degree Celsius");

                            float temp = weather.getTemperature(Weather.CELSIUS);
                            Log.e(TAG, "The temperature is = " + temp + " degree Celsius");

                            float feelsLike = weather.getFeelsLikeTemperature(Weather.CELSIUS);
                            Log.e(TAG, "And it Feels Like = " + feelsLike + " degree Celsius");
                        }
                        synchronized (AwarenessBackgroundService.objWeather) {
                            AwarenessBackgroundService.objWeather.notifyAll();
                        }
                    }
                });
        synchronized (this.objWeather) {
            try {
                this.objWeather.wait();
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    private String conditionToString(int condition) {
        switch (condition) {
            case Weather.CONDITION_CLEAR:
                return new String("CONDITION_CLEAR");
            case Weather.CONDITION_CLOUDY:
                return new String("CONDITION_CLOUDY");
            case Weather.CONDITION_FOGGY:
                return new String("CONDITION_FOGGY");
            case Weather.CONDITION_HAZY:
                return new String("CONDITION_HAZY");
            case Weather.CONDITION_ICY:
                return new String("CONDITION_ICY");
            case Weather.CONDITION_RAINY:
                return new String("CONDITION_RAINY");
            case Weather.CONDITION_SNOWY:
                return new String("CONDITION_SNOWY");
            case Weather.CONDITION_STORMY:
                return new String("CONDITION_STORMY");
            case Weather.CONDITION_WINDY:
                return new String("CONDITION_WINDY");
            default:
                return new String("CONDITION_UNKNOWN");
        }
    }

}
