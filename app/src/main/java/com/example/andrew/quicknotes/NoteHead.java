package com.example.andrew.quicknotes;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
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
    private FrameLayout deleteLayout;
    private FrameLayout newNoteLayout;
    private boolean overlay = false;
    final Service myHeads = this;
    EditText et ;
    String noteContents;
    String currentNote;
    HomeWatcher mHomeWatcher;
    int numberOfNotes = 0;
    ArrayList<String> noteNames = new ArrayList<>();
    String notesDirectoryName = "myQuickNotes";
    int curNoteindex = 0;
    File notesDirectory;

    float phonePxDensity;

    NoteHead nh = this;

    boolean notOutsideClick = true;


    @Override public IBinder onBind(Intent intent) {
        // Not used

        return null;
    }

    @Override public void onCreate() {
        super.onCreate();

        phonePxDensity = this.getResources().getDisplayMetrics().density;


        //gather names of the notes
        notesDirectory = new File(Environment.getExternalStorageDirectory(), notesDirectoryName);
        File[] notes = notesDirectory.listFiles();

        boolean firstFound = false;
        for(File f: notes){
            firstFound = true;
            numberOfNotes++;
            String name = f.getName();
            noteNames.add(name);
        }

        if(!firstFound){
            String newNoteName = "note_" + Integer.toString(numberOfNotes);
            writeFile(newNoteName, "new note.");
            noteNames.add(newNoteName);
            numberOfNotes++;
            currentNote = noteNames.get(0);
            //send new note message
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService((LAYOUT_INFLATER_SERVICE));
        layout =  (FrameLayout) inflater.inflate(R.layout.notes_overlay, null);
        layout.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        layout.setFocusableInTouchMode(true);

        deleteLayout =  (FrameLayout) inflater.inflate(R.layout.delete, null);
        deleteLayout.setFocusableInTouchMode(true);

        newNoteLayout = (FrameLayout) inflater.inflate(R.layout.addnote, null);
        newNoteLayout.setFocusableInTouchMode(true);

        chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.ic_library_books_black_24dp);

        FileInputStream is;
        currentNote = noteNames.get(0);
        noteContents = getFileContents(currentNote);

        //EditText et = layout.findViewById(R.id.noteText);
        Log.i("note", noteContents);
        et = layout.findViewById(R.id.noteText);
        et.setText( noteContents, TextView.BufferType.EDITABLE);


        final TextView tv = layout.findViewById(R.id.counter);
        final TextView nameWatermark = layout.findViewById(R.id.noteWaterM);
        final Button sharebut = layout.findViewById(R.id.shareBut);
        final Button newNoteBut = layout.findViewById(R.id.newNoteBut);
        nameWatermark.setText(currentNote);
        updateNoteIndicator(curNoteindex+1, numberOfNotes, tv);

        Button goLeft = layout.findViewById(R.id.leftNote);
        Button goRight = layout.findViewById(R.id.rightNote);
        Button delete = layout.findViewById(R.id.deleteBut);
        Button add = layout.findViewById(R.id.newNoteBut);

        goLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((curNoteindex+1) > 1){
                    String curContents = String.valueOf(et.getText());
                    if(noteContents.compareTo(curContents) != 0){
                        writeFile(currentNote, curContents);
                        sendUpdateMessage();
                    }
                    curNoteindex--;
                    currentNote  = noteNames.get(curNoteindex);
                    updateText(et, nameWatermark, noteNames.get(curNoteindex));
                    updateNoteIndicator(curNoteindex+1, numberOfNotes, tv);
                }
            }
        });

        goRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((curNoteindex+1) < numberOfNotes){
                    String curContents = String.valueOf(et.getText());
                    if(noteContents.compareTo(curContents) != 0){
                        writeFile(currentNote, curContents);
                        sendUpdateMessage();
                    }
                    curNoteindex++;
                    currentNote = noteNames.get(curNoteindex);
                    updateText(et, nameWatermark, noteNames.get(curNoteindex));
                    updateNoteIndicator(curNoteindex+1, numberOfNotes, tv);
                }
            }
        });






        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                //WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);


        //params.x = -10;
        params.y = 0;

        // TRY MAKING THIS ITS OWN ACTIVITY then use moveTaskToBack
        final WindowManager.LayoutParams OverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        //OverlayParams.gravity = Gravity.LEFT;

        final WindowManager.LayoutParams deleteParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);


        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windowManager.addView(deleteLayout, deleteParams);
            }
        });

        newNoteBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windowManager.addView(newNoteLayout, deleteParams);
            }
        });

        Button yd = deleteLayout.findViewById(R.id.yesDelete);
        Button nd = deleteLayout.findViewById(R.id.noDelete);

        Button na = newNoteLayout.findViewById(R.id.noDelete);

        yd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (curNoteindex > 0) {
                    noteNames.remove(curNoteindex);
                    curNoteindex--;
                    numberOfNotes--;
                    updateText(et, nameWatermark, noteNames.get(curNoteindex));
                    updateNoteIndicator(curNoteindex + 1, numberOfNotes, tv);
                    sendDeleteMessage(currentNote);
                    currentNote = noteNames.get(curNoteindex);
                } else {
                    if (curNoteindex == 0 && numberOfNotes == 1) {
                        sendDeleteMessage(currentNote);
                        sendMessage();
                        stopSelf();
                    } else {
                        noteNames.remove(curNoteindex);
                        numberOfNotes--;
                        updateText(et, nameWatermark, noteNames.get(curNoteindex));
                        updateNoteIndicator(curNoteindex + 1, numberOfNotes, tv);
                        sendDeleteMessage(currentNote);
                        currentNote = noteNames.get(curNoteindex);
                    }
                }
                windowManager.removeView(deleteLayout);
            }
        });
        nd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windowManager.removeView(deleteLayout);
            }
        });

        na.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windowManager.removeView(newNoteLayout);
            }
        });

        sharebut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = et.getText().toString();
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, currentNote);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                Intent chooser = Intent.createChooser(sharingIntent, "Share note using");
                chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chooser);
            }
        });


        final int maxWidth = getScreenWidth();
        final int maxHeight = getScreenHeight();
        params.x = -(maxWidth/2) + 10;
        Log.i("maxWidth", Integer.toString(maxWidth));
        Log.i("maxHeight", Integer.toString(maxHeight));

        chatHead.setOnTouchListener(new View.OnTouchListener() {


            private float mdx = 0, mdy = 0;
            int oldx = 0, oldy = 0;
            int roldx = 0, roldy = 0;

            int differenceX = 0, differenceY = 0;



            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                int action = motionEvent.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    mdx = params.x - motionEvent.getRawX();
                    mdy = params.y - motionEvent.getRawY();
                    roldx = params.x; roldy = params.y;
                    oldx = params.x; oldy = params.y;
                    differenceX = 0; differenceY = 0;

                    chatHead.setImageResource(R.drawable.ic_library_books_black2_24dp);

                } else
                if(action == MotionEvent.ACTION_MOVE) {
                        int sumx = (int) (motionEvent.getRawX() + mdx);
                        int sumy = (int) (motionEvent.getRawY() + mdy);

                        Log.i("sumx", Integer.toString(sumx));
                        Log.i("max width", Integer.toString(maxWidth));

                        if(Math.abs(sumx) > (maxWidth/2) - (10*phonePxDensity)) sumx = oldx;
                        if(Math.abs(sumy) > (maxHeight/2) - (10*phonePxDensity)) sumy = oldy;
                        //params.x = (int) (motionEvent.getRawX() + mdx);
                        //params.y = (int) (motionEvent.getRawY() + mdy);
                        params.x = sumx;
                        params.y = sumy;

                        differenceX += Math.abs(oldx - params.x) / phonePxDensity;
                        differenceY += Math.abs(oldy - params.y) / phonePxDensity;

                        Log.i("width", Integer.toString(params.x));
                        Log.i("height", Integer.toString(params.y));

                        oldx = params.x; oldy = params.y;


                        windowManager.updateViewLayout(chatHead, params);

                } else
                if(action == MotionEvent.ACTION_UP){
                    //animate to left if notes are more towards the left
                    chatHead.setImageResource(R.drawable.ic_library_books_black_24dp);

                    if(params.y > (maxHeight/2) - 100){
                        sendMessage();
                        writeFile(currentNote, String.valueOf(et.getText()));
                        sendUpdateMessage();
                        myHeads.stopSelf();

                    }else{
                        if(wasClicked(roldx, roldy, params.x, params.y, differenceX, differenceY)) {
                            if (!overlay && notOutsideClick) {
                                windowManager.addView(layout, OverlayParams);

                                overlay = true;
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
                                      int oldx = 0, oldy = 0;
                                      @Override
                                      public boolean onTouch(View view, MotionEvent motionEvent) {


                                          int action = motionEvent.getAction();
                                          et2.clearFocus();
                                          InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                                          if(action == MotionEvent.ACTION_DOWN){
                                              mdx = OverlayParams.x - motionEvent.getRawX();
                                              mdy = OverlayParams.y - motionEvent.getRawY();
                                              oldx = OverlayParams.x; oldy = OverlayParams.y;


                                          } else
                                          if(action == MotionEvent.ACTION_MOVE) {
                                              Log.i("x position", Integer.toString(OverlayParams.x));
                                              Log.i("y position", Integer.toString(OverlayParams.y));
                                              int sumx = (int) (motionEvent.getRawX() + mdx);
                                              int sumy = (int) (motionEvent.getRawY() + mdy);

                                              if(Math.abs(sumx) > (maxWidth/2) - (10*phonePxDensity)) sumx = oldx;
                                              if(Math.abs(sumy) > (maxHeight/2) - (10*phonePxDensity)) sumy = oldy;

                                              OverlayParams.x = sumx; OverlayParams.y = sumy;

                                              oldx = OverlayParams.x; oldy = OverlayParams.y;

                                              windowManager.updateViewLayout(layout, OverlayParams);
                                          return true;


                                      }
                                      if(action == MotionEvent.ACTION_OUTSIDE){
                                              notOutsideClick = false;
                                              Thread t1 = new Thread(new Runnable() {
                                                 public void run() {
                                                  SystemClock.sleep(500);
                                                  notOutsideClick = true;
                                                }
                                              });
                                              t1.start();
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

        deleteLayout.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                Log.i("key pressed", Integer.toString(i));
                if(i == KeyEvent.KEYCODE_BACK || i == KeyEvent.KEYCODE_HOME){
                    Log.i("back", "BACK");
                    windowManager.removeView(deleteLayout);
                    //overlay = !overlay;
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
                String curContents = String.valueOf(et.getText());
                if(noteContents.compareTo(curContents) != 0){
                    writeFile(currentNote, curContents);
                    sendUpdateMessage();
                }
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

    // Do the actual file io frome the service in case main dies/fails which it has
    private void sendUpdateMessage(){
        Intent intent = new Intent("updateNotes");
        intent.putExtra("cardName", currentNote);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendDeleteMessage(String noteName){
        Intent intent = new Intent("deleteNote");
        intent.putExtra("cardName", currentNote);

        File noteToBeDeleted = new File(notesDirectory, noteName);
        boolean successfulDelete;
        successfulDelete = noteToBeDeleted.delete();

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

    public boolean wasClicked(int oldx, int oldy, int newx, int newy, int difx, int dify){
        float pixelDensity = this.getResources().getDisplayMetrics().density;
        float oldxDP = oldx/pixelDensity;
        float oldyDP = oldy/pixelDensity;
        float newxDP = newx/pixelDensity;
        float newyDP = newy/pixelDensity;
        int clickRange = 10;
        float topRangex =  (oldxDP + clickRange);
        float bottomRangex =  (oldxDP - clickRange);
        float topRangey = (oldyDP + clickRange);
        float bottomRangey = (oldyDP - clickRange);

        if(difx > 30 || dify > 30){
            return false;
        }

        if( ((newxDP > bottomRangex && newxDP < topRangex) && (newyDP > bottomRangey && newyDP < topRangey)) ){
            return true;
        }

        return false;
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

    public void updateNoteIndicator(int curIndex, int totalNotes, TextView tv){
        String noteIndicator = Integer.toString(curNoteindex+1) + "/" + Integer.toString(numberOfNotes);
        tv.setText(noteIndicator);
    }

    public void updateText(EditText et, TextView name, String noteName){
        String noteContents = getFileContents(noteName);
        et.setText( noteContents, TextView.BufferType.EDITABLE);
        name.setText(noteName);
    }



}



