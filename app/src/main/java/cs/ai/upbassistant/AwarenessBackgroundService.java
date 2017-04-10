package cs.ai.upbassistant;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class AwarenessBackgroundService extends IntentService {

    private final String TAG = "AwarenessBackgroundService";
    public AwarenessBackgroundService() {
        super("AwarenessBackgroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = this.getApplicationContext();
        for(Integer i = 1; i < 6; i ++ ) {
            try {
                Thread.sleep(1000);
                Log.d(TAG, i.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
