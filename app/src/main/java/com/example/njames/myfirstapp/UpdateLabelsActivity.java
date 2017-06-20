package com.example.njames.myfirstapp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;

public class UpdateLabelsActivity extends AppCompatActivity implements
        View.OnClickListener{

    private static final String TAG = "UpdateLabelsActivity";
    private String thingName = "UpdateLabelsActivity";
    ArrayList<View> views = new ArrayList<View>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_labels);
        findViewById(R.id.update_button).setOnClickListener(this);
        thingName = this.getIntent().getStringExtra(Constants.EXTRA_THING_ID);
        ArrayList<String> td = this.getIntent().getStringArrayListExtra(Constants.EXTRA_LABELS);
        ArrayList<String> tu = this.getIntent().getStringArrayListExtra(Constants.EXTRA_THRESHOLDS_UPPER);
        ArrayList<String> tl = this.getIntent().getStringArrayListExtra(Constants.EXTRA_THRESHOLDS_LOWER);
        views.add(findViewById(R.id.t0));
        views.add(findViewById(R.id.t1));
        views.add(findViewById(R.id.t2));

        int i=0;
        for (View view: views) {
            ((EditText)view.findViewById(R.id.td)).setText(td.get(i));
            ((EditText)view.findViewById(R.id.tu)).setText(tu.get(i));
            ((EditText)view.findViewById(R.id.tl)).setText(tl.get(i));
            i++;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.update_button:
                Constants.hideKeyboard(this);
                updateLabels();
                break;
        }
    }
    private void updateLabels() {
        Log.i(TAG, "Updating labels: "+thingName);
        //CognitoCachingCredentialsProvider credentialsProvider = FirebaseIntentService.getCredentialProvider(getApplicationContext());
        RequestClass request = new RequestClass(thingName, FirebaseInstanceId.getInstance().getToken());
        for (View view : views) {
            String ts = ((EditText)view.findViewById(R.id.tu)).getText().toString();
            if (ts.isEmpty()) { ts = "1000"; }
            Integer tu = Integer.parseInt(ts);
            ts = ((EditText)view.findViewById(R.id.tl)).getText().toString();
            if (ts.isEmpty()) { ts = "0"; }
            Integer tl = Integer.parseInt(ts);
            String td = ((EditText)view.findViewById(R.id.td)).getText().toString();
            request.add(td,tu,tl);
        }
        /*
        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.US_WEST_2, credentialsProvider);
        final UpdateThingDescInterface myInterface = factory.build(UpdateThingDescInterface.class);

        new AsyncTask<RequestClass, Void, Void>() {
            @Override
            protected Void doInBackground(RequestClass... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    myInterface.UpdateThingDesc(params[0]);
                    return null;
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke echo", lfe);
                    return null;
                }
            }
        }.execute(request);
        */
    }
}
