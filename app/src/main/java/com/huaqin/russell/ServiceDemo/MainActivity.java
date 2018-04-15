package com.huaqin.russell.ServiceDemo;

import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
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

import static java.lang.Math.min;
import static java.lang.Math.max;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ServiceDemo";

    private Thread mRunThread = null;
    Menu mMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tv = findViewById(R.id.sample_text);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText("Hello Service Demo");

        mRunThread = new Thread(new Runnable() {
            public void run() {

                DisplayFromBackgroundThread(null);

                Random r = new Random();
                long startTime = SystemClock.elapsedRealtime();

                for (; ; ) {
                    double v = (r.nextGaussian() * 16000 + 40000);
                    v = max(v, 10.0f);
                    v = min(v, 80000.0f);
                    int x1, y1, x2, y2;
                    boolean forward = (long) v % 4 >= 1;
                    if (forward) {
                        x1 = 600;
                        y1 = 600;
                        x2 = 200;
                        y2 = 200;
                    } else {
                        x1 = 200;
                        y1 = 200;
                        x2 = 600;
                        y2 = 600;

                    }
                    sendSwipe(InputDevice.SOURCE_TOUCHSCREEN, x1, y1, x2, y2, 300);

                    long current = SystemClock.elapsedRealtime();
                    long left = startTime + GetRunningTimeFromUI() * 60 * 1000 - current;
                    if (left <= 0) {
                        break;
                    }
                    String msg = String.format("delay time %.1f, %s, %d seconds left", v / 1000, forward ? "forward" : "backward", left / 1000);
                    Log.i(TAG, msg);
                    DisplayFromBackgroundThread(msg);

                    SystemClock.sleep((long) v);
                }

                DisplayFromBackgroundThread("Finished");
                Log.i(TAG, "Finished");

            }
        });
        mRunThread.start();
    }

    private int GetRunningTimeFromMenuItem(int id, int minutes) {
        if (mMenu == null) {
            return 0;
        }
        MenuItem item = mMenu.findItem(id);
        if (item == null) {
            return 0;
        }
        return item.isChecked() ? minutes :0;
    }
    int GetRunningTimeFromUI() {
        int v = 0;
        v += GetRunningTimeFromMenuItem(R.id.action_15min , 15);
        v += GetRunningTimeFromMenuItem(R.id.action_30min , 30);
        v += GetRunningTimeFromMenuItem(R.id.action_60min , 60);
        v += GetRunningTimeFromMenuItem(R.id.action_90min , 90);
        v += GetRunningTimeFromMenuItem(R.id.action_120min, 120);
        if (v == 0) {
            v = 1;
        }
        return v;
    }
    private String text = "";

    public void DisplayFromBackgroundThread(final String str) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (str == null) {
                    text = "";
                } else {
                    text += str + "\n";
                }
                TextView tv = findViewById(R.id.sample_text);
                tv.setText(text);
                final Layout layout = tv.getLayout();
                if (layout != null) {
                    int scrollDelta = layout.getLineBottom(tv.getLineCount() - 1)
                            - tv.getScrollY() - tv.getHeight();
                    if (scrollDelta > 0)
                        tv.scrollBy(0, scrollDelta);
                }

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(true);
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

        public void invoke(android.view.InputEvent event, int id) {
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
     * @param action      the MotionEvent.ACTION_* for the event
     * @param when        the value of SystemClock.uptimeMillis() at which the event happened
     * @param x           x coordinate of event
     * @param y           y coordinate of event
     * @param pressure    pressure of event
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
        Log.d(TAG, "injectMotionEvent: " + event);
        injectInputEvent.invoke(event, 2);
    }
}
