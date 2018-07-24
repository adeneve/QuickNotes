package com.example.andrew.quicknotes;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

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
    boolean floatysStarted = false;
    Context ctx = this;

    Intent intenty;
    ActionBar ab;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ab = getSupportActionBar();
        ab.setBackgroundDrawable(new ColorDrawable(Color.rgb(192, 239, 167)));

        if(Build.VERSION.SDK_INT >= 23){
        boolean candraw = Settings.canDrawOverlays(this);
        Log.i("canDraw", Boolean.toString(candraw));
        if(!candraw){
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatActivity thisV = thisView;


        FloatingActionButton fab = findViewById(R.id.fab);
        final ScrollView sv = findViewById(R.id.scroller);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newNoteName = "note_" + Integer.toString(numberOfNotes) + ".txt";
                boolean created = createNewNote(newNoteName);
                if(!created){
                    Toast.makeText(c, "max number of notes reached", Toast.LENGTH_SHORT).show();
                }else{
                    sv.scrollTo(0, sv.getBottom());
                }
            }
        });

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notesToggleReceiver,
                        new IntentFilter("toggleHeads"));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notesUpdateReceiver,
                        new IntentFilter("updateNotes"));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(floatsStartingReceiver,
                        new IntentFilter("floatysStarting"));

        notesDirectory = new File(Environment.getExternalStorageDirectory(), notesDirectoryName);
        if(!notesDirectory.isDirectory()) notesDirectory.mkdirs();
        File[] notes = notesDirectory.listFiles();
        int fcount = 0;
        boolean firstFound = false;
        final LinearLayout linlayout =  findViewById(R.id.notelist);
        linlayout.removeAllViewsInLayout();
        noteCards = new CardView[5];
        notesContent = new EditText[5];

        for(File f: notes){
            fcount++;
            String name = f.getName();
            Log.i("fname", name);
            firstFound = true;
            int index = findOpenSlot(noteCards);
            CardView cv = new CardView(this);
            int dp = 300;
            float height =  dp * this.getResources().getDisplayMetrics().density;
            int heightInt = (int) height;
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            String noteContents = getFileContents( f.getName());
            Log.i("noteName", f.getName());
            notesContent[index] = et;
            noteCards[index] = cv;
            noteIndexPairs.add(Pair.create(name, index));
            numberOfNotes++;
            Log.i("noteContents", noteContents);
            et.setFocusable(false);
            et.setText( noteContents, TextView.BufferType.NORMAL);
            cv.addView(et);
            linlayout.addView(cv, lps);
        }

        if(!firstFound){
            String newNoteName = "note_" + Integer.toString(numberOfNotes) + ".txt";
            createNewNote(newNoteName);
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
            if(cardName != null) {
                //Log.i("RECIEVED-NAME", cardName);
                String text = getFileContents(cardName);
                notesContent[getAssociatedInt(cardName)].setText(text, TextView.BufferType.NORMAL);
            }
        }
    };
    private BroadcastReceiver floatsStartingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            headsOn = !headsOn;
            startService(new Intent(thisView, NoteHead.class));
            moveTaskToBack(true);
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
        ab.setDisplayShowTitleEnabled(true);
        Log.i("headsOn", Boolean.toString(headsOn));
        if(headsOn) {
                stopService(new Intent(thisView, NoteHead.class));
                headsOn = !headsOn;
            // Use broadcast here instead to signal finished I/O write
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        intenty = new Intent(this, Overlay.class);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            ab.setDisplayShowTitleEnabled(false);
            startActivity(intenty);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    public boolean createNewNote(String noteName){
        final LinearLayout linlayout =  findViewById(R.id.notelist);
        File mypath=new File(notesDirectory,noteName);
        //File file = new File(notesDirectory, fname);
        String defaultText = "new note.";

        CardView cv = new CardView(ctx);
        int dp = 300;
        float height =  dp * this.getResources().getDisplayMetrics().density;
        int heightInt = (int) height;
        LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
        EditText et = new EditText(ctx);
        et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int slot = findOpenSlot(noteCards);
        if(slot == -1){
            return false;
        }

        try{
            FileOutputStream os = new FileOutputStream(mypath);
            os = openFileOutput(noteName, Context.MODE_PRIVATE);
            os.write(defaultText.getBytes());
            os.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        notesContent[slot] = et;
        noteCards[slot] = cv;
        noteIndexPairs.add(new Pair<String, Integer>(noteName, slot));
        numberOfNotes++;
        et.setText( defaultText, TextView.BufferType.NORMAL);
        et.setFocusable(false);
        cv.addView(et);
        linlayout.addView(cv, lps);
        return  true;

    }

}

