package com.example.shutthebox.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shutthebox.Game;
import com.example.shutthebox.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class GameRoomFragment extends Fragment {

    private static final String TAG = "TAG_GAME_ROOM";
    private static final String GAME_ROOM_ID = "GAME_ROOM_ID";
    TextView gameRoomNumberText;
    Button readyButton, startGameButton, leaveRoomButton;
    FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_game_room, container, false);

        Activity activity = requireActivity();

        gameRoomNumberText = view.findViewById(R.id.game_room_number_text);
        readyButton = view.findViewById(R.id.room_ready_button);
        startGameButton = view.findViewById(R.id.room_start_button);
        leaveRoomButton = view.findViewById(R.id.room_leave_button);
        firebaseFirestore = FirebaseFirestore.getInstance();

        String _game_room_number = "Game room " + activity.getIntent().getExtras().getString("game_room_number", "zero");
        gameRoomNumberText.setText(_game_room_number);

        CollectionReference collectionReference = firebaseFirestore.collection("rooms");


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
                Intent intent = new Intent(activity.getApplicationContext(), Game.class);
                intent.putExtra(GAME_ROOM_ID, 1);
                startActivity(intent);
            }
        });

        leaveRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody leaves the room");
                Intent intent = new Intent(activity.getApplicationContext(), LobbyActivity.class);
                // Notify Firestore that someone leaves the room and refresh everyone's UI
                startActivity(intent);
            }
        });

        return view;
    }
}
