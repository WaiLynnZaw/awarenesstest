package com.wlz.awarenesstest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mApiClient;

    // The fence key is how callback code determines which fence fired.
    private final String FENCE_KEY = "fence_key";

    private final String TAG = getClass().getSimpleName();

    private PendingIntent mPendingIntent;

    private FenceReceiver mFenceReceiver;

    private LogFragment mLogFragment;

    // The intent action which will be fired when your fence is triggered.
    private final String FENCE_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";


    private static final int MY_PERMISSION_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("CLICKED", "TRUE");
                printSnapshot();
            }
        });

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Awareness.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();


        mLogFragment =
                (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);
    }

    @Override
    protected void onStop() {
        if (mFenceReceiver != null) {
            unregisterReceiver(mFenceReceiver);
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        // Unregister the fence:
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .removeFence(FENCE_KEY)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully unregistered.");
                        } else {
                            Log.e(TAG, "Fence could not be unregistered: " + status);
                        }
                    }
                });
        super.onPause();
    }

    /**
     * Uses the snapshot API to print out some contextual information the device is "aware" of.
     */
    private void printSnapshot() {
        // Clear the console screen of previous snapshot / fence log data
//        mLogFragment.getLogView().setText("");

        // Each type of contextual information in the snapshot API has a corresponding "get" method.
        //  For instance, this is how to get the user's current Activity.
        Awareness.SnapshotApi.getDetectedActivity(mApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult dar) {
                        ActivityRecognitionResult arr = dar.getActivityRecognitionResult();

                        // getMostProbableActivity() is good enough for basic Activity detection.
                        // To work within a threshold of confidence,
                        // use ActivityRecognitionResult.getProbableActivities() to get a list of
                        // potential current activities, and check the confidence of each one.
                        DetectedActivity probableActivity = arr.getMostProbableActivity();

                        // Confidence is an int between 0 and 100.
                        int confidence = probableActivity.getConfidence();
                        String activityStr = probableActivity.toString();
                        mLogFragment.getLogView().println("Activity: " + activityStr
                                + ", Confidence: " + confidence + "/100");

                        Log.e("RES>>>>", "Activity: " + activityStr
                                + ", Confidence: " + confidence + "/100");
                    }
                });

        // Pulling headphone state is similar, but doesn't involve analyzing confidence.
        Awareness.SnapshotApi.getHeadphoneState(mApiClient)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                        boolean pluggedIn = headphoneState.getState() == HeadphoneState.PLUGGED_IN;
                        String stateStr =
                                "Headphones are " + (pluggedIn ? "plugged in" : "unplugged");
                        mLogFragment.getLogView().println(stateStr);
                    }
                });


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        Awareness.SnapshotApi.getPlaces(mApiClient)
//                .setResultCallback(new ResultCallback<PlacesResult>() {
//                    @Override
//                    public void onResult(@NonNull PlacesResult placesResult) {
//                        for (int i =0; i< placesResult.getPlaceLikelihoods().size();i++){
//                            Log.e("Results>>>",placesResult.getPlaceLikelihoods().get(i).getPlace().getName().toString());
//                        }
//                    }
//                });
        Awareness.SnapshotApi.getLocation(mApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        Log.e("Result>>",locationResult.getLocation().toString());

                    }
                });

        // Some of the data available via Snapshot API requires permissions that must be checked
        // at runtime.  Weather snapshots are a good example of this.  Since weather is protected
        // by a runtime permission, and permission request callbacks will happen asynchronously,
        // the easiest thing to do is put weather snapshot code in its own method.  That way it
        // can be called from here when permission has already been granted on subsequent runs,
        // and from the permission request callback code when permission is first granted.
        if (checkAndRequestWeatherPermissions()) {
            getWeatherSnapshot();
        }
    }

    /** Helper method to retrieve weather data using the Snapshot API.  Since Weather is protected
     * by a runtime permission, this snapshot code is going to be called in multiple places:
     * {@link #printSnapshot()} when the permission has already been accepted, and
     * {@link #onRequestPermissionsResult(int, String[], int[])} when the permission is requested
     * and has been granted.
     */
    private void getWeatherSnapshot() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Awareness.SnapshotApi.getWeather(mApiClient)
                    .setResultCallback(new ResultCallback<WeatherResult>() {
                        @Override
                        public void onResult(@NonNull WeatherResult weatherResult) {
                            if (!weatherResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Could not get weather.");
                                return;
                            }
                            Weather weather = weatherResult.getWeather();
                            weather.getConditions();
                            mLogFragment.getLogView().println("Weather: " + weather);
                        }
                    });
        }

    }

    /**
     * Helper method to handle requesting the runtime permissions required for weather snapshots.
     * @return true if the permission has already been granted, false otherwise.
     */
    private boolean checkAndRequestWeatherPermissions() {
        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_LOCATION
                );
            } else {
                Log.i(TAG, "Permission previously denied and app shouldn't ask again.  Skipping" +
                        " weather snapshot.");
            }
            return false;

        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getWeatherSnapshot();
                } else {
                    Log.i(TAG, "Location permission denied.  Weather snapshot skipped.");
                }
            }
        }
    }

    /**
     * Sets up {@link AwarenessFence}'s for the sample app, and registers callbacks for them
     * with a custom {@link BroadcastReceiver}
     */
    private void setupFences() {
        // DetectedActivityFence will fire when it detects the user performing the specified
        // activity.  In this case it's walking.
        AwarenessFence walkingFence = DetectedActivityFence.during(DetectedActivityFence.WALKING);

        // There are lots of cases where it's handy for the device to know if headphones have been
        // plugged in or unplugged.  For instance, if a music app detected your headphones fell out
        // when you were in a library, it'd be pretty considerate of the app to pause itself before
        // the user got in trouble.
        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

        // Combines multiple fences into a compound fence.  While the first two fences trigger
        // individually, this fence will only trigger its callback when all of its member fences
        // hit a true state.
        AwarenessFence walkingWithHeadphones = AwarenessFence.and(walkingFence, headphoneFence);

        // We can even nest compound fences.  Using both "and" and "or" compound fences, this
        // compound fence will determine when the user has headphones in and is engaging in at least
        // one form of exercise.
        // The below breaks down to "(headphones plugged in) AND (walking OR running OR bicycling)"
        AwarenessFence exercisingWithHeadphonesFence = AwarenessFence.and(
                headphoneFence,
                AwarenessFence.or(
                        walkingFence,
                        DetectedActivityFence.during(DetectedActivityFence.RUNNING),
                        DetectedActivityFence.during(DetectedActivityFence.ON_BICYCLE)));


        // Now that we have an interesting, complex condition, register the fence to receive
        // callbacks.

        // Register the fence to receive callbacks.
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence(FENCE_KEY, headphoneFence, mPendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully registered.");
                        } else {
                            Log.e(TAG, "Fence could not be registered: " + status);
                        }
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("Connected","true");
        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        mPendingIntent =
                PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        // The broadcast receiver that will receive intents when a fence is triggered.
        mFenceReceiver = new FenceReceiver();
        registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
        setupFences();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("OnSuspended","reach");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("OnConnectionFail","Fail");
        Log.e("OnConnectionFailed",connectionResult.getErrorMessage());
    }

    /**
     * A basic BroadcastReceiver to handle intents from from the Awareness API.
     */
    public class FenceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                mLogFragment.getLogView()
                        .println("Received an unsupported action in FenceReceiver: action="
                                + intent.getAction());
                return;
            }

            // The state information for the given fence is em
            FenceState fenceState = FenceState.extract(intent);

            if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
                String fenceStateStr;
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "true";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "false";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
                mLogFragment.getLogView().println("Fence state: " + fenceStateStr);
            }
        }
    }
}
