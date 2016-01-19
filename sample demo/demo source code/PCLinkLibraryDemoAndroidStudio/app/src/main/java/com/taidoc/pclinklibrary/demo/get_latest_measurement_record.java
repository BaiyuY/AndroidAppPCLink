package com.taidoc.pclinklibrary.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class get_latest_measurement_record extends ActionBarActivity {

    Button bSET;
    EditText  etthreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_latest_measurement_record);



        etthreshold = (EditText) findViewById(R.id.thresholdTitle);
        bSET = (Button) findViewById(R.id.set);

    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.set:


                break;

        }
    }
}