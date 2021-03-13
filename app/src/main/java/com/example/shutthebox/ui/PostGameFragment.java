package com.example.shutthebox.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shutthebox.R;

public class PostGameFragment extends Fragment {

    Button goToLobbyButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_game, container, false);

        Activity activity = requireActivity();

        goToLobbyButton = view.findViewById(R.id.go_to_lobby_button);

        goToLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(activity.getApplicationContext(), LobbyActivity.class));
            }
        });

        return view;
    }
}
