package com.example.andrew.quicknotes;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by andrew on 2/22/2018.
 */

public class NoteHead extends Service {
    private WindowManager windowManager;
    private ImageView chatHead;
    private FrameLayout layout;
    private boolean overlay = false;
    boolean noteUp = true;
    final Service myHeads = this;


    @Override public IBinder onBind(Intent intent) {
        // Not used

        return null;
    }

    @Override public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService((LAYOUT_INFLATER_SERVICE));
        layout =  (FrameLayout) inflater.inflate(R.layout.notes_overlay, null);

        chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.ic_launcher_background);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

       // params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = -10;
        params.y = 0;

        // TRY MAKING THIS ITS OWN ACTIVITY then use moveTaskToBack
        final WindowManager.LayoutParams OverlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        chatHead.setOnTouchListener(new View.OnTouchListener() {
            private float mdx = 0, mdy = 0;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    mdx = params.x - motionEvent.getRawX();
                    mdy = params.y - motionEvent.getRawY();

                } else
                if(action == MotionEvent.ACTION_MOVE) {
                    Log.i("x position", Integer.toString(params.x));
                    Log.i("y position", Integer.toString(params.y));
                    params.x = (int) (motionEvent.getRawX() + mdx);
                    params.y = (int) (motionEvent.getRawY() + mdy);

                    windowManager.updateViewLayout(chatHead, params);
                } else
                if(action == MotionEvent.ACTION_UP){
                    //animate to left if notes are more towards the left


                    if(params.y > 450){
                        sendMessage();
                        myHeads.stopSelf();
                        //windowManager.removeView(chatHead);
                    }else{
                        if( !overlay) {
                            windowManager.addView(layout, OverlayParams);

                            overlay = true;
                        }else{
                            windowManager.removeView(layout);
                            overlay = false;
                        }
                    }
                }
                return true;
            }

        });
        final EditText et = layout.findViewById(R.id.noteText);
        layout.setOnTouchListener(new View.OnTouchListener() {
                                      float mdx=0, mdy=0;
                                      @Override
                                      public boolean onTouch(View view, MotionEvent motionEvent) {

                                          int action = motionEvent.getAction();
                                          et.clearFocus();
                                          InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                          imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
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
                                      return true;
                                  }});
        final Button button4 = layout.findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                windowManager.removeView(layout);
                overlay = false;
            }
        });

                windowManager.addView(chatHead, params);

    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatHead != null) windowManager.removeView(chatHead);

    }

    private void sendMessage() {
        // The string "my-integer" will be used to filer the intent
        Intent intent = new Intent("toggleHeads");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
