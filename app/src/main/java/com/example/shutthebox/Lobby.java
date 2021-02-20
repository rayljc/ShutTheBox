package com.example.shutthebox;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Lobby extends AppCompatActivity {

    private static final String TAG = "TAG_LOBBY";
    private static final String GAME_ROOM_NO = "game_room_number";
    Button joinRoomOneButton, joinRoomTwoButton, switchAccountButton;
    FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        firebaseFirestore = FirebaseFirestore.getInstance();
        joinRoomOneButton = findViewById(R.id.join_room_1_button);
        joinRoomTwoButton = findViewById(R.id.join_room_2_button);
        switchAccountButton = findViewById(R.id.lobby_switch_account_button);

        joinRoomOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), GameRoom.class);
                intent.putExtra(GAME_ROOM_NO, "one");
                startActivity(intent);
            }
        });

        joinRoomTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), GameRoom.class);
                intent.putExtra(GAME_ROOM_NO, "two");
                startActivity(intent);
            }
        });

        switchAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        });
    }
}