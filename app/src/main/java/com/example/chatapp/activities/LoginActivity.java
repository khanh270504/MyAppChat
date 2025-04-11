package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.R;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    EditText email, pass;
    Button LoginBtn;
    TextView regis, forgotPass;
    PreferenceManager preferenceManager;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());

        //Check Signed in
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
            finish();
        }

        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.inputEmail);
        pass = findViewById(R.id.password);
        LoginBtn = findViewById(R.id.login_button);
        regis = findViewById(R.id.register);
        forgotPass = findViewById(R.id.forgot_password);

        LoginBtn.setOnClickListener(v -> {
            String inputEmail = email.getText().toString().trim();
            String inputPassword = pass.getText().toString().trim();
            boolean isValid = true;
            if (TextUtils.isEmpty(inputEmail)) {
                email.setError("Vui lòng nhập email");
                email.requestFocus();
                isValid = false;
            } else {
                email.setError(null);
            }

            if (TextUtils.isEmpty(inputPassword)) {
                pass.setError("Vui lòng nhập mật khẩu");
                pass.requestFocus();
                isValid = false;
            } else {
                pass.setError(null);
            }

            if (!isValid) {
                return;
            }

            mAuth.signInWithEmailAndPassword(inputEmail, inputPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                //GetData from FireStore
                                GetDataFromFirestore(user.getUid());
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: ", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        regis.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        forgotPass.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, FogotPassActivity.class);
            startActivity(i);
        });
    }

    private void GetDataFromFirestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString(Constants.KEY_NAME);
                        String email = documentSnapshot.getString(Constants.KEY_EMAIL);
                        String image = documentSnapshot.getString(Constants.KEY_IMAGE);

                        // Lưu thông tin vào PreferenceManager
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, userId);
                        preferenceManager.putString(Constants.KEY_NAME, name);
                        preferenceManager.putString(Constants.KEY_EMAIL, email);
                        preferenceManager.putString(Constants.KEY_IMAGE, image);

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Lỗi khi lấy dữ liệu: ", Toast.LENGTH_SHORT).show();
                });
    }
}