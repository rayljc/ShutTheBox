package com.example.shutthebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GameRoom extends AppCompatActivity {

    private static final String TAG = "TAG_GAME_ROOM";
    private static final String GAME_ROOM_ID = "GAME_ROOM_ID";
    TextView gameRoomNumberText;
    Button readyButton, startGameButton, leaveRoomButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_room);

        gameRoomNumberText = findViewById(R.id.game_room_number_text);
        readyButton = findViewById(R.id.room_ready_button);
        startGameButton = findViewById(R.id.room_start_button);
        leaveRoomButton = findViewById(R.id.room_leave_button);

        String _game_room_number = "Game room " + getIntent().getExtras().getString("game_room_number", "zero");
        gameRoomNumberText.setText(_game_room_number);

        readyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody is ready");
            }
        });

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody wants to start the game");
                Intent intent = new Intent(getApplicationContext(), Game.class);
                intent.putExtra(GAME_ROOM_ID, 1);
                startActivity(intent);
            }
        });

        leaveRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody leaves the room");
                Intent intent = new Intent(getApplicationContext(), Lobby.class);
                // Notify Firestore that someone leaves the room and refresh everyone's UI
                startActivity(intent);
            }
        });

    }
}