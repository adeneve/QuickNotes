package com.example.andrew.quicknotes;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by andrew on 2/27/2018.
 */

/*public class Overlay extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_overlay);
        EditText someEditText = (EditText) findViewById(R.id.noteText);
        someEditText.setFocusableInTouchMode(true);
        someEditText.requestFocus();
        InputMethodManager mgr =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(someEditText, InputMethodManager.SHOW_IMPLICIT);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onResume(){
        super.onResume();
    }
}*/
