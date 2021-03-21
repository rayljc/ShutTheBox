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
import com.example.shutthebox.model.Player;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterFragment extends Fragment {

    private static final String TAG = "TAG_REGISTER";
    EditText email, password, displayName;
    TextView backToLogin;
    Button registerButton;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_register, container, false);

        Activity activity = requireActivity();

        email = view.findViewById(R.id.createAccountEmailEditText);
        password = view.findViewById(R.id.createAccountPasswordEditText);
        displayName = view.findViewById(R.id.createAccountNameEditText);
        backToLogin = view.findViewById(R.id.back_to_login);
        registerButton = view.findViewById(R.id.registerButton);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.signOut();
        }

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String _email = email.getText().toString().trim();
                String _password = password.getText().toString().trim();
                String _displayName = displayName.getText().toString().trim();

                if (TextUtils.isEmpty(_email)) {
                    email.setError("Email is required!");
                    return;
                }

                if (_password.length() < 8) {
                    password.setError("Password must be at least 8 digits!");
                    return;
                }

                if (TextUtils.isEmpty(_displayName)) {
                    displayName.setError("Display name is required!");
                    return;
                }

                firebaseAuth.createUserWithEmailAndPassword(_email, _password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            firebaseUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(activity.getApplicationContext(), "Verification Email Has been Sent.", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "onFailure: Email not sent " + e.getMessage());
                                }
                            });

                            Toast.makeText(activity.getApplicationContext(), "User Created.", Toast.LENGTH_SHORT).show();
                            String userID = firebaseUser.getUid();
                            DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);
                            Player userProfile = new Player(_email, _displayName, 0);
                            documentReference.set(userProfile).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "User profile Saved: " + userID);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "Failed to save user profile: " + e.getMessage());
                                }
                            });
                        } else {
                            Toast.makeText(activity.getApplicationContext(), "Failed to create user: ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                startActivity(new Intent(activity.getApplicationContext(), LoginActivity.class));
            }
        });

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(activity.getApplicationContext(), LoginActivity.class));
            }
        });

        return view;
    }
}
