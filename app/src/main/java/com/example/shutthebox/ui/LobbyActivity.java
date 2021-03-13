package com.example.shutthebox.ui;

import androidx.fragment.app.Fragment;

public class LobbyActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new LobbyFragment();
    }
}
