package com.example.andrew.quicknotes;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    boolean headsOn = false;
    final AppCompatActivity thisView = this;
    File notesDirectory;
    int numberOfNotes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            //boolean headsOn = false;
            public void onClick(View v){
                if( !headsOn) {
                    startService(new Intent(thisView, NoteHead.class));
                    headsOn = !headsOn;
                    return;
                }else{
                    stopService(new Intent(thisView, NoteHead.class));
                    headsOn = !headsOn;
                    return;
                }
            }
        });

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notesToggleReceiver,
                        new IntentFilter("toggleHeads"));

        LinearLayout linlayout = (LinearLayout) findViewById(R.id.notelist);

        notesDirectory = getApplicationContext().getFilesDir();
        Log.i("w", "hi");
        File[] notes = notesDirectory.listFiles();
        int fcount = 0;
        for(File f: notes){
            fcount++;
            String name = f.getName();
            if(name.substring(0,1).compareTo("n")!=0) continue;
            CardView cv = new CardView(this);
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(700, 700);
            numberOfNotes++;
            linlayout.addView(cv, lps);
        }

        if(fcount <= 2){
            //no notes are in the directory, so put a default in
            String fname = "note_0";
            File file = new File(notesDirectory, fname);
            String defaultText = "new note.";
            FileOutputStream os;
            try{
                os = openFileOutput(fname, Context.MODE_PRIVATE);
                os.write(defaultText.getBytes());
                os.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }


    }



    // Handling the received Intents for the "toggle heads" event
    private BroadcastReceiver notesToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            headsOn = !headsOn;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(headsOn){
            stopService(new Intent(thisView, NoteHead.class));
        }
    }



}

