package com.example.shutthebox.ui;

import androidx.fragment.app.Fragment;

public class PostGameActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new PostGameFragment();
    }
}
