package co.gaptap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by gareth on 7/20/13.
 */
public class GCMIntentService extends com.google.android.gcm.GCMBaseIntentService {

    public GCMIntentService()
    {
        super("260532533724");
    }

    @Override
    protected void onMessage(Context context, Intent intent) {

    }

    @Override
    protected void onError(Context context, String s) {

    }

    @Override
    protected void onRegistered(Context context, String s) {

    }

    @Override
    protected void onUnregistered(Context context, String s) {

    }
}
