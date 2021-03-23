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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyFragment extends Fragment {

    private static final String TAG = "TAG_LOBBY";
    private static final String GAME_ROOM_NO = "game_room_number";
    private static final String USERS_COLLECTION = "users";
    private static final String ROOMS_COLLECTION = "rooms";
    private static final String ROOM_ONE = "room_1";
    private static final int MAX_PLAYER_NUMBER = 4;
    private TextView roomName_1;
    private TextView roomCurrentPlayers_1;
    private TextView roomAvailable_1;
    private Player player;
    private Boolean room1Available;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_lobby, container, false);

        Activity activity = requireActivity();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        Button joinRoomOneButton = view.findViewById(R.id.join_room_1_button);
        Button switchAccountButton = view.findViewById(R.id.lobby_switch_account_button);

        roomName_1 = view.findViewById(R.id.game_room_1_name);
        roomCurrentPlayers_1 = view.findViewById(R.id.game_room_1_cp);
        roomAvailable_1 = view.findViewById(R.id.game_room_1_status);

        // Get current user profile
        firebaseFirestore.collection(USERS_COLLECTION).document(
                firebaseAuth.getCurrentUser().getUid()
        ).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                player = task.getResult().toObject(Player.class);
            }
        });

        // Initialize the lobby page using room_1 info
        final DocumentReference docRefRoom1 = firebaseFirestore.collection(ROOMS_COLLECTION).document(ROOM_ONE);
        docRefRoom1.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final Room room1 = task.getResult().toObject(Room.class);
                assert room1 != null;
                final String gameName = "Game: " + room1.getGameName();
                final List<Player> players = room1.getPlayers();
                final String numberOfPlayersText = "Current Players: " + players.size();
                final String statusText = room1.getAvailable() ? "Status: available" : "Status: unavailable";
                room1Available = room1.getAvailable();
                roomName_1.setText(gameName);
                roomAvailable_1.setText(statusText);
                roomCurrentPlayers_1.setText(numberOfPlayersText);
            }
        });

        // Add room_1 listener
        docRefRoom1.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "Event listener for room 1 failed");
            }

            if (value != null && value.exists()) {
                Log.d(TAG, "Current room 1 data" + value.getData());
                final Room room1 = value.toObject(Room.class);
                assert room1 != null;
                final List<Player> players = room1.getPlayers();
                final String numberOfPlayersText = "Current Players: " + players.size();
                if (players.size() >= MAX_PLAYER_NUMBER) {
                    final Map<String, Object> map = new HashMap<>();
                    map.put("available", false);
                    docRefRoom1.update(map).addOnCompleteListener(task -> Log.d(TAG, "room_1 status set to unavailable"))
                            .addOnFailureListener(e -> Log.d(TAG, "Failed to update room_1 status"));
                } else {
                    final Map<String, Object> map = new HashMap<>();
                    map.put("available", true);
                    docRefRoom1.update(map).addOnCompleteListener(task -> Log.d(TAG, "room_1 status set to available"))
                            .addOnFailureListener(e -> Log.d(TAG, "Failed to update room_1 status"));
                }

                final String statusText = room1.getAvailable() ? "Status: available" : "Status: unavailable";
                room1Available = room1.getAvailable();

                // Only update player numbers and room status
                roomAvailable_1.setText(statusText);
                roomCurrentPlayers_1.setText(numberOfPlayersText);
            } else {
                Log.d(TAG, "Current room 1 data is null or not exists");
            }
        });

        joinRoomOneButton.setOnClickListener(v -> {
            if (!room1Available) {
                return;
            }
            Intent intent = new Intent(activity.getApplicationContext(), GameRoomActivity.class);
            intent.putExtra(GAME_ROOM_NO, ROOM_ONE);
            addPlayerToGameRoom(ROOM_ONE, player);
            startActivity(intent);
        });

        // Switch account. Sign out and return to login page.
        switchAccountButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(activity.getApplicationContext(), LoginActivity.class));
            activity.finish();
        });

        return view;
    }


    private void addPlayerToGameRoom(@NonNull final String roomID, @NonNull final Player player) {
        final DocumentReference docRefRoom = firebaseFirestore.collection(ROOMS_COLLECTION).document(roomID);
        docRefRoom.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final Room room = task.getResult().toObject(Room.class);
                assert room != null;
                List<Player> players = room.getPlayers();
                if (players == null || players.size() == 0) {
                    players = new ArrayList<>();
                }
                players.add(player);
                final Map<String, Object> map = new HashMap<>();
                map.put("players", players);

                docRefRoom.update(map).addOnCompleteListener(task1 -> Log.d(TAG, "updated success"))
                        .addOnFailureListener(e -> Log.d(TAG, "updated fail"));
            }
        });
    }

    private void createRoomForTheFirstTime2() {
        /**
         * Do not execute this method. Those data is already there now!
         */
        final CollectionReference collectionReference = firebaseFirestore.collection(ROOMS_COLLECTION);
        if (firebaseFirestore != null) {
            final Room room = new Room("room_1", new ArrayList<Player>(), "Shut the box", true, "");
            collectionReference.document("room_1").set(room);
        }

        if (firebaseFirestore != null) {
            final Room room = new Room("room_2", new ArrayList<Player>(), "Shut the box", false, "");
            collectionReference.document("room_2").set(room);
        }
    }
}
