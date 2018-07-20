package com.example.andrew.quicknotes;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    boolean headsOn = false;
    final AppCompatActivity thisView = this;
    File notesDirectory;
    String notesDirectoryName = "myQuickNotes";
    int numberOfNotes = 0;
    Context c = this;
    EditText[] notesContent = new EditText[5];
    CardView[] noteCards = new CardView[5];
    List<Pair<String, Integer>> noteIndexPairs = new ArrayList<Pair<String, Integer>>();


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Build.VERSION.SDK_INT >= 23){
        boolean candraw = Settings.canDrawOverlays(this);
        Log.i("canDraw", Boolean.toString(candraw));
        if(!candraw){
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            //boolean headsOn = false;
            public void onClick(View v){
                if( !headsOn) {
                    startService(new Intent(thisView, NoteHead.class));
                    headsOn = !headsOn;
                    //return;
                }else{
                    stopService(new Intent(thisView, NoteHead.class));
                    headsOn = !headsOn;
                    //return;
                }
            }
        });

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notesToggleReceiver,
                        new IntentFilter("toggleHeads"));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notesUpdateReceiver,
                        new IntentFilter("updateNotes"));

        LinearLayout linlayout =  findViewById(R.id.notelist);

        notesDirectory = new File(Environment.getExternalStorageDirectory(), notesDirectoryName);
        if(!notesDirectory.isDirectory()) notesDirectory.mkdirs();
        File[] notes = notesDirectory.listFiles();
        int fcount = 0;
        boolean firstFound = false;

        for(File f: notes){
            fcount++;
            String name = f.getName();
            Log.i("fname", name);
            firstFound = true;
            int index = findOpenSlot(noteCards);
            CardView cv = new CardView(this);
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(700, 700);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            String noteContents = getFileContents( f.getName());
            Log.i("noteName", f.getName());
            notesContent[index] = et;
            noteCards[index] = cv;
            noteIndexPairs.add(Pair.create(name, index));
            numberOfNotes++;
            Log.i("noteContents", noteContents);
            et.setText( noteContents, TextView.BufferType.NORMAL);
            cv.addView(et);
            linlayout.addView(cv, lps);
        }

        if(!firstFound){
            Log.i("new note", "new note");
            //no notes are in the directory, so put a default in
            String fname = "note_0.txt";
            int cardNum = 0;
            File mypath=new File(notesDirectory,fname);
            //File file = new File(notesDirectory, fname);
            String defaultText = "new note.";
            try{
                FileOutputStream os = new FileOutputStream(mypath);
                os = openFileOutput(fname, Context.MODE_PRIVATE);
                os.write(defaultText.getBytes());
                os.close();
            }catch(Exception e){
                e.printStackTrace();
            }

            CardView cv = new CardView(this);
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(400, 400);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            notesContent[cardNum] = et;
            noteCards[cardNum] = cv;
            noteIndexPairs.add(new Pair<String, Integer>(fname, cardNum));
            numberOfNotes++;
            et.setText( defaultText, TextView.BufferType.NORMAL);
            cv.addView(et);
            linlayout.addView(cv, lps);

        }



    }



    // Handling the received Intents for the "toggle heads" event
    private BroadcastReceiver notesToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            headsOn = !headsOn;
        }
    };

    private BroadcastReceiver notesUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String cardName = intent.getStringExtra("cardName");
            Log.i("RECIEVED-NAME", cardName);
            String text = getFileContents(cardName);
            notesContent[getAssociatedInt(cardName)].setText( text, TextView.BufferType.NORMAL);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(headsOn){
            stopService(new Intent(thisView, NoteHead.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(headsOn) {
            stopService(new Intent(thisView, NoteHead.class));
            headsOn = !headsOn;
            // Use broadcast here instead to signal finished I/O write
        }
        //String newdata = getFileContents("note_0");
        //notesContent[0].setText(newdata, TextView.BufferType.NORMAL);
    }


    public String getFileContents(String noteName){
        String note = "";
        File notesPath=new File(notesDirectory,noteName);
        try{
            FileInputStream is = new FileInputStream(notesPath);
            int val;
            is = openFileInput(noteName);
            while((val = is.read()) != -1){
                Log.i("new note val", Character.toString((char)val));
                note = note.concat(Character.toString((char)val));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return note;
    }

    public int findOpenSlot(CardView[] cv){
        int slot = -1;
        for(int i = 0; i < 5; i++){
            if(cv[i] == null){
                slot = i;
                break;
            }
        }
        return slot;
    }

    public int getAssociatedInt(String cardName){
        Iterator noteIterator = noteIndexPairs.iterator();
        int index = -1;
        while(noteIterator.hasNext()){
            Pair<String, Integer> noteIndex = (Pair<String, Integer>) noteIterator.next();
            Log.i("cname", noteIndex.first);
            Log.i("cname2", cardName);
            if(noteIndex.first.compareTo(cardName) == 0){
                index = noteIndex.second;
                break;
            }

        }
        return index;
    }

}

