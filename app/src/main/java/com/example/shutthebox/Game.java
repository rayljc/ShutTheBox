package com.example.shutthebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.shutthebox.ui.PostGameActivity;

import java.util.Random;

public class Game extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "TAG_GAME";
    private static final String LOSER_NAME = "LOSER_NAME";
    private static final double SHAKE_THRESHOLD = 5.0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double lastX, lastY, lastZ;
    private boolean lastInitialized = false;
    Random random = new Random();
    Button rollButton, finishButton, quitButton;
    Button[] woodenCards = new Button[13];  // 13 instead of 12, can be a TO-DO item later
    ImageView dice1, dice2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        rollButton = findViewById(R.id.game_roll_dice_button);
        finishButton = findViewById(R.id.game_finish_button);
        quitButton = findViewById(R.id.game_quit_button);
        dice1 = findViewById(R.id.dice_image_view_1);
        dice2 = findViewById(R.id.dice_image_view_2);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        for (int i = 1; i <= 12; i++) {  // Can be a TO-DO item later
            String _woodenCardID = "wooden_card_" + i;
            int _resourceID = getResources().getIdentifier(_woodenCardID, "id", getPackageName());
            woodenCards[i] = (Button) findViewById(_resourceID);
            woodenCards[i].setBackgroundColor(Color.MAGENTA);

            int finalI = i;
            woodenCards[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "button clicked: " + String.valueOf(finalI));
                    woodenCards[finalI].setBackgroundColor(Color.GRAY);
                }
            });
        }

        rollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody rolls the dice");
                rollDice(dice1);
                rollDice(dice2);
            }
        });

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody finish marking wooden cards");
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody quits the game");
                Intent intent = new Intent(getApplicationContext(), PostGameActivity.class);
                intent.putExtra(LOSER_NAME, "player_one");
                startActivity(intent);
            }
        });
    }

    private void rollDice(ImageView dice) {
        int randomNumber = random.nextInt(6) + 1;
        switch (randomNumber) {
            case 1:
                dice.setImageResource(R.drawable.dice1);
                break;
            case 2:
                dice.setImageResource(R.drawable.dice2);
                break;
            case 3:
                dice.setImageResource(R.drawable.dice3);
                break;
            case 4:
                dice.setImageResource(R.drawable.dice4);
                break;
            case 5:
                dice.setImageResource(R.drawable.dice5);
                break;
            case 6:
                dice.setImageResource(R.drawable.dice6);
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double currentX = event.values[0];
        double currentY = event.values[1];
        double currentZ = event.values[2];

        if (lastInitialized) {
            double diffX = Math.abs(lastX - currentX);
            double diffY = Math.abs(lastY - currentY);
            double diffZ = Math.abs(lastZ - currentZ);

            if ((diffX > SHAKE_THRESHOLD) || (diffY > SHAKE_THRESHOLD) || (diffZ > SHAKE_THRESHOLD)) {
                Log.d(TAG, "Somebody rolls the dice");
                rollDice(dice1);
                rollDice(dice2);
            }
        }

        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        lastInitialized = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /**
         * This method belongs to the SensorEventListener
         * We don't have to implement this method now.
         */
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (accelerometer != null) {
            sensorManager.unregisterListener(this);
        }
    }
}