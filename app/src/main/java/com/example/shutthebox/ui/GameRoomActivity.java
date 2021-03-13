package com.example.shutthebox.ui;

import androidx.fragment.app.Fragment;

public class GameRoomActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new GameRoomFragment();
    }
}
