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

public class GameRoomFragment extends Fragment {

    private static final String TAG = "TAG_GAME_ROOM";
    private static final String GAME_ROOM_ID = "GAME_ROOM_ID";
    TextView gameRoomNumberText;
    TextView playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText;
    Button startGameButton, leaveRoomButton;
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;
    Player player;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_game_room, container, false);

        Activity activity = requireActivity();

        gameRoomNumberText = view.findViewById(R.id.game_room_number_text);

        playerOneNameText = view.findViewById(R.id.gr_player1_name);
        playerTwoNameText = view.findViewById(R.id.gr_player2_name);
        playerThreeNameText = view.findViewById(R.id.gr_player3_name);
        playerFourNameText = view.findViewById(R.id.gr_player4_name);

        startGameButton = view.findViewById(R.id.room_start_button);
        leaveRoomButton = view.findViewById(R.id.room_leave_button);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Get current user profile
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

        //++ Room specific info
        String _game_room_number = "Game room " + activity.getIntent().getExtras().getString("game_room_number", "zero");
        gameRoomNumberText.setText(_game_room_number);
        //-- Room specific info

        // Initialize the room page using room_1 info
        DocumentReference docRefRoom1 = firebaseFirestore.collection("rooms").document("room_1");
        docRefRoom1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room1 = task.getResult().toObject(Room.class);
                    assert room1 != null;
                    List<Player> players = room1.getPlayers();
//                    setPlayersNameOnView(players);
                }
            }
        });

        // Add room_1 listener
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
                    List<Player> players = room1.getPlayers();
//                    setPlayersNameOnView(players);
                } else {
                    Log.d(TAG, "Current room 1 data is null or not exists");
                }
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

        // The user leaves the game room
        leaveRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody leaves the room");
                Intent intent = new Intent(activity.getApplicationContext(), LobbyActivity.class);
                removePlayerFromGameRoom("room_1", player);
                startActivity(intent);
            }
        });

        return view;
    }

    /* java.lang.IndexOutOfBoundsException */
    private void setPlayersNameOnView(List<Player> players) {
        switch (players.size()) {
            case 0:
                Log.d(TAG, "Something bad happens: there is no player");
                break;
            case 1:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setVisibility(View.INVISIBLE);
                playerThreeNameText.setVisibility(View.INVISIBLE);
                playerFourNameText.setVisibility(View.INVISIBLE);
            case 2:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setText(players.get(1).getDisplayName());
                playerThreeNameText.setVisibility(View.INVISIBLE);
                playerFourNameText.setVisibility(View.INVISIBLE);
            case 3:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setText(players.get(1).getDisplayName());
                playerThreeNameText.setText(players.get(2).getDisplayName());
                playerFourNameText.setVisibility(View.INVISIBLE);
            case 4:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setText(players.get(1).getDisplayName());
                playerThreeNameText.setText(players.get(2).getDisplayName());
                playerFourNameText.setText(players.get(3).getDisplayName());
            default:
                Log.d(TAG, "Something bad happens: player size unknown");
                break;
        }
    }

    private void removePlayerFromGameRoom(String roomID, @NonNull Player player) {
        DocumentReference docRefRoom = firebaseFirestore.collection("rooms").document(roomID);
        docRefRoom.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room = task.getResult().toObject(Room.class);
                    List<Player> players = room.getPlayers();
                    if (players == null || players.size() == 0) {
                        return;
                    }
                    players.remove(player);  // Haven't been tested
                    Map<String, Object> map = new HashMap<>();
                    map.put("players", players);

                    docRefRoom.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "update success");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "update fail");
                        }
                    });
                }
            }
        });
    }
}
