package com.example.shutthebox.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
import com.example.shutthebox.model.GameEntry;
import com.example.shutthebox.model.Player;
import com.example.shutthebox.model.Room;
import com.example.shutthebox.model.WoodenCard;
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
import java.util.UUID;

public class GameRoomFragment extends Fragment {

    private static final String TAG = "TAG_GAME_ROOM";
    private static final String GAME_ROOM_ID = "GAME_ROOM_ID";
    private static final String GAME_ENTRY_ID = "GAME_ENTRY_ID";
    private static final String GAME_ROOM_NO = "game_room_number";
    private static final String ROOMS_COLLECTION = "rooms";
    TextView gameRoomNumberText;
    TextView playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText;
    Button startGameButton, leaveRoomButton;
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;
    Player player;
    String gameRoomNumber;

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
        gameRoomNumber = activity.getIntent().getExtras().getString(GAME_ROOM_NO, "");
        String welcomeToRoom = getString(R.string.welcome_text) + " " + gameRoomNumber;
        gameRoomNumberText.setText(welcomeToRoom);
        //-- Room specific info

        // Initialize the room page using room_1 info
        DocumentReference docRefRoom1 = firebaseFirestore.collection(ROOMS_COLLECTION).document("room_1");
        docRefRoom1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room1 = task.getResult().toObject(Room.class);
                    assert room1 != null;
                    List<Player> players = room1.getPlayers();
                    setPlayersNameOnView(players);
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
                    Room room1 = value.toObject(Room.class);
                    assert room1 != null;
                    List<Player> players = room1.getPlayers();
                    String gameEntryID = room1.getGameEntryID();

                    if (players.size() == 0) {
                        Intent intent = new Intent(activity.getApplicationContext(), Game.class);
                        intent.putExtra(GAME_ROOM_ID, 1);
                        intent.putExtra(GAME_ENTRY_ID, gameEntryID);
                        startActivity(intent);
                    }

                    setPlayersNameOnView(players);

                } else {
                    Log.d(TAG, "Current room 1 data is null or not exists");
                }
            }
        });

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                docRefRoom1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Room room = task.getResult().toObject(Room.class);
                            assert room != null;
                            List<Player> players = room.getPlayers();

                            // new a GameEntry
                            String gameEntryID = newGameEntry(players, gameRoomNumber);

                            // Delete all players in the room and set gameEntryID
                            Map<String, Object> map = new HashMap<>();
                            map.put("players", new ArrayList<>());
                            map.put("gameEntryID", gameEntryID);
                            docRefRoom1.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "updated success");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "updated failed");
                                }
                            });
                        }
                    }
                });
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

    private String newGameEntry(List<Player> players, String gameRoomNumber) {
        String id = UUID.randomUUID().toString();
        List<WoodenCard> woodenCards = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            woodenCards.add(new WoodenCard(i, false));
        }

        GameEntry gameEntry = new GameEntry(id, woodenCards, 1, 6,
                players, 0, null, gameRoomNumber);

        firebaseFirestore.collection("games").document(id).set(gameEntry);

        return id;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        assert player != null;
        removePlayerFromGameRoom(gameRoomNumber, player);
    }

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
                break;
            case 2:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setText(players.get(1).getDisplayName());
                playerThreeNameText.setVisibility(View.INVISIBLE);
                playerFourNameText.setVisibility(View.INVISIBLE);
                break;
            case 3:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setText(players.get(1).getDisplayName());
                playerThreeNameText.setText(players.get(2).getDisplayName());
                playerFourNameText.setVisibility(View.INVISIBLE);
                break;
            case 4:
                playerOneNameText.setText(players.get(0).getDisplayName());
                playerTwoNameText.setText(players.get(1).getDisplayName());
                playerThreeNameText.setText(players.get(2).getDisplayName());
                playerFourNameText.setText(players.get(3).getDisplayName());
                break;
            default:
                Log.d(TAG, "Something bad happens: player size unknown");
                break;
        }
    }

    private void removePlayerFromGameRoom(String roomID, @NonNull Player player) {
        DocumentReference docRefRoom = firebaseFirestore.collection(ROOMS_COLLECTION).document(roomID);
        docRefRoom.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Room room = task.getResult().toObject(Room.class);
                    List<Player> players = room.getPlayers();
                    if (players == null || players.size() == 0) {
                        return;
                    }
                    players.remove(player);
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
