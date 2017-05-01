package cs.ai.upbassistant;

import android.animation.ObjectAnimator;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;


public class AwarenessBackgroundService extends IntentService {

    private GoogleApiClient googleApiClient;
    private final String TAG = "AwarenessBackgroundService";
    public static Object objHeadphone = new Object();
    public AwarenessBackgroundService() {
        super("AwarenessBackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        setupGoogleApiClient();
        writeHeadphoneState();
    }

    public void setupGoogleApiClient() {
        Context context = this.getApplicationContext();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        googleApiClient.connect();
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


}
