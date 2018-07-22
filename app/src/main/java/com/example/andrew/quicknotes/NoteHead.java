package com.example.andrew.quicknotes;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.CircularArray;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by andrew on 2/22/2018.
 */

public class NoteHead extends Service {
    private WindowManager windowManager;
    private ImageView chatHead;
    private FrameLayout layout;
    private boolean overlay = false;
    final Service myHeads = this;
    EditText et ;
    String noteContents;
    String currentNote;
    HomeWatcher mHomeWatcher;
    int numberOfNotes = 0;
    ArrayList<String> noteNames = new ArrayList<>();
    String notesDirectoryName = "myQuickNotes";
    File notesDirectory;


    @Override public IBinder onBind(Intent intent) {
        // Not used

        return null;
    }

    @Override public void onCreate() {
        super.onCreate();

        //gather names of the notes
        notesDirectory = new File(Environment.getExternalStorageDirectory(), notesDirectoryName);
        File[] notes = notesDirectory.listFiles();
        for(File f: notes){
            numberOfNotes++;
            String name = f.getName();
            noteNames.add(name);
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService((LAYOUT_INFLATER_SERVICE));
        layout =  (FrameLayout) inflater.inflate(R.layout.notes_overlay, null);
        layout.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        layout.setFocusableInTouchMode(true);



        chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.notes_icon);

        FileInputStream is;
        currentNote = noteNames.get(0);
        /*try{
            int val;
            is = openFileInput(firstNote);
            while((val = is.read()) != -1){
                Log.i("new note val", Character.toString((char)val));
                originalNote = originalNote.concat(Character.toString((char)val));
            }
        }catch (Exception e){
            e.printStackTrace();
        }*/
        noteContents = getFileContents(currentNote);

        //EditText et = layout.findViewById(R.id.noteText);
        Log.i("note", noteContents);
        et = layout.findViewById(R.id.noteText);
        et.setText( noteContents, TextView.BufferType.EDITABLE);






        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                //WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

       // params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = -10;
        params.y = 0;

        // TRY MAKING THIS ITS OWN ACTIVITY then use moveTaskToBack
        final WindowManager.LayoutParams OverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        final int maxWidth = getScreenWidth();
        final int maxHeight = getScreenHeight();
        Log.i("maxWidth", Integer.toString(maxWidth));
        Log.i("maxHeight", Integer.toString(maxHeight));

        chatHead.setOnTouchListener(new View.OnTouchListener() {


            private float mdx = 0, mdy = 0;
            int oldx = 0, oldy = 0;
            int roldx = 0, roldy = 0;



            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int action = motionEvent.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    mdx = params.x - motionEvent.getRawX();
                    mdy = params.y - motionEvent.getRawY();
                    roldx = params.x; roldy = params.y;
                    oldx = params.x; oldy = params.y;

                    chatHead.setImageResource(R.drawable.notes_icon_pressed);

                } else
                if(action == MotionEvent.ACTION_MOVE) {
                        int sumx = (int) (motionEvent.getRawX() + mdx);
                        int sumy = (int) (motionEvent.getRawY() + mdy);

                        if(Math.abs(sumx) > (maxWidth/2) - 40) sumx = oldx;
                        if(Math.abs(sumy) > (maxHeight/2) - 40) sumy = oldy;
                        //params.x = (int) (motionEvent.getRawX() + mdx);
                        //params.y = (int) (motionEvent.getRawY() + mdy);
                        params.x = sumx;
                        params.y = sumy;

                        Log.i("width", Integer.toString(params.x));
                        Log.i("height", Integer.toString(params.y));

                        oldx = params.x; oldy = params.y;


                        windowManager.updateViewLayout(chatHead, params);

                } else
                if(action == MotionEvent.ACTION_UP){
                    //animate to left if notes are more towards the left
                    chatHead.setImageResource(R.drawable.notes_icon);

                    if(params.y > (maxHeight/2) - 60){
                        sendMessage();
                        writeFile("note_0.txt", String.valueOf(et.getText()));
                        sendUpdateMessage();
                        myHeads.stopSelf();

                    }else{
                        if(roldx== params.x && roldy == params.y) {
                            if (!overlay) {
                                windowManager.addView(layout, OverlayParams);

                                overlay = true;
                            } else {
                                windowManager.removeView(layout);
                                overlay = false;
                            }
                        }
                    }
                }
                return true;
            }

        }
        );

        final EditText et2 = layout.findViewById(R.id.noteText);
        layout.setOnTouchListener(new View.OnTouchListener() {
                                      float mdx=0, mdy=0;
                                      @Override
                                      public boolean onTouch(View view, MotionEvent motionEvent) {

                                          int action = motionEvent.getAction();
                                          et2.clearFocus();
                                          InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                                          if(action == MotionEvent.ACTION_DOWN){
                                              mdx = OverlayParams.x - motionEvent.getRawX();
                                              mdy = OverlayParams.y - motionEvent.getRawY();

                                          } else
                                          if(action == MotionEvent.ACTION_MOVE) {
                                              Log.i("x position", Integer.toString(OverlayParams.x));
                                              Log.i("y position", Integer.toString(OverlayParams.y));
                                              OverlayParams.x = (int) (motionEvent.getRawX() + mdx);
                                              OverlayParams.y = (int) (motionEvent.getRawY() + mdy);

                                              windowManager.updateViewLayout(layout, OverlayParams);
                                          return true;


                                      }
                                      if(action == MotionEvent.ACTION_OUTSIDE){
                                              imm.hideSoftInputFromWindow(et2.getWindowToken(), 0);
                                              windowManager.removeView(layout);
                                              overlay = !overlay;


                                      }
                                      return true;
                                  }});

        layout.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                Log.i("key pressed", Integer.toString(i));
                if(i == KeyEvent.KEYCODE_BACK || i == KeyEvent.KEYCODE_HOME){
                    Log.i("back", "BACK");
                    windowManager.removeView(layout);
                    overlay = !overlay;
                }

                return true;
            }
        });

        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i == KeyEvent.KEYCODE_BACK) {
                    et.clearFocus();
                    return true;
                }
                else return false;
            }
        });



        final Button button4 = layout.findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windowManager.removeView(layout);
                overlay = false;
            }
        });


                windowManager.addView(chatHead, params);

                mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                if(overlay){
                    windowManager.removeView(layout);
                    overlay = !overlay;
                }
            }
            @Override
            public void onHomeLongPressed() {
                if(overlay){
                    windowManager.removeView(layout);
                    overlay = !overlay;
                }
            }
        });
        mHomeWatcher.startWatch();



    }


    @Override
    public void onDestroy() {
        mHomeWatcher.stopWatch();
        String lastEdit = String.valueOf(et.getText());
        if(lastEdit.compareTo(noteContents) != 0){
            Log.i("writing", "writing new d");
            writeFile("note_0.txt", lastEdit);
        }

        super.onDestroy();
        if(overlay) {
            windowManager.removeView(layout);
        }
         windowManager.removeView(chatHead);

    }

    private void sendMessage() {
        // The string "my-integer" will be used to filer the intent
        Intent intent = new Intent("toggleHeads");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendUpdateMessage(){
        Intent intent = new Intent("updateNotes");
        intent.putExtra("cardName", currentNote);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public void writeFile(String fname, String data){
        File mypath=new File(notesDirectory,fname);
        String defaultText = "new note.";
        try{
            FileOutputStream os = new FileOutputStream(mypath);
            os = openFileOutput(fname, Context.MODE_PRIVATE);
            os.write(data.getBytes());
            os.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private String getCurNote() {
        return currentNote;
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
}



