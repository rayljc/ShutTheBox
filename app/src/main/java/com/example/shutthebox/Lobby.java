package com.example.shutthebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.shutthebox.model.Player;
import com.example.shutthebox.model.Room;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lobby extends AppCompatActivity {

    private static final String TAG = "TAG_LOBBY";
    private static final String GAME_ROOM_NO = "game_room_number";
    Button joinRoomOneButton, joinRoomTwoButton, switchAccountButton;
    FirebaseFirestore firebaseFirestore;
    TextView roomTitle_1, roomName_1, roomCurrentPlayers_1, roomAvailable_1;
    TextView roomTitle_2, roomName_2, roomCurrentPlayers_2, roomAvailable_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        firebaseFirestore = FirebaseFirestore.getInstance();
        joinRoomOneButton = findViewById(R.id.join_room_1_button);
        joinRoomTwoButton = findViewById(R.id.join_room_2_button);
        switchAccountButton = findViewById(R.id.lobby_switch_account_button);

        roomTitle_1 = findViewById(R.id.game_room_1_title);
        roomName_1 = findViewById(R.id.game_room_1_name);
        roomCurrentPlayers_1 = findViewById(R.id.game_room_1_cp);
        roomAvailable_1 = findViewById(R.id.game_room_1_status);
        roomTitle_2 = findViewById(R.id.game_room_2_title);
        roomName_2 = findViewById(R.id.game_room_2_name);
        roomCurrentPlayers_2 = findViewById(R.id.game_room_2_cp);
        roomAvailable_2 = findViewById(R.id.game_room_2_status);

        DocumentReference docRefRoom1 = firebaseFirestore.collection("rooms").document("room_1");
        docRefRoom1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room1 = task.getResult().toObject(Room.class);
                    String gameName = room1.getGameName();
                    String statusText = room1.getAvailable() ? "Status: available" : "Status: unavailable";
                    List<Player> players = room1.getPlayers();
                    String numberOfPlayersText = "Players: " + players.size();
                    roomName_1.setText(gameName);
                    roomAvailable_1.setText(statusText);
                    roomCurrentPlayers_1.setText(numberOfPlayersText);

                }
            }
        });

        docRefRoom1.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.d(TAG, "Event listener for room 1 failed");
                }

                if (value != null && value.exists()) {
                    Log.d(TAG, "Current room 1 data" + value.getData());
                    Room room1 = value.toObject(Room.class);
                    String statusText = room1.getAvailable() ? "Status: available" : "Status: unavailable";
                    List<Player> players = room1.getPlayers();
                    String numberOfPlayersText = "Players: " + players.size();
                    // Only update player numbers and room status
                    roomAvailable_1.setText(statusText);
                    roomCurrentPlayers_1.setText(numberOfPlayersText);
                } else {
                    Log.d(TAG, "Current room 1 data is null or not exists");
                }
            }
        });

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

    @Deprecated
    private void createRoomsForTheFirstTime() {
        /**
         * This method is deprecated now. It's for reference only.
         */
        CollectionReference collectionReference = firebaseFirestore.collection("rooms");
        if (firebaseFirestore != null) {
            Map<String, Object> room = new HashMap<>();
            room.put("id", 1);
            room.put("players", new ArrayList<>());
            room.put("game_name", "Shut The Box");
            room.put("available", true);
            collectionReference.document("room_1").set(room);
        }

        if (firebaseFirestore != null) {
            Map<String, Object> room = new HashMap<>();
            room.put("id", 2);
            room.put("players", new ArrayList<>());
            room.put("game_name", "Shut The Box");
            room.put("available", true);
            collectionReference.document("room_2").set(room);
        }
    }

    private void createRoomForTheFirstTime2() {
        /**
         * Do not execute this method. Those data is already there now!
         */
        CollectionReference collectionReference = firebaseFirestore.collection("rooms");
        if (firebaseFirestore != null) {
            Room room = new Room("room_1", new ArrayList<Player>(), "Shut the box", true);
            collectionReference.document("room_1").set(room);
        }

        if (firebaseFirestore != null) {
            Room room = new Room("room_2", new ArrayList<Player>(), "Shut the box", false);
            collectionReference.document("room_2").set(room);
        }
    }
}