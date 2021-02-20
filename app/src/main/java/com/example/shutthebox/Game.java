package com.example.shutthebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Random;

public class Game extends AppCompatActivity {

    private static final String TAG = "TAG_GAME";
    private static final String LOSER_NAME = "LOSER_NAME";
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
                Intent intent = new Intent(getApplicationContext(), PostGame.class);
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
}