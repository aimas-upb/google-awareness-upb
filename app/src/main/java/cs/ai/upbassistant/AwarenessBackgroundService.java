package cs.ai.upbassistant;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.JsonWriter;
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
import com.google.android.gms.location.places.Places;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class AwarenessBackgroundService extends IntentService {

    private GoogleApiClient googleApiClient;
    WifiManager wifi;
    private final String TAG = "TAGAwareness";
    public static Object objHeadphone = new Object();
    public static Object objWeather = new Object();
    public static Object objLocation = new Object();
    public static Object objActivity = new Object();
    public static Object objPlaces = new Object();

    public static boolean headphoneResponse;
    public static boolean headphoneState;

    public static boolean weatherResponse;
    public static int[] conditions;
    public static float humidity;
    public static boolean celsius;
    public static float dewPoint;
    public static float temperature;
    public static float feelsLike;

    public static boolean locationResponse;
    public static Location currentLocation;

    public static boolean activityResponse;
    public static List<DetectedActivity> activities;
    public static DetectedActivity mostProbableActivity;

    public static boolean placesResponse;
    public static List<PlaceLikelihood> places;

    public static boolean wifiON;
    public static List<ScanResult> wifis;


    private void resetResponses() {
        headphoneResponse = false;
        weatherResponse = false;
        locationResponse = false;
        activityResponse = false;
        placesResponse = false;
    }



    public AwarenessBackgroundService() {
        super("AwarenessBackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        resetResponses();
        setupGoogleApiClient();

        writeHeadphoneState();
        writeWeather();
        writeLocation();
        writeActivity();
        writePlaces();
        googleApiClient.disconnect();

        writeWifi();
        writeToFile();
    }

    private void setupGoogleApiClient() {
        Context context = this.getApplicationContext();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        googleApiClient.connect();
    }

    private void writeToFile() {
        if (AlarmService.filename != null) {
            Context context = this.getApplicationContext();
            try {
                File dir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        "UPB_Awareness");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                if (AlarmService.filename == null)
                    return;

                File json_file = new File(dir, AlarmService.filename);
                FileWriter out = new FileWriter(json_file, true);
                JsonWriter json = new JsonWriter(out);
                json.setIndent("    ");

                json.beginObject();
                String value;

                //timestamp Unix Epoch
                long timestamp = System.currentTimeMillis() / 1000L;
                json.name("timestamp_unix").value(timestamp);

                //timestamp UTC
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                value = format.format(new Date());
                json.name("timestamp_utc").value(value);

                // Headphone Status
                if (AwarenessBackgroundService.headphoneResponse) {
                    json.name("headphones").value(AwarenessBackgroundService.headphoneState);
                } else
                    json.name("headphones").nullValue();

                // Weather Status
                if (!AwarenessBackgroundService.weatherResponse)
                    json.name("weather").nullValue();
                else {
                    json.name("weather");
                    json.beginObject();

                    value = new String("FAHRENHEIT");
                    if (AwarenessBackgroundService.celsius) {
                        value = new String("CELSIUS");
                    }
                    json.name("scale").value(value);
                    json.name("humidity").value(AwarenessBackgroundService.humidity);
                    json.name("dew_point").value(AwarenessBackgroundService.dewPoint);
                    json.name("temperature").value(AwarenessBackgroundService.temperature);
                    json.name("feels_like").value(AwarenessBackgroundService.feelsLike);
                    json.name("conditions_size").value(AwarenessBackgroundService.conditions.length);
                    json.name("conditions");
                    json.beginArray();
                    for (int i = 0; i < AwarenessBackgroundService.conditions.length; i++) {
                        json.value(conditionToString(AwarenessBackgroundService.conditions[i]));
                    }
                    json.endArray();
                    json.endObject();
                }

                // Location
                if (!AwarenessBackgroundService.locationResponse)
                    json.name("location").nullValue();
                else {
                    json.name("location");
                    json.beginObject();
                    json.name("latitude").value(AwarenessBackgroundService
                            .currentLocation.getLatitude());
                    json.name("longitude").value(AwarenessBackgroundService
                            .currentLocation.getLongitude());
                    json.name("altitude").value(AwarenessBackgroundService
                            .currentLocation.getAltitude());
                    json.name("data_accuracy").value(AwarenessBackgroundService
                            .currentLocation.getAccuracy());
                    json.endObject();
                }

                // Activities
                if (AwarenessBackgroundService.activityResponse == false) {
                    json.name("activities").nullValue();
                } else {
                    json.name("activities");
                    json.beginArray();
                    for (int i = 0; i < AwarenessBackgroundService.activities.size(); i++) {
                        DetectedActivity act = AwarenessBackgroundService.activities.get(i);
                        String name = activityToString(act.getType());
                        if (name == null)
                            continue;
                        json.beginObject();
                        Log.e(TAG, "Nume = "+name);
                        json.name("activity_name").value(name);
                        json.name("activity_confidence").value(act.getConfidence());
                        json.endObject();
                    }
                    json.endArray();
                }

                // Places
                if (AwarenessBackgroundService.placesResponse == false) {
                    json.name("number_of_places").value(0);
                    json.name("places").nullValue();
                } else {
                    json.name("number_of_places").value(AwarenessBackgroundService.places.size());
                    json.name("places");
                    json.beginArray();
                    for ( int i = 0; i < AwarenessBackgroundService.places.size(); i ++) {

                        json.beginObject();
                        PlaceLikelihood place = AwarenessBackgroundService.places.get(i);
                        json.name("place_name").value(place.getPlace().getName().toString());
                        json.name("place_likelihood").value(place.getLikelihood());
                        json.name("place_latitude").value(place.getPlace().getLatLng().latitude);
                        json.name("place_longitude").value(place.getPlace().getLatLng().longitude);
                        Location place_location = new Location((String)place.getPlace().getName());
                        place_location.setLongitude(place.getPlace().getLatLng().longitude);
                        place_location.setLatitude(place.getPlace().getLatLng().latitude);
                        float dist = AwarenessBackgroundService.currentLocation
                                .distanceTo(place_location);
                        json.name("distance_from_user_meters").value(dist);
                        json.endObject();
                    }
                    json.endArray();

                }

                // Wifi
                if (AwarenessBackgroundService.wifiON == false) {
                    json.name("wifi_ON").value(false);
                    json.name("wifi_networks").nullValue();
                } else {
                    json.name("wifi_ON").value(true);
                    json.name("wifi_networks");
                    json.beginArray();
                    for (int i = 0; i < AwarenessBackgroundService.wifis.size(); i++) {
                        ScanResult wifiNetwork = AwarenessBackgroundService.wifis.get(i);
                        json.beginObject();
                        int signal_power = wifi.calculateSignalLevel(wifiNetwork.level, 10);
                        json.name("SSID").value(wifiNetwork.SSID);
                        json.name("signal_power").value(signal_power);
                        json.endObject();
                    }
                    json.endArray();

                }

                json.endObject();
                json.close();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeWifi() {
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wifi.isWifiEnabled()) {
            AwarenessBackgroundService.wifiON = true;
            AwarenessBackgroundService.wifis = wifi.getScanResults();
        } else {
            AwarenessBackgroundService.wifiON = false;
        }
    }

    private void writePlaces() {
        Awareness.SnapshotApi.getPlaces(googleApiClient)
                .setResultCallback(new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        if(placesResult.getStatus().isSuccess()) {
                            AwarenessBackgroundService.places = placesResult.getPlaceLikelihoods();
                        }

                        synchronized (AwarenessBackgroundService.objPlaces) {
                            AwarenessBackgroundService.placesResponse = true;
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

                            AwarenessBackgroundService.activities =
                                    activity.getProbableActivities();
                            AwarenessBackgroundService.mostProbableActivity = activity
                                    .getMostProbableActivity();
                        }

                        synchronized (AwarenessBackgroundService.objActivity) {
                            AwarenessBackgroundService.activityResponse = true;
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
                            AwarenessBackgroundService.currentLocation = locationResult.getLocation();
                        }

                        synchronized (AwarenessBackgroundService.objLocation) {
                            AwarenessBackgroundService.locationResponse = true;
                            AwarenessBackgroundService.objLocation.notifyAll();
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
                if (headphoneStateResult.getStatus().isSuccess()) {

                    HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                    AwarenessBackgroundService.headphoneState = false;
                    if (headphoneState.getState() == HeadphoneState.PLUGGED_IN) {

                        AwarenessBackgroundService.headphoneState = true;
                    }
                }

                synchronized (AwarenessBackgroundService.objHeadphone) {
                    AwarenessBackgroundService.headphoneResponse = true;
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

                            AwarenessBackgroundService.conditions = weather.getConditions();
                            AwarenessBackgroundService.humidity = weather.getHumidity();
                            AwarenessBackgroundService.dewPoint = weather.getDewPoint(Weather.CELSIUS);
                            AwarenessBackgroundService.celsius = true;
                            AwarenessBackgroundService.temperature = weather.getTemperature(Weather.CELSIUS);
                            AwarenessBackgroundService.feelsLike = weather.getFeelsLikeTemperature(Weather.CELSIUS);
                        }
                        synchronized (AwarenessBackgroundService.objWeather) {
                            AwarenessBackgroundService.weatherResponse = true;
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
