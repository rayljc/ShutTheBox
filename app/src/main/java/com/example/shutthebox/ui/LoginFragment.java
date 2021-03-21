package com.example.shutthebox.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shutthebox.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private static final String TAG = "TAG_LOGIN";
    EditText email, password;
    Button loginButton;
    TextView createAccountText;
    FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_main, container, false);

        Activity activity = requireActivity();

        email = view.findViewById(R.id.emailEditText);
        password = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        createAccountText = view.findViewById(R.id.createAccountText);
        firebaseAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(v -> {
            String _email = email.getText().toString().trim();
            String _password = password.getText().toString().trim();

            if (TextUtils.isEmpty(_email)) {
                email.setError("Email is required!");
                return;
            }

            if (_password.length() < 8) {
                password.setError("Password must be at least 8 digits!");
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(_email, _password).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Log.d(TAG, "Login success");
                    startActivity(new Intent(activity.getApplicationContext(), LobbyActivity.class));
                } else {
                    Log.d(TAG, "Login Failed: " + task.getException().getMessage());
                    Toast.makeText(activity.getApplicationContext(), "Login failed! Please check your email and password", Toast.LENGTH_SHORT).show();
                }
            });
        });

        createAccountText.setOnClickListener(v -> startActivity(new Intent(activity.getApplicationContext(), RegisterActivity.class)));

        return view;
    }
}
