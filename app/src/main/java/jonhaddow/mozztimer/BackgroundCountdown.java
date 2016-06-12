package jonhaddow.mozztimer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by haddo on 11/06/2016.
 */
public class BackgroundCountdown extends Service {

    private static final int COUNT_DOWN_INTERVAL = 1000;

    // Is timer paused or playing?
    public static boolean isPaused = false;

    // Is service running or not?
    public static boolean isRunning = false;

    private NotificationManager mNotificationManager;
    private PowerManager.WakeLock mWakeLock;
    private LocalBroadcastManager broadcastManager;
    private CountDownTimer mCountDown;
    private int mMilliRemaining;
    private BroadcastReceiver mBroadcastReceiver;
    private int mMilliseconds2Start;
    private int minuteCounter;

    @Override
    public void onCreate() {

        isRunning = true;
        isPaused = false;

        // Get notification manager
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // Get type of broadcast.
                String type = intent.getStringExtra("type");

                // Deal with broadcast depending on type.
                switch (type) {
                    case CustomBroadcasts.CANCEL_TIMER:
                        cancelTimer();
                        break;
                    case CustomBroadcasts.PAUSE_TIMER:
                        pauseTimer();
                        break;
                    case CustomBroadcasts.PLAY_TIMER:
                        resumeTimer();
                        break;
                    case CustomBroadcasts.REPLAY_TIMER:
                        replayTimer();
                }
            }
        };

        // Register receivers
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(CustomBroadcasts.BROADCAST));

        // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        mWakeLock.acquire();

        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Get the length of the timer and start countdown.
        mMilliseconds2Start = intent.getIntExtra("Milli", 0);
        startCountdownTimer(mMilliseconds2Start);

        return super.onStartCommand(intent, flags, startId);
    }

    private void startCountdownTimer(int milli) {

        minuteCounter = 0;

        // Start countdown service
        mCountDown = new CountDownTimer(milli, COUNT_DOWN_INTERVAL) {

            @Override
            public void onTick(long millisUntilFinished) {

                minuteCounter++;

                // Buzz every 60 seconds
                if (minuteCounter == 60) {

                    Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);

                    minuteCounter = 0;
                }

                mMilliRemaining = (int) millisUntilFinished;

                TimeConverter myTimer = new TimeConverter(mMilliRemaining);

                // Start foreground notification with time remaining
                startForeground(1,
                        Notifications.setupRunningNotification(getApplicationContext(), myTimer, mMilliseconds2Start).build());


                // Send remaining milliseconds to main activity to update UI
                sendResult(mMilliRemaining);
            }

            @Override
            public void onFinish() {

                // Send broadcast to main activity to update UI
                sendResult(0);

                stopForeground(true);

                // Notify user that timer has stopped
                mNotificationManager.notify(1, Notifications.setupFinishedNotification(getApplicationContext()).build());

                isRunning = false;
            }
        }.start();
    }

    private void sendResult(int milliRemaining) {

        Intent intent = new Intent(CustomBroadcasts.BROADCAST);
        intent.putExtra("type", CustomBroadcasts.TIME_REMAINING);
        intent.putExtra(CustomBroadcasts.TIME_REMAINING, milliRemaining);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {

        isRunning = false;

        // unregister mStopTimerReceiver
        broadcastManager.unregisterReceiver(mBroadcastReceiver);

        // Release wake lock
        mWakeLock.release();
        super.onDestroy();
    }

    private void replayTimer() {

        startCountdownTimer(mMilliseconds2Start);
    }

    private void cancelTimer() {

        // Cancel notifications, countdown and stop service
        mNotificationManager.cancelAll();
        if (mCountDown != null) {
            mCountDown.cancel();
        }
        stopSelf();
    }

    private void pauseTimer() {

        isPaused = true;

        mCountDown.cancel();

        TimeConverter myTimer = new TimeConverter(mMilliRemaining);

        startForeground(1, Notifications.setupPausedNotification(getApplicationContext(), myTimer).build());

        sendResult(mMilliRemaining);
    }

    private void resumeTimer() {

        isPaused = false;

        // Restart countdown service
        startCountdownTimer(mMilliRemaining);
    }

}
