package com.example.andrew.quicknotes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

//data structures
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity {

    boolean headsOn = false;
    final AppCompatActivity thisView = this;
    File notesDirectory;
    String notesDirectoryName = "myQuickNotes";
    int numberOfNotes = 0;
    EditText[] notesContent = new EditText[5];
    CardView[] noteCards = new CardView[5];
    TextView[] noteDates = new TextView[5];
    List<Pair<String, Integer>> noteIndexPairs = new ArrayList<Pair<String, Integer>>();
    List<Pair<Pair<String, CardView>, EditText>> listOfCards = new ArrayList<>();
    PriorityQueue<Long> lastModifiedDates = new PriorityQueue<Long>();
    List<Pair<String,Long>> noteDatePairs = new ArrayList<>();
    Context ctx = this;

    Intent SettingsActivity;
    ActionBar ab;

    float phonePxDensity;

    boolean candraw = true;

    AlertDialog.Builder deleteBuilder;
    AlertDialog.Builder fileNameBuilder;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ab = getSupportActionBar();
        ab.setBackgroundDrawable(new ColorDrawable(Color.rgb(192, 239, 167)));

        phonePxDensity = this.getResources().getDisplayMetrics().density;

        if(Build.VERSION.SDK_INT >= 23){
        candraw = Settings.canDrawOverlays(this);
        Log.i("canDraw", Boolean.toString(candraw));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatActivity thisV = thisView;

        boolean granted = isStoragePermissionGranted();
        Log.i("GRANTED", Boolean.toString(granted));

        //if granted do everything below

        if(granted){


        final EditText fileNameInput = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        fileNameInput.setLayoutParams(lp);
        fileNameInput.setTextColor(Color.rgb(255,255,255));



        // Alert Dialog for deleting a note
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            deleteBuilder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            fileNameBuilder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            deleteBuilder = new AlertDialog.Builder(this);
            fileNameBuilder = new AlertDialog.Builder(this);
        }
        deleteBuilder.setTitle("Delete note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
        final ScrollView sv = findViewById(R.id.scroller);
        fileNameBuilder.setView(fileNameInput);
        fileNameBuilder.setTitle("Note name")
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newNoteName = fileNameInput.getText().toString();
                        String created = createNewNote(newNoteName);
                        if(created.compareTo("success") != 0){
                            Toast.makeText(ctx, created, Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(ctx, "new note created", Toast.LENGTH_SHORT).show();
                            sv.scrollTo(0, sv.getTop());
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });


        final AlertDialog newFileDialog = fileNameBuilder.create();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newFileDialog.show();
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
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(deleteReceiver,
                        new IntentFilter("deleteNote"));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(newNoteReceiver,
                        new IntentFilter("addNote"));


        notesDirectory = new File(Environment.getExternalStorageDirectory(), notesDirectoryName);
        if(!notesDirectory.isDirectory()) notesDirectory.mkdirs();
        File[] notes = notesDirectory.listFiles();
        final LinearLayout linlayout =  findViewById(R.id.notelist);
        linlayout.removeAllViewsInLayout();
        noteCards = new CardView[5];
        notesContent = new EditText[5];
        lastModifiedDates.clear();
        noteDatePairs.clear();
        noteIndexPairs.clear();

        for(File f: notes){
            String noteName = f.getName();
            Log.i("fname", noteName);
            long lastModified = f.lastModified();
            lastModifiedDates.add(lastModified);
            noteDatePairs.add(Pair.create(noteName, lastModified));
            int index = findOpenSlot(noteCards);

            CardView cv = new CardView(this);
            int dp = 300;
            float height =  dp * phonePxDensity;
            // code for setting up a card can be placed into a separate function
            int heightInt = (int) height;
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            notesContent[index] = et;
            noteCards[index] = cv;

            noteIndexPairs.add(Pair.create(noteName, index));
            numberOfNotes++;


            /*TextView nameWatermark = new TextView(ctx);
            nameWatermark.setGravity(Gravity.BOTTOM| Gravity.RIGHT);
            nameWatermark.setText(noteName);
            nameWatermark.setPadding(2,2,2,2);
            nameWatermark.setTextSize(20);
            et.setFocusable(false);
            et.setText( noteContents, TextView.BufferType.NORMAL);
            cv.addView(et);
            cv.addView(nameWatermark);
            cv.setPadding(2,4,2,4);
            addEditTransition(cv);
            lps.setMargins(2,4,2,4);
            linlayout.addView(cv, lps);*/
        }

        for(int i = 0; i < numberOfNotes; i++){

            long date;
            if(lastModifiedDates.size() > 1) {
                date = lastModifiedDates.remove();
            }else{
                date = lastModifiedDates.peek();
            }
            Log.i("DATE", Long.toString(date));
            String noteName = getAssociatedNote(date);
            int index = getAssociatedInt(noteName);
            CardView cv = new CardView(this);
            int dp = 300;
            float height =  dp * phonePxDensity;
            // code for setting up a card can be placed into a separate function
            int heightInt = (int) height;
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            String noteContents = getFileContents( noteName);
            Log.i("noteName", noteName);
            notesContent[index] = et;
            noteCards[index] = cv;
            Log.i("noteContents", noteContents);

            TextView nameWatermark = new TextView(ctx);
            nameWatermark.setGravity(Gravity.BOTTOM| Gravity.RIGHT);
            nameWatermark.setText(noteName);
            nameWatermark.setPadding(2,2,2,2);
            nameWatermark.setTextSize(20);

            TextView dateWatermark = new TextView(ctx);
            noteDates[index] = dateWatermark;
            dateWatermark.setGravity(Gravity.BOTTOM | Gravity.LEFT);
            dateWatermark.setText(getDateFormat(date));
            dateWatermark.setPadding(2,2,2,2);
            dateWatermark.setTextSize(15);

            et.setFocusable(false);
            et.setText( noteContents, TextView.BufferType.NORMAL);
            cv.addView(et);
            cv.addView(nameWatermark);
            cv.addView(dateWatermark);
            cv.setPadding(2,4,2,4);
            addEditTransition(cv);
            lps.setMargins(2,4,2,4);
            linlayout.addView(cv, 0, lps);


        }}

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
                LinearLayout linlayout = findViewById(R.id.notelist);
                Log.i("RECIEVED-NAME", cardName);
                String text = getFileContents(cardName);
                Log.i("cardName", cardName);
                int index = getAssociatedInt(cardName);
                notesContent[index].setText(text, TextView.BufferType.NORMAL);
                //linlayout remove view, then push it on the top to resemble last modified
                linlayout.removeView(noteCards[index]);
                float size = 300 * phonePxDensity;
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams((int)size, (int)size);
                cardParams.setMargins(2,4,2,4);
                linlayout.addView(noteCards[index], 0, cardParams);
                long date = System.currentTimeMillis();
                noteDates[index].setText(getDateFormat(date), TextView.BufferType.NORMAL);
            }
        }
    };
    private BroadcastReceiver floatsStartingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Build.VERSION.SDK_INT >= 23){
                candraw = Settings.canDrawOverlays(ctx);
            }
            if(candraw) {
                headsOn = !headsOn;
                startService(new Intent(thisView, NoteHead.class));
                moveTaskToBack(true);
            }else{
                Toast.makeText(ctx, "overlay permission not granted, to turn on this permission go to Settings>Apps>Permissions", Toast.LENGTH_SHORT).show();
                Intent intento = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intento);
            }
        }
    };

    private BroadcastReceiver deleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String cardName = intent.getStringExtra("cardName");
            deleteNote(cardName);
        }
    };

    private BroadcastReceiver newNoteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //not duplicate code down here, the service does the file i/o in case the main activity was killed
            String noteName = intent.getStringExtra("cardName");
            String noteContents = getFileContents(noteName);
            LinearLayout linlayout = findViewById(R.id.notelist);
            int index = findOpenSlot(noteCards);
            CardView cv = new CardView(ctx);
            int dp = 300;
            float height =  dp * phonePxDensity;
            // code for setting up a card can be placed into a separate function
            int heightInt = (int) height;
            LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
            EditText et = new EditText(ctx);
            et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            notesContent[index] = et;
            noteCards[index] = cv;
            noteIndexPairs.add(Pair.create(noteName, index));
            numberOfNotes++;
            Log.i("noteContents", noteContents);

            TextView nameWatermark = new TextView(ctx);
            nameWatermark.setGravity(Gravity.BOTTOM| Gravity.RIGHT);
            nameWatermark.setText(noteName);
            nameWatermark.setPadding(2,2,2,2);
            nameWatermark.setTextSize(20);

            TextView dateWatermark = new TextView(ctx);
            noteDates[index] = dateWatermark;
            dateWatermark.setGravity(Gravity.BOTTOM | Gravity.LEFT);
            dateWatermark.setText(getDateFormat(System.currentTimeMillis()));
            dateWatermark.setPadding(2,2,2,2);
            dateWatermark.setTextSize(15);

            et.setFocusable(false);
            et.setText( noteContents, TextView.BufferType.NORMAL);
            cv.addView(et);
            cv.addView(nameWatermark);
            cv.addView(dateWatermark);
            cv.setPadding(2,4,2,4);
            addEditTransition(cv);
            lps.setMargins(2,4,2,4);
            linlayout.addView(cv, 0,  lps);

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
        if(numberOfNotes == 0) {
            findViewById(R.id.noNotes).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.noNotes).setVisibility(View.INVISIBLE);
        }
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
        SettingsActivity = new Intent(this, Overlay.class);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            ab.setDisplayShowTitleEnabled(false);
            startActivity(SettingsActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == 0){
            Log.i("Permission", "granted!");


            final EditText fileNameInput = new EditText(MainActivity.this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            fileNameInput.setLayoutParams(lp);
            fileNameInput.setTextColor(Color.rgb(255,255,255));



            // Alert Dialog for deleting a note
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                deleteBuilder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                fileNameBuilder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                deleteBuilder = new AlertDialog.Builder(this);
                fileNameBuilder = new AlertDialog.Builder(this);
            }
            deleteBuilder.setTitle("Delete note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
            final ScrollView sv = findViewById(R.id.scroller);
            fileNameBuilder.setView(fileNameInput);
            fileNameBuilder.setTitle("Note name")
                    .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String newNoteName = fileNameInput.getText().toString();
                            String created = createNewNote(newNoteName);
                            if(created.compareTo("success") != 0){
                                Toast.makeText(ctx, created, Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(ctx, "new note created", Toast.LENGTH_SHORT).show();
                                sv.scrollTo(0, sv.getTop());
                            }
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });


            final AlertDialog newFileDialog = fileNameBuilder.create();
            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    newFileDialog.show();
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
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(deleteReceiver,
                            new IntentFilter("deleteNote"));
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(newNoteReceiver,
                            new IntentFilter("addNote"));


            notesDirectory = new File(Environment.getExternalStorageDirectory(), notesDirectoryName);
            if(!notesDirectory.isDirectory()) notesDirectory.mkdirs();
            File[] notes = notesDirectory.listFiles();
            final LinearLayout linlayout =  findViewById(R.id.notelist);
            linlayout.removeAllViewsInLayout();
            noteCards = new CardView[5];
            notesContent = new EditText[5];
            lastModifiedDates.clear();
            noteDatePairs.clear();
            noteIndexPairs.clear();

            for(File f: notes){
                String noteName = f.getName();
                Log.i("fname", noteName);
                long lastModified = f.lastModified();
                lastModifiedDates.add(lastModified);
                noteDatePairs.add(Pair.create(noteName, lastModified));
                int index = findOpenSlot(noteCards);

                CardView cv = new CardView(this);
                int dp = 300;
                float height =  dp * phonePxDensity;
                // code for setting up a card can be placed into a separate function
                int heightInt = (int) height;
                LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
                EditText et = new EditText(this);
                et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                notesContent[index] = et;
                noteCards[index] = cv;

                noteIndexPairs.add(Pair.create(noteName, index));
                numberOfNotes++;


            /*TextView nameWatermark = new TextView(ctx);
            nameWatermark.setGravity(Gravity.BOTTOM| Gravity.RIGHT);
            nameWatermark.setText(noteName);
            nameWatermark.setPadding(2,2,2,2);
            nameWatermark.setTextSize(20);
            et.setFocusable(false);
            et.setText( noteContents, TextView.BufferType.NORMAL);
            cv.addView(et);
            cv.addView(nameWatermark);
            cv.setPadding(2,4,2,4);
            addEditTransition(cv);
            lps.setMargins(2,4,2,4);
            linlayout.addView(cv, lps);*/
            }

            for(int i = 0; i < numberOfNotes; i++){

                long date;
                if(lastModifiedDates.size() > 1) {
                    date = lastModifiedDates.remove();
                }else{
                    date = lastModifiedDates.peek();
                }
                Log.i("DATE", Long.toString(date));
                String noteName = getAssociatedNote(date);
                int index = getAssociatedInt(noteName);
                CardView cv = new CardView(this);
                int dp = 300;
                float height =  dp * phonePxDensity;
                // code for setting up a card can be placed into a separate function
                int heightInt = (int) height;
                LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
                EditText et = new EditText(this);
                et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                String noteContents = getFileContents( noteName);
                Log.i("noteName", noteName);
                notesContent[index] = et;
                noteCards[index] = cv;
                Log.i("noteContents", noteContents);

                TextView nameWatermark = new TextView(ctx);
                nameWatermark.setGravity(Gravity.BOTTOM| Gravity.RIGHT);
                nameWatermark.setText(noteName);
                nameWatermark.setPadding(2,2,2,2);
                nameWatermark.setTextSize(20);

                TextView dateWatermark = new TextView(ctx);
                noteDates[index] = dateWatermark;
                dateWatermark.setGravity(Gravity.BOTTOM | Gravity.LEFT);
                dateWatermark.setText(getDateFormat(date));
                dateWatermark.setPadding(2,2,2,2);
                dateWatermark.setTextSize(15);

                et.setFocusable(false);
                et.setText( noteContents, TextView.BufferType.NORMAL);
                cv.addView(et);
                cv.addView(nameWatermark);
                cv.addView(dateWatermark);
                cv.setPadding(2,4,2,4);
                addEditTransition(cv);
                lps.setMargins(2,4,2,4);
                linlayout.addView(cv, 0, lps);


            }

        }else{
            Toast.makeText(ctx, "this app requires the ability to write and read files", Toast.LENGTH_SHORT);
            finish();
        }
    }

    public  String getFileContents(String noteName){
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

    public String getAssociatedNote(long date){
        Iterator noteDateIterator = noteDatePairs.iterator();
        String noteName = "";
        while(noteDateIterator.hasNext()){
            Pair<String, Long> notePair = (Pair<String, Long>) noteDateIterator.next();
            if(notePair.second == date){
                noteName = notePair.first;
                break;
            }
        }
        if(noteName.compareTo("") == 0){
            Log.i("ERROR", "COULDNT FIND NOTE FROM DATE");
        }
        return noteName;
    }

    public String getDateFormat(long date){
        Date thedate=new Date(date);
        SimpleDateFormat df2 = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");
        String dateText = df2.format(thedate);
        return dateText;
    }

    public void removeFromList(String cardName){
        Iterator noteIterator = noteIndexPairs.iterator();
        int index = -1;
        Pair<String, Integer> theindex = null;
        while(noteIterator.hasNext()){
            Pair<String, Integer> noteIndex = (Pair<String, Integer>) noteIterator.next();
            Log.i("cname", noteIndex.first);
            Log.i("cname2", cardName);
            if(noteIndex.first.compareTo(cardName) == 0){
                theindex = noteIndex;
            }
        }
        noteIndexPairs.remove(theindex);
    }

    public String createNewNote(String noteName){
        final LinearLayout linlayout =  findViewById(R.id.notelist);
        boolean sameName = false;
        File[] notes = notesDirectory.listFiles();
        for(File f : notes){
            if(f.getName().compareTo(noteName) == 0){
                sameName = true;
            }
        }
        if(sameName) return "a note with this name already exists";
        File mypath=new File(notesDirectory,noteName);
        String defaultText = "new note.";

        CardView cv = new CardView(ctx);
        int dp = 300;
        float height =  dp * phonePxDensity;
        int heightInt = (int) height;
        LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(heightInt, heightInt);
        EditText et = new EditText(ctx);
        et.setLayoutParams(new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView nameWatermark = new TextView(ctx);
        //nameWatermark.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        nameWatermark.setGravity(Gravity.BOTTOM| Gravity.RIGHT);
        nameWatermark.setText(noteName);
        nameWatermark.setPadding(2,2,2,2);
        nameWatermark.setTextSize(20);

        long date = System.currentTimeMillis();
        TextView dateWatermark = new TextView(ctx);
        dateWatermark.setGravity(Gravity.BOTTOM | Gravity.LEFT);
        dateWatermark.setText(getDateFormat(date));
        dateWatermark.setPadding(2,2,2,2);
        dateWatermark.setTextSize(15);

        int slot = findOpenSlot(noteCards);
        if(slot == -1){
            return "max number of notes reached";
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
        noteDates[slot] = dateWatermark;
        noteIndexPairs.add(new Pair<String, Integer>(noteName, slot));
        numberOfNotes++;
        et.setText( defaultText, TextView.BufferType.NORMAL);
        et.setFocusable(false);
        cv.addView(et);
        cv.addView(nameWatermark);
        cv.addView(dateWatermark);
        addEditTransition(cv);
        findViewById(R.id.noNotes).setVisibility(View.INVISIBLE);
        cv.setPadding(2,2,2,2);
        linlayout.addView(cv, 0, lps);
        return  "success";

    }

    public boolean deleteNote(String noteName){
        final LinearLayout linlayout =  findViewById(R.id.notelist);
        int cardIndex = getAssociatedInt(noteName);
        numberOfNotes--;
        boolean successfulDelete = true;
        EditText et = notesContent[cardIndex];
        CardView cv = noteCards[cardIndex];
        linlayout.removeView(cv);
        notesContent[cardIndex] = null;
        noteCards[cardIndex] = null;
        removeFromList(noteName);
        return successfulDelete;
    }

    public void setupCard(CardView cv, String noteName, String contents, long dateModified){

    }


    // function for setting a click listener on the card view to transition to an edit activity
    float origX =0, origY = 0;
    float moveX = 0, moveY =0 ;
    public void addEditTransition(final CardView cv){
        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start edit activity
                Toast.makeText(ctx, "transitioning", Toast.LENGTH_SHORT).show();
            }
        });

        cv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    origX = motionEvent.getRawX();
                    origY = motionEvent.getRawY();
                    cv.setCardBackgroundColor(Color.parseColor("#c9e0e0"));

                }
                if(action == MotionEvent.ACTION_UP){
                    cv.setCardBackgroundColor(Color.parseColor("#ffffff"));
                }
                if(action == MotionEvent.ACTION_MOVE){
                    moveX = motionEvent.getRawX();
                    moveY = motionEvent.getRawY();
                    if(Math.abs(origX - moveX)/phonePxDensity > 10 || Math.abs(origY - moveY)/phonePxDensity > 10 )
                    cv.setCardBackgroundColor(Color.parseColor("#ffffff"));
                }
                return false;
            }
        });

    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("blah","Permission is granted");
                return true;
            } else {

                Log.v("nah","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("cah","Permission is granted");
            return true;
        }
    }




}

