package cs.ai.upbassistant;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;

import java.util.List;


public class AwarenessBackgroundService extends IntentService {

    private GoogleApiClient googleApiClient;
    WifiManager wifi;
    private final String TAG = "TAGAwareness";
    public static Object objHeadphone = new Object();
    public static Object objWeather = new Object();
    public static Object objLocation = new Object();
    public static Object objActivity = new Object();
    public static Object objPlaces = new Object();
    public static Location currentLocation;
    public AwarenessBackgroundService() {
        super("AwarenessBackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        setupGoogleApiClient();
        writeHeadphoneState();
        writeWeather();
        writeLocation();
        writeActivity();
        writePlaces();
        googleApiClient.disconnect();
        writeWifi();
    }

    private void setupGoogleApiClient() {
        Context context = this.getApplicationContext();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        googleApiClient.connect();
    }

    private void writeWifi() {
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wifi.isWifiEnabled()) {
            List<ScanResult> wifis = wifi.getScanResults();

            Log.e(TAG, "Size of wifis = " + wifis.size());
            for (int i = 0 ; i < wifis.size(); i ++) {
                ScanResult wifiNetwork = wifis.get(i);
                Log.e(TAG, "Wifi no."+i+" has name "+wifiNetwork.SSID+ " and level "+
                wifi.calculateSignalLevel(wifiNetwork.level, 10));

            }
        } else {

        }
    }


    private void writePlaces() {
        Awareness.SnapshotApi.getPlaces(googleApiClient)
                .setResultCallback(new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        if(placesResult.getStatus().isSuccess()) {
                            List<PlaceLikelihood> places = placesResult.getPlaceLikelihoods();
                            for (int i = 0; i < places.size(); i ++) {
                                PlaceLikelihood place = places.get(i);
                                float confidence = place.getLikelihood();
                                /*
                                if (confidence < 0.0001 )
                                    continue;
                                    */
                                String placeName = (String) place.getPlace().getName();
                                double lat = place.getPlace().getLatLng().latitude;
                                double longi = place.getPlace().getLatLng().longitude;


                                Location placeLocation = new Location(placeName);
                                placeLocation.setLatitude(lat);
                                placeLocation.setLongitude(longi);
                                float distance = AwarenessBackgroundService.currentLocation
                                        .distanceTo(placeLocation);

                                Log.e(TAG, "Place no."+i+" is " + placeName +
                                        " and confidence is " + confidence+
                                        " and Latitude is " + lat + " and Longitude is "+ longi
                                        + " and distance in meters is " + distance);
                            }
                        }



                        synchronized (AwarenessBackgroundService.objPlaces) {
                            AwarenessBackgroundService.objPlaces.notifyAll();
                        }
                    }
                });
        synchronized (this.objPlaces) {
            try {
                this.objPlaces.wait(3000);
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    private void writeActivity() {
        Awareness.SnapshotApi.getDetectedActivity(googleApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if (detectedActivityResult.getStatus().isSuccess()) {
                            ActivityRecognitionResult activity = detectedActivityResult
                                    .getActivityRecognitionResult();

                            List<DetectedActivity> activities =
                                    activity.getProbableActivities();
                            Log.e(TAG, "Size of activities = "+ activities.size());

                            for (int i = 0; i < activities.size(); i ++) {
                                DetectedActivity act = activities.get(i);
                                String actName = activityToString(act.getType());
                                if (actName == null)
                                    continue;
                                Log.e(TAG, "Activity no. "+i+" is " + actName
                                        + " and it has confidence " + act.getConfidence());

                            }

                            Log.e(TAG, "The most probable activity is " + activityToString(activity
                                    .getMostProbableActivity().getType()) +
                                    " and it has confidence " + activity.getMostProbableActivity()
                                    .getConfidence());
                        }

                        synchronized (AwarenessBackgroundService.objActivity) {
                            AwarenessBackgroundService.objActivity.notifyAll();
                        }
                    }
                });
        synchronized (this.objActivity) {
            try {
                this.objActivity.wait(3000);
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    private void writeLocation() {
        Awareness.SnapshotApi.getLocation(googleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (locationResult.getStatus().isSuccess()) {
                            Location location = locationResult.getLocation();
                            AwarenessBackgroundService.currentLocation = location;

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
                this.objLocation.wait(3000);
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    private void writeHeadphoneState() {
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
                    Log.e(TAG, "Headphones are plugged in.\n");
                }
                else {
                    Log.e(TAG, "Headphones are NOT plugged in.\n");
                }
                synchronized (AwarenessBackgroundService.objHeadphone) {
                    AwarenessBackgroundService.objHeadphone.notifyAll();
                }
            }
        });
        synchronized (this.objHeadphone) {
            try {
                this.objHeadphone.wait(3000);
            } catch (InterruptedException e) {
                Log.e(TAG,  "Interrupted exception " + e.getMessage());
            }
        }
    }

    private void writeWeather() {

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
                this.objWeather.wait(3000);
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
                return null;
        }
    }

    private String activityToString(int activity) {
        switch (activity) {
            case 0:
                return new String("IN_VEHICLE");
            case 1:
                return new String("ON_BICYCLE");
            case 2:
                return new String("ON_FOOT");
            case 8:
                return new String("RUNNING");
            case 3:
                return new String("STILL");
            case 5:
                return new String("TILTING");
            case 4:
                return new String("UNKNOWN");
            case 7:
                return new String("WALKING");
            default:
                return null;
        }
    }

}
