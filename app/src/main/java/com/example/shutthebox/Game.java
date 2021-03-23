package com.example.shutthebox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shutthebox.model.GameEntry;
import com.example.shutthebox.model.Player;
import com.example.shutthebox.model.WoodenCard;
import com.example.shutthebox.ui.PostGameActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "TAG_GAME";
    private static final String LOSER_NAME = "LOSER_NAME";
    private static final String GAME_ENTRY_ID = "GAME_ENTRY_ID";
    private static final String USERS_COLLECTION = "users";
    private static final String GAMES_COLLECTION = "games";
    private static final double SHAKE_THRESHOLD = 5.0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double lastX, lastY, lastZ;
    private boolean lastInitialized = false;
    private final Random random = new Random();
    private final Button[] woodenCards = new Button[13];  // 13 instead of 12, can be a TO-DO item later
    private ImageView dice1, dice2;
    private TextView playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText;
    private boolean gameInitialized = false;
    private Player player;  // invariant got from cloud db
    private String gameEntryID;  // invariant got from cloud db
    private List<Player> playerList;  // invariant got from cloud db
    private int currentPlayerIndex = 0;  // Use as a copy of cloud db state
    private boolean[] woodenCardsMarked = new boolean[13];  // Use as a copy of cloud db state
    private boolean diceRolled = false;  // variable specific to each player
    private int diceOnePoint = 1;  // variable specific to each player, used in game result checking
    private int diceTwoPoint = 6;  // variable specific to each player, used in game result checking
    private List<Integer> currentMarkedCards = new ArrayList<>();  // variable specific to each player, used in game result checking
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        Button rollButton = findViewById(R.id.game_roll_dice_button);
        Button finishButton = findViewById(R.id.game_finish_button);
        Button quitButton = findViewById(R.id.game_quit_button);
        dice1 = findViewById(R.id.dice_image_view_1);
        dice2 = findViewById(R.id.dice_image_view_2);

        playerOneNameText = findViewById(R.id.game_player_name_one);
        playerTwoNameText = findViewById(R.id.game_player_name_two);
        playerThreeNameText = findViewById(R.id.game_player_name_three);
        playerFourNameText = findViewById(R.id.game_player_name_four);

        // Get current user profile
        firebaseFirestore.collection(USERS_COLLECTION).document(
                firebaseAuth.getCurrentUser().getUid()
        ).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                player = task.getResult().toObject(Player.class);
            }
        });

        // GameEntry specific info
        gameEntryID = getIntent().getExtras().getString(GAME_ENTRY_ID, "");

        // Initialize the page using GameEntry info
        final DocumentReference gameEntryDocRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        gameEntryDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final GameEntry entry = task.getResult().toObject(GameEntry.class);
                assert entry != null;
                playerList = entry.getPlayers();
                setPlayersNameOnView(playerList,
                        Arrays.asList(playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText));
                currentPlayerIndex = entry.getPlayerTurnIndex();
                setCurrentPlayersBackground(currentPlayerIndex,
                        Arrays.asList(playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText));
            }
        });

        // Add GameEntry listener
        gameEntryDocRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "Event listener for GameEntry failed");
            }

            if (value != null && value.exists()) {
                final GameEntry entry = value.toObject(GameEntry.class);
                assert entry != null;

                final Player loser = entry.getLoser();
                if (loser != null) {
                    goToPostGamePage(loser);
                }

                refreshDiceImage(dice1, entry.getDice1());
                refreshDiceImage(dice2, entry.getDice2());

                for (WoodenCard woodenCard : entry.getWoodenCards()) {
                    if (woodenCard.isMarked()) {
                        woodenCardsMarked[woodenCard.getNumber()] = true;
                        woodenCards[woodenCard.getNumber()].setBackgroundColor(Color.GRAY);
                    }
                }

                currentPlayerIndex = entry.getPlayerTurnIndex();
                setCurrentPlayersBackground(currentPlayerIndex,
                        Arrays.asList(playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText));

            } else {
                Log.d(TAG, "Current GameEntry is null or not exists");
            }
        });

        for (int i = 1; i <= 12; i++) {  // Can be a TO-DO item later
            final String _woodenCardID = "wooden_card_" + i;
            int _resourceID = getResources().getIdentifier(_woodenCardID, "id", getPackageName());
            woodenCards[i] = findViewById(_resourceID);
            woodenCards[i].setBackgroundColor(Color.MAGENTA);

            int finalI = i;
            woodenCards[i].setOnClickListener(v -> {
                if (playerList.indexOf(player) != currentPlayerIndex) {
                    return;
                }
                if (!diceRolled) {
                    return;
                }
                if (woodenCardsMarked[finalI]) {
                    return;
                }

                Log.d(TAG, "button clicked: " + finalI);
                currentMarkedCards.add(finalI);

                updateWoodenCardState(finalI);
            });
        }

        rollButton.setOnClickListener(v -> roll());

        finishButton.setOnClickListener(v -> {
            if (playerList.indexOf(player) != currentPlayerIndex) {
                return;
            }
            if (!diceRolled) {
                return;
            }

            // Check the result
            boolean isLegal = checkResult(diceOnePoint, diceTwoPoint, currentMarkedCards);
            if (!isLegal) {
                updateLoserInfo();  // update Loser info and the listener would do the rest.
            } else {
                nextRound();
                diceRolled = false;
                currentMarkedCards = new ArrayList<>();
            }
        });

        quitButton.setOnClickListener(v -> {
            Log.d(TAG, "Somebody quits the game");
            updateLoserInfo();
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        gameInitialized = true;
    }

    private void goToPostGamePage(@NonNull final Player loser) {
        Intent intent = new Intent(getApplicationContext(), PostGameActivity.class);
        intent.putExtra(LOSER_NAME, loser.getDisplayName());
        startActivity(intent);
    }

    private void updateWoodenCardState(int finalI) {
        final DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final GameEntry entry = task.getResult().toObject(GameEntry.class);
                assert entry != null;
                final List<WoodenCard> woodenCards = entry.getWoodenCards();
                int index = finalI - 1;  // Could be a TO-DO item
                woodenCards.get(index).setMarked(true);

                final Map<String, Object> map = new HashMap<>();
                map.put("woodenCards", woodenCards);

                docRef.update(map).addOnCompleteListener(task1 -> Log.d(TAG, "updated success"))
                        .addOnFailureListener(e -> Log.d(TAG, "updated failed"));
            }
        });
    }

    private void updateLoserInfo() {
        final DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        final Map<String, Object> map = new HashMap<>();
        map.put("loser", player);
        docRef.update(map).addOnCompleteListener(task -> Log.d(TAG, "updated success"))
                .addOnFailureListener(e -> Log.d(TAG, "updated failed"));
    }

    private boolean checkResult(int diceOnePoint, int diceTwoPoint, @NonNull final List<Integer> currentMarkedCards) {
        int sum = currentMarkedCards.stream().reduce(0, Integer::sum);
        return sum == diceOnePoint + diceTwoPoint;
    }

    private void roll() {
        if (playerList.indexOf(player) != currentPlayerIndex) {
            return;
        }
        if (diceRolled) {
            return;
        }

        diceOnePoint = rollDice();
        diceTwoPoint = rollDice();

        updateDicePoints(diceOnePoint, diceTwoPoint);

        diceRolled = true;
    }

    private void updateDicePoints(int diceOnePoint, int diceTwoPoint) {
        final DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        final Map<String, Object> map = new HashMap<>();
        map.put("dice1", diceOnePoint);
        map.put("dice2", diceTwoPoint);
        docRef.update(map).addOnCompleteListener(task -> Log.d(TAG, "updated success"))
                .addOnFailureListener(e -> Log.d(TAG, "updated failed"));
    }

    private int rollDice() {
        return random.nextInt(6) + 1;
    }

    private void refreshDiceImage(@NonNull final ImageView dice, int point) {
        switch (point) {
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
            default:
                break;
        }
    }

    private void setPlayersNameOnView(@NonNull final List<Player> players, @NonNull final List<TextView> textViews) {
        if (players.size() > textViews.size()) {
            Log.d(TAG, "Too many players! Only at most 4 players can be in a game");
            return;
        }

        for (int i = 0; i < players.size(); i++) {
            textViews.get(i).setText(players.get(i).getDisplayName());
            textViews.get(i).setVisibility(View.VISIBLE);
        }

        for(int j = players.size(); j < textViews.size(); j++) {
            textViews.get(j).setVisibility(View.INVISIBLE);
        }
    }

    private void setCurrentPlayersBackground(int index, @NonNull final List<TextView> textViews) {
        for (int i = 0; i < textViews.size(); i++) {
            if (i == index) {
                textViews.get(i).setBackgroundColor(Color.GREEN);
            } else {
                textViews.get(i).setBackgroundColor(Color.YELLOW);
            }
        }
    }

    private void nextRound() {
        final DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                final GameEntry entry = task.getResult().toObject(GameEntry.class);
                assert entry != null;
                int index = entry.getPlayerTurnIndex();
                index++;
                if (index >= entry.getPlayers().size()) {
                    index = 0;
                }

                final Map<String, Object> map = new HashMap<>();
                map.put("playerTurnIndex", index);
                docRef.update(map).addOnCompleteListener(task1 -> Log.d(TAG, "updated success"))
                        .addOnFailureListener(e -> Log.d(TAG, "updated failed"));
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (playerList == null || player == null || !gameInitialized) {
            return;
        }

        if (playerList.indexOf(player) != currentPlayerIndex) {
            return;
        }

        double currentX = event.values[0];
        double currentY = event.values[1];
        double currentZ = event.values[2];

        if (lastInitialized) {
            double diffX = Math.abs(lastX - currentX);
            double diffY = Math.abs(lastY - currentY);
            double diffZ = Math.abs(lastZ - currentZ);

            if ((diffX > SHAKE_THRESHOLD) || (diffY > SHAKE_THRESHOLD) || (diffZ > SHAKE_THRESHOLD)) {
                roll();
            }
        }

        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        lastInitialized = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /**
         * This method belongs to the SensorEventListener
         * We don't have to implement this method now.
         */
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (accelerometer != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
