package com.huaqin.russell.ServiceDemo;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ServiceDemo";

    private Thread mRunThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("Have to say something");
        mRunThread = new Thread(new Runnable() {
            public void run() {
                Random r = new Random();
                for (;;) {
                    long v = (long)(r.nextGaussian() * 3000 + 10000);
                    if (v <= 10) {
                        v = 10;
                    }
                    sendSwipe(InputDevice.SOURCE_TOUCHSCREEN, 600, 600, 20, 200, 300);
                    Log.d(TAG, String.format("delay time %d", v));
                    SystemClock.sleep(v);
                }

            }
        });
        mRunThread.start();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void sendTap(int inputSource, float x, float y) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x, y, 1.0f);
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x, y, 0.0f);
    }

    private void sendSwipe(int inputSource, float x1, float y1, float x2, float y2, int duration) {
        if (duration < 0) {
            duration = 300;
        }
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x1, y1, 1.0f);
        long startTime = now;
        long endTime = startTime + duration;
        while (now < endTime) {
            long elapsedTime = now - startTime;
            float alpha = (float) elapsedTime / duration;
            injectMotionEvent(inputSource, MotionEvent.ACTION_MOVE, now, lerp(x1, x2, alpha),
                    lerp(y1, y2, alpha), 1.0f);
            now = SystemClock.uptimeMillis();
        }
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x2, y2, 0.0f);
    }

    private static final float lerp(float a, float b, float alpha) {
        return (b - a) * alpha + a;
    }

    class InjectInputEventCaller {
        private Object InputManagerObj = null;
        private Method injectInputEvent = null;
        InjectInputEventCaller() {
            try {
                Class clazz = ClassLoader.getSystemClassLoader().loadClass("android.hardware.input.InputManager");
                Method m = clazz.getMethod("getInstance");
                InputManagerObj = m.invoke(null);
                injectInputEvent = clazz.getMethod("injectInputEvent", android.view.InputEvent.class, int.class);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                InputManagerObj = null;
                injectInputEvent = null;
            }
        }
        public void invoke( android.view.InputEvent event, int id) {
            if (injectInputEvent == null || InputManagerObj == null) {
                return;
            }
            try {
                injectInputEvent.invoke(InputManagerObj, event, id);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
    final private InjectInputEventCaller injectInputEvent = new InjectInputEventCaller();
    /**
     * Builds a MotionEvent and injects it into the event stream.
     *
     * @param inputSource the InputDevice.SOURCE_* sending the input event
     * @param action the MotionEvent.ACTION_* for the event
     * @param when the value of SystemClock.uptimeMillis() at which the event happened
     * @param x x coordinate of event
     * @param y y coordinate of event
     * @param pressure pressure of event
     */
    private void injectMotionEvent(int inputSource, int action, long when, float x, float y, float pressure) {
        final float DEFAULT_SIZE = 1.0f;
        final int DEFAULT_META_STATE = 0;
        final float DEFAULT_PRECISION_X = 1.0f;
        final float DEFAULT_PRECISION_Y = 1.0f;
        final int DEFAULT_DEVICE_ID = 0;
        final int DEFAULT_EDGE_FLAGS = 0;
        MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, DEFAULT_SIZE,
                DEFAULT_META_STATE, DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
                DEFAULT_EDGE_FLAGS);
        event.setSource(inputSource);
        Log.i(TAG, "injectMotionEvent: " + event);
        injectInputEvent.invoke(event, 2);
    }

    float initialX, initialY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //mGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                initialY = event.getY();
                Log.d(TAG, String.format("Action was DOWN at (%f,%f)", initialX, initialY));
                break;

            case MotionEvent.ACTION_MOVE:
                float midX = event.getX();
                float midY = event.getY();
                //Log.d(TAG, String.format("Action was MOVE at (%f,%f)", midX, midY));
                break;

            case MotionEvent.ACTION_UP:
                float finalX = event.getX();
                float finalY = event.getY();
                Log.d(TAG, String.format("Action was UP   at (%f,%f)", finalX, finalY));
                break;

            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG,"Action was CANCEL");
                break;

            case MotionEvent.ACTION_OUTSIDE:
                Log.d(TAG, "Movement occurred outside bounds of current screen element");
                break;
        }

        return super.onTouchEvent(event);
    }
}
