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

import com.example.shutthebox.R;
import com.example.shutthebox.model.Player;
import com.example.shutthebox.model.Room;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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

public class LobbyFragment extends Fragment {

    private static final String TAG = "TAG_LOBBY";
    private static final String GAME_ROOM_NO = "game_room_number";
    Button joinRoomOneButton, joinRoomTwoButton, switchAccountButton;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    TextView roomTitle_1, roomName_1, roomCurrentPlayers_1, roomAvailable_1;
    TextView roomTitle_2, roomName_2, roomCurrentPlayers_2, roomAvailable_2;
    Player player;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_lobby, container, false);

        Activity activity = requireActivity();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        joinRoomOneButton = view.findViewById(R.id.join_room_1_button);
        joinRoomTwoButton = view.findViewById(R.id.join_room_2_button);
        switchAccountButton = view.findViewById(R.id.lobby_switch_account_button);

        roomTitle_1 = view.findViewById(R.id.game_room_1_title);
        roomName_1 = view.findViewById(R.id.game_room_1_name);
        roomCurrentPlayers_1 = view.findViewById(R.id.game_room_1_cp);
        roomAvailable_1 = view.findViewById(R.id.game_room_1_status);
        roomTitle_2 = view.findViewById(R.id.game_room_2_title);
        roomName_2 = view.findViewById(R.id.game_room_2_name);
        roomCurrentPlayers_2 = view.findViewById(R.id.game_room_2_cp);
        roomAvailable_2 = view.findViewById(R.id.game_room_2_status);

        firebaseFirestore.collection("users").document(
                firebaseAuth.getCurrentUser().getUid()
        ).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    player = task.getResult().toObject(Player.class);
                }
            }
        });

        DocumentReference docRefRoom1 = firebaseFirestore.collection("rooms").document("room_1");
        docRefRoom1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room1 = task.getResult().toObject(Room.class);
                    assert room1 != null;
                    String gameName = "Game: " + room1.getGameName();
                    String statusText = room1.getAvailable() ? "Status: available" : "Status: unavailable";
                    List<Player> players = room1.getPlayers();
                    String numberOfPlayersText = "Current Players: " + players.size();
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
                    assert room1 != null;
                    String statusText = room1.getAvailable() ? "Status: available" : "Status: unavailable";
                    List<Player> players = room1.getPlayers();
                    String numberOfPlayersText = "Current Players: " + players.size();
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
                Intent intent = new Intent(activity.getApplicationContext(), GameRoomActivity.class);
                intent.putExtra(GAME_ROOM_NO, "room_1");
                addPlayerToGameRoom("room_1", player);
                startActivity(intent);
            }
        });

        // This button is only skin deep LOL
        joinRoomTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity.getApplicationContext(), GameRoomActivity.class);
                intent.putExtra(GAME_ROOM_NO, "room_2");
                startActivity(intent);
            }
        });

        switchAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(activity.getApplicationContext(), LoginActivity.class));
                activity.finish();
            }
        });

        return view;
    }

    private void addPlayerToGameRoom(String roomID, @NonNull Player player) {
        DocumentReference docRefRoom = firebaseFirestore.collection("rooms").document(roomID);
        docRefRoom.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room = task.getResult().toObject(Room.class);
                    List<Player> players = room.getPlayers();
                    if (players == null || players.size() == 0) {
                        players = new ArrayList<>();
                    }
                    if (players.size() >= 5) {
                        players = new ArrayList<>();
                    }
                    players.add(player);
                    Map<String, Object> map = new HashMap<>();
                    map.put("players", players);

                    docRefRoom.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "updated success");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "updated fail");
                        }
                    });
                }
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
