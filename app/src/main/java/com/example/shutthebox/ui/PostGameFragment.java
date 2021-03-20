package com.example.shutthebox.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shutthebox.R;

public class PostGameFragment extends Fragment {

    private static final String LOSER_NAME = "LOSER_NAME";
    Button goToLobbyButton;
    TextView loserNameText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_game, container, false);

        Activity activity = requireActivity();

        loserNameText = view.findViewById(R.id.end_of_game_loser_name);
        goToLobbyButton = view.findViewById(R.id.go_to_lobby_button);

        String loserName = activity.getIntent().getExtras().getString(LOSER_NAME);
        loserNameText.setText(loserName);

        goToLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(activity.getApplicationContext(), LobbyActivity.class));
            }
        });

        return view;
    }
}
