package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.Toast;

public class FogotPassActivity extends AppCompatActivity {

    EditText email;
    Button SubmitBtn;
    TextView backLogin;
    FirebaseAuth mAuth;
    String strEmail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fogot_pass);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        email = findViewById(R.id.inputEmail);
        SubmitBtn = findViewById(R.id.submit_button);
        backLogin = findViewById(R.id.back_to_login);
        mAuth = FirebaseAuth.getInstance();

        backLogin.setOnClickListener(v ->{
            Intent i = new Intent(FogotPassActivity.this, LoginActivity.class);
            startActivity(i);
        });

        SubmitBtn.setOnClickListener(v -> {
            strEmail = email.getText().toString().trim();
            if (!TextUtils.isEmpty(strEmail)) {
                ResetPassword();
            } else {
                email.setError("Không được để trống email");
            }
        });

    }
    private void ResetPassword() {
        SubmitBtn.setVisibility(View.INVISIBLE);

        mAuth.sendPasswordResetEmail(strEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(FogotPassActivity.this, "Link Reset Password đã gửi trong Gmail", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(FogotPassActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(FogotPassActivity.this, "Error :- " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        SubmitBtn.setVisibility(View.VISIBLE);
                    }
                });
    }
}