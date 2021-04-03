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
import com.example.shutthebox.model.GameEntry;
import com.example.shutthebox.model.Player;
import com.example.shutthebox.model.Room;
import com.example.shutthebox.model.WoodenCard;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameRoomFragment extends Fragment {

    private static final String TAG = "TAG_GAME_ROOM";
    private static final String GAME_ROOM_ID = "GAME_ROOM_ID";
    private static final String GAME_ENTRY_ID = "GAME_ENTRY_ID";
    private static final String GAME_ROOM_NO = "game_room_number";
    private static final String PLAYER_ID = "player_id";
    private static final String USERS_COLLECTION = "users";
    private static final String ROOMS_COLLECTION = "rooms";
    private static final String GAMES_COLLECTION = "games";
    private boolean leaveButtonPressed = false;
    private TextView playerOneNameText, playerTwoNameText, playerThreeNameText;
    private Player player;
    private String gameRoomNumber;
    FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_game_room, container, false);

        Activity activity = requireActivity();

        TextView gameRoomNumberText = view.findViewById(R.id.game_room_number_text);

        playerOneNameText = view.findViewById(R.id.gr_player1_name);
        playerTwoNameText = view.findViewById(R.id.gr_player2_name);
        playerThreeNameText = view.findViewById(R.id.gr_player3_name);

        Button startGameButton = view.findViewById(R.id.room_start_button);
        Button leaveRoomButton = view.findViewById(R.id.room_leave_button);
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Room specific info
        gameRoomNumber = activity.getIntent().getExtras().getString(GAME_ROOM_NO, "");
        String welcomeToRoom = getString(R.string.welcome_text) + " " + gameRoomNumber;
        gameRoomNumberText.setText(welcomeToRoom);

        // Get current user profile
        final String playerID = activity.getIntent().getExtras().getString(PLAYER_ID, "");
        firebaseFirestore.collection(USERS_COLLECTION).document(playerID).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        player = task.getResult().toObject(Player.class);
                    }
                });

        // Initialize the room page using room_1 info
        final DocumentReference docRefRoom1 = firebaseFirestore.collection(ROOMS_COLLECTION).document("room_1");
        docRefRoom1.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final Room room1 = task.getResult().toObject(Room.class);
                assert room1 != null;
                final List<Player> players = room1.getPlayers();
                setPlayersNameOnView(players,
                        Arrays.asList(playerOneNameText, playerTwoNameText, playerThreeNameText));
            }
        });

        // Add room_1 listener
        docRefRoom1.addSnapshotListener((value, error) -> {
            if (leaveButtonPressed) {
                return;
            }

            if (error != null) {
                Log.d(TAG, "Event listener for room failed");
            }

            if (value != null && value.exists()) {
                final Room room1 = value.toObject(Room.class);
                assert room1 != null;
                final List<Player> players = room1.getPlayers();
                final String gameEntryID = room1.getGameEntryID();
                if (players.size() == 0) {
                    Intent intent = new Intent(activity.getApplicationContext(), Game.class);
                    intent.putExtra(GAME_ROOM_ID, 1);
                    intent.putExtra(GAME_ENTRY_ID, gameEntryID);
                    intent.putExtra(PLAYER_ID, playerID);
                    startActivity(intent);
                }

                setPlayersNameOnView(players,
                        Arrays.asList(playerOneNameText, playerTwoNameText, playerThreeNameText));

            } else {
                Log.d(TAG, "Current room data is null or not exists");
            }
        });

        startGameButton.setOnClickListener(v -> docRefRoom1.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final Room room = task.getResult().toObject(Room.class);
                assert room != null;
                final List<Player> players = room.getPlayers();

                // new a GameEntry
                final String gameEntryID = newGameEntry(players, gameRoomNumber);

                // Delete all players in the room and set gameEntryID
                final Map<String, Object> map = new HashMap<>();
                map.put("players", new ArrayList<>());
                map.put("gameEntryID", gameEntryID);
                docRefRoom1.update(map).addOnCompleteListener(task1 -> Log.d(TAG, "players and gameEntryID update succeeded"))
                        .addOnFailureListener(e -> Log.d(TAG, "players and gameEntryID update failed"));
            }
        }));

        // The user leaves the game room
        leaveRoomButton.setOnClickListener(v -> {
            leaveButtonPressed = true;
            Log.d(TAG, "Somebody leaves the room");
            Intent intent = new Intent(activity.getApplicationContext(), LobbyActivity.class);
            removePlayerFromGameRoom("room_1", player);
            startActivity(intent);
        });

        return view;
    }

    private String newGameEntry(@NonNull final List<Player> players, @NonNull final String gameRoomNumber) {
        final String id = UUID.randomUUID().toString();
        final List<WoodenCard> woodenCards = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            woodenCards.add(new WoodenCard(i, false));
        }

        final GameEntry gameEntry = new GameEntry(id, woodenCards, 1, 6,
                players, 0, null, gameRoomNumber);
        firebaseFirestore.collection(GAMES_COLLECTION).document(id).set(gameEntry);

        return id;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        assert player != null;
        removePlayerFromGameRoom(gameRoomNumber, player);
    }

    private void setPlayersNameOnView(@NonNull final List<Player> players, @NonNull final List<TextView> textViews) {
        if (players.size() > textViews.size()) {
            Log.d(TAG, "Too many players! Only at most 3 players can be in a game");
            return;
        }

        for (int i = 0; i < players.size(); i++) {
            textViews.get(i).setText(players.get(i).getDisplayName());
            textViews.get(i).setVisibility(View.VISIBLE);
        }

        for (int j = players.size(); j < textViews.size(); j++) {
            textViews.get(j).setVisibility(View.INVISIBLE);
        }
    }

    private void removePlayerFromGameRoom(@NonNull final String roomID, @NonNull final Player player) {
        final DocumentReference docRefRoom = firebaseFirestore.collection(ROOMS_COLLECTION).document(roomID);
        docRefRoom.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final Room room = task.getResult().toObject(Room.class);
                assert room != null;
                final List<Player> players = room.getPlayers();
                if (players == null || players.size() == 0) {
                    return;
                }
                players.remove(player);
                final Map<String, Object> map = new HashMap<>();
                map.put("players", players);

                docRefRoom.update(map).addOnCompleteListener(task1 -> Log.d(TAG, "players update succeeded"))
                        .addOnFailureListener(e -> Log.d(TAG, "players update failed"));
            }
        });
    }
}
