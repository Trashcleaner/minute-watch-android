package jonhaddow.mozztimer;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    // References id of all digit buttons
    private final int[] mDigitButtons = {
            R.id.b_digit0,
            R.id.b_digit1,
            R.id.b_digit2,
            R.id.b_digit3,
            R.id.b_digit4,
            R.id.b_digit5,
            R.id.b_digit6,
            R.id.b_digit7,
            R.id.b_digit8,
            R.id.b_digit9
    };
    private View mRootView;
    private TextView mTvTimeRemaining;
    private ImageView mIvPauseTimer;
    private ImageView mIvCancelTimer;
    private LocalBroadcastManager mBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Implement on Long click listener and on click listener for all elements in view
        findViewById(R.id.ib_delete).setOnLongClickListener(this);
        findViewById(R.id.ib_delete).setOnClickListener(this);
        findViewById(R.id.fab_start_timer).setOnClickListener(this);
        for (int digitButton : mDigitButtons) {
            findViewById(digitButton).setOnClickListener(this);
        }
        mTvTimeRemaining = (TextView) findViewById(R.id.timeRemaining);
        mIvPauseTimer = (ImageView) findViewById(R.id.iv_pause_timer);
        mIvCancelTimer = (ImageView) findViewById(R.id.iv_cancel_timer);
        mIvPauseTimer.setOnClickListener(this);
        mIvCancelTimer.setOnClickListener(this);

        mRootView = findViewById(R.id.content);

        // Manage local broadcasts from this activity.
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // Get type of broadcast.
                String type = intent.getStringExtra("type");

                // Deal with broadcast depending on the type.
                switch (type) {
                    case CustomBroadcasts.TIME_REMAINING:
                        int timeRemaining = intent.getIntExtra(CustomBroadcasts.TIME_REMAINING, 0);
                        if (timeRemaining < 1) {
                            hideBottomBar();
                        }
                        updateTimeRemaining(timeRemaining);
                        break;
                    case CustomBroadcasts.CANCEL_TIMER:
                        hideBottomBar();
                        cancelTimer();
                        break;
                    case CustomBroadcasts.PAUSE_TIMER:
                        pauseTimer();
                        break;
                    case CustomBroadcasts.PLAY_TIMER:
                        resumeTimer();
                        break;
                    case CustomBroadcasts.REPLAY_TIMER:
                        showBottomBar();
                }
            }

        };
    }

    @Override
    public void onClick(View v) {
        Intent broadcastIntent = new Intent(CustomBroadcasts.BROADCAST);
        switch (v.getId()) {
            case R.id.iv_pause_timer:
                // If it's paused, play. Else, pause.
                if (BackgroundCountdown.isPaused) {
                    broadcastIntent.putExtra("type", CustomBroadcasts.PLAY_TIMER);
                } else {
                    broadcastIntent.putExtra("type", CustomBroadcasts.PAUSE_TIMER);
                }
                mBroadcastManager.sendBroadcast(broadcastIntent);
                break;
            case R.id.iv_cancel_timer:
                broadcastIntent.putExtra("type", CustomBroadcasts.CANCEL_TIMER);
                mBroadcastManager.sendBroadcast(broadcastIntent);
            case R.id.ib_delete:
                // If delete button is clicked, collect current display data and shift all digits to the right
                setTimerLogic.removeNumberFromDisplay(setTimerLogic.collectDisplayData(mRootView));
                break;
            case R.id.fab_start_timer:
                // If FAB button is clicked
                onStartTimer();
                break;
            default:
                //When a digit is selected, the value is added to the display
                setTimerLogic.addNumberToDisplay(
                        String.valueOf(((Button) v).getText()),
                        setTimerLogic.collectDisplayData(mRootView.findViewById(R.id.content)));
        }
    }

    private void hideBottomBar() {

        mTvTimeRemaining.setText("");
        mIvPauseTimer.setVisibility(View.INVISIBLE);
        mIvCancelTimer.setVisibility(View.INVISIBLE);
    }

    public void updateTimeRemaining(int milliseconds) {

        // Convert milliseconds to Timer class
        TimeConverter myTimer = new TimeConverter(milliseconds);

        if (milliseconds < 1) {
            mTvTimeRemaining.setText("");
        } else {
            mTvTimeRemaining.setText(myTimer.toString());
        }
    }

    public void cancelTimer() {

        // Cancel all notifications
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();

        mTvTimeRemaining.setText("");

        hideBottomBar();
    }

    public void pauseTimer() {

        // Change to play drawable
        mIvPauseTimer.setImageResource(R.drawable.ic_play_circle);
    }

    public void resumeTimer() {

        // Change to pause drawable
        mIvPauseTimer.setImageResource(R.drawable.ic_pause_circle);
    }

    private void showBottomBar() {

        mIvPauseTimer.setVisibility(View.VISIBLE);
        mIvCancelTimer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {

        // Unregister receiver
        mBroadcastManager.unregisterReceiver(mBroadcastReceiver);

        super.onPause();
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Register receivers
        mBroadcastManager.registerReceiver((mBroadcastReceiver),
                new IntentFilter(CustomBroadcasts.BROADCAST));


        // Set correct pause/play drawable.
        if (BackgroundCountdown.isPaused) {
            mIvPauseTimer.setImageDrawable(getDrawable(R.drawable.ic_play_circle));
        } else {
            mIvPauseTimer.setImageDrawable(getDrawable(R.drawable.ic_pause_circle));
        }

        // Clear time remaining text when no timer is running.
        if (!BackgroundCountdown.isRunning) {
            hideBottomBar();
        } else {
            showBottomBar();
        }

    }

    private void onStartTimer() {

        // Check that service isn't already running
        if (BackgroundCountdown.isRunning) {
            Toast.makeText(this, "Timer already running", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect current display values and convert to milliseconds.
        TextView[] displayNumbers = setTimerLogic.collectDisplayData(mRootView);
        int hours = Integer.parseInt(String.valueOf(displayNumbers[0].getText()) + String.valueOf(displayNumbers[1].getText()));
        int minutes = Integer.parseInt(String.valueOf(displayNumbers[2].getText() + String.valueOf(displayNumbers[3].getText())));
        int seconds = Integer.parseInt(String.valueOf(displayNumbers[4].getText() + String.valueOf(displayNumbers[5].getText())));
        TimeConverter myTimer = new TimeConverter(hours,minutes,seconds);
        int milliseconds = myTimer.getMilli();

        // Clear Display.
        setTimerLogic.clearDisplay(setTimerLogic.collectDisplayData(mRootView));

        if (milliseconds == 0) {
            Toast.makeText(this, R.string.toast_no_input, Toast.LENGTH_SHORT).show();
            return;
        }

        // Start a new countdown service
        Intent countdownIntent = new Intent(this, BackgroundCountdown.class);
        countdownIntent.putExtra("Milli", myTimer.getMilli());
        startService(countdownIntent);

        showBottomBar();
    }

    @Override
    public boolean onLongClick(View v) {

        setTimerLogic.clearDisplay(setTimerLogic.collectDisplayData(mRootView));
        return false;
    }
}
