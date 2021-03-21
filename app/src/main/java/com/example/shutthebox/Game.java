package com.example.shutthebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

import com.example.shutthebox.model.GameEntry;
import com.example.shutthebox.model.Player;
import com.example.shutthebox.model.WoodenCard;
import com.example.shutthebox.ui.PostGameActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "TAG_GAME";
    private static final String LOSER_NAME = "LOSER_NAME";
    private static final String GAME_ENTRY_ID = "GAME_ENTRY_ID";
    private static final String GAMES_COLLECTION = "games";
    private static final double SHAKE_THRESHOLD = 5.0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private double lastX, lastY, lastZ;
    private boolean lastInitialized = false;

    Random random = new Random();
    Button rollButton, finishButton, quitButton;
    Button[] woodenCards = new Button[13];  // 13 instead of 12, can be a TO-DO item later
    ImageView dice1, dice2;
    TextView playerOneNameText, playerTwoNameText, playerThreeNameText, playerFourNameText;
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        rollButton = findViewById(R.id.game_roll_dice_button);
        finishButton = findViewById(R.id.game_finish_button);
        quitButton = findViewById(R.id.game_quit_button);
        dice1 = findViewById(R.id.dice_image_view_1);
        dice2 = findViewById(R.id.dice_image_view_2);

        playerOneNameText = findViewById(R.id.game_player_name_one);
        playerTwoNameText = findViewById(R.id.game_player_name_two);
        playerThreeNameText = findViewById(R.id.game_player_name_three);
        playerFourNameText = findViewById(R.id.game_player_name_four);

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

        //++ GameEntry specific info
        gameEntryID = getIntent().getExtras().getString(GAME_ENTRY_ID, "");
        //-- GameEntry specific info

        // Initialize the page using GameEntry info
        DocumentReference gameEntryDocRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        gameEntryDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    GameEntry entry = task.getResult().toObject(GameEntry.class);
                    assert entry != null;
                    playerList = entry.getPlayers();
                    setPlayersNameOnView(playerList);
                    currentPlayerIndex = entry.getPlayerTurnIndex();
                    setCurrentPlayerBackground(currentPlayerIndex);
                }
            }
        });

        // Add GameEntry listener
        gameEntryDocRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.d(TAG, "Event listener for GameEntry failed");
                }

                if (value != null && value.exists()) {
                    GameEntry entry = value.toObject(GameEntry.class);
                    assert entry != null;

                    Player loser = entry.getLoser();
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
                    setCurrentPlayerBackground(currentPlayerIndex);

                } else {
                    Log.d(TAG, "Current GameEntry is null or not exists");
                }
            }
        });

        for (int i = 1; i <= 12; i++) {  // Can be a TO-DO item later
            String _woodenCardID = "wooden_card_" + i;
            int _resourceID = getResources().getIdentifier(_woodenCardID, "id", getPackageName());
            woodenCards[i] = (Button) findViewById(_resourceID);
            woodenCards[i].setBackgroundColor(Color.MAGENTA);

            int finalI = i;
            woodenCards[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (playerList.indexOf(player) != currentPlayerIndex) {
                        return;
                    }
                    if (!diceRolled) {
                        return;
                    }
                    if (woodenCardsMarked[finalI]) {
                        return;
                    }

                    Log.d(TAG, "button clicked: " + String.valueOf(finalI));
                    currentMarkedCards.add(finalI);

                    updateWoodenCardState(finalI);
                }
            });
        }

        rollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roll();
            }
        });

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Somebody quits the game");
                updateLoserInfo();
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        gameInitialized = true;
    }

    private void goToPostGamePage(Player loser) {
        Intent intent = new Intent(getApplicationContext(), PostGameActivity.class);
        intent.putExtra(LOSER_NAME, loser.getDisplayName());
        startActivity(intent);
    }

    private void updateWoodenCardState(int finalI) {
        DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    GameEntry entry = task.getResult().toObject(GameEntry.class);
                    assert entry != null;
                    List<WoodenCard> woodenCards = entry.getWoodenCards();
                    int index = finalI - 1;  // Could be a TO-DO item
                    woodenCards.get(index).setMarked(true);

                    Map<String, Object> map = new HashMap<>();
                    map.put("woodenCards", woodenCards);

                    docRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
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

    private void updateLoserInfo() {
        DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        Map<String, Object> map = new HashMap<>();
        map.put("loser", player);
        docRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
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

    private boolean checkResult(int diceOnePoint, int diceTwoPoint, List<Integer> currentMarkedCards) {
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
        DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        Map<String, Object> map = new HashMap<>();
        map.put("dice1", diceOnePoint);
        map.put("dice2", diceTwoPoint);
        docRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
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

    private int rollDice() {
        return random.nextInt(6) + 1;
    }

    private void refreshDiceImage(ImageView dice, int point) {
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

    private void setCurrentPlayerBackground(int index) {
        switch (index) {
            case 0:
                playerOneNameText.setBackgroundColor(Color.GREEN);
                playerTwoNameText.setBackgroundColor(Color.YELLOW);
                playerThreeNameText.setBackgroundColor(Color.YELLOW);
                playerFourNameText.setBackgroundColor(Color.YELLOW);
                break;
            case 1:
                playerOneNameText.setBackgroundColor(Color.YELLOW);
                playerTwoNameText.setBackgroundColor(Color.GREEN);
                playerThreeNameText.setBackgroundColor(Color.YELLOW);
                playerFourNameText.setBackgroundColor(Color.YELLOW);
                break;
            case 2:
                playerOneNameText.setBackgroundColor(Color.YELLOW);
                playerTwoNameText.setBackgroundColor(Color.YELLOW);
                playerThreeNameText.setBackgroundColor(Color.GREEN);
                playerFourNameText.setBackgroundColor(Color.YELLOW);
                break;
            case 3:
                playerOneNameText.setBackgroundColor(Color.YELLOW);
                playerTwoNameText.setBackgroundColor(Color.YELLOW);
                playerThreeNameText.setBackgroundColor(Color.YELLOW);
                playerFourNameText.setBackgroundColor(Color.GREEN);
                break;
            default:
                break;
        }
    }

    private void nextRound() {
        DocumentReference docRef = firebaseFirestore.collection(GAMES_COLLECTION).document(gameEntryID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    GameEntry entry = task.getResult().toObject(GameEntry.class);
                    assert entry != null;
                    int index = entry.getPlayerTurnIndex();
                    index++;
                    if (index >= entry.getPlayers().size()) {
                        index = 0;
                    }

                    Map<String, Object> map = new HashMap<>();
                    map.put("playerTurnIndex", index);
                    docRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
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