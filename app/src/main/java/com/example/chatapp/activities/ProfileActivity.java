package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.R;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    Button btnLogout;
    BottomNavigationView bottomNav;
    PreferenceManager preferenceManager;
    TextView textFullName, textEmail;
    RoundedImageView profileImage;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        preferenceManager = new PreferenceManager(getApplicationContext());
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        profileImage = findViewById(R.id.imageProfile);
        textFullName = findViewById(R.id.textUserName);
        textEmail = findViewById(R.id.textUserEmail);
        btnLogout = findViewById(R.id.buttonLogout);
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        loadUserData();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_message) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId != null) {
                DocumentReference userRef = db.collection(Constants.KEY_USERS).document(userId);

                userRef.update(Constants.KEY_FCM_TOKEN, FieldValue.delete())
                        .addOnSuccessListener(unused -> {
                            mAuth.signOut();
                            preferenceManager.clear();
                            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> showToast("Lỗi đăng xuất: " + e.getMessage()));
            } else {
                mAuth.signOut();
                preferenceManager.clear();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        profileImage.setOnClickListener(v -> selectImage());
    }

    private void loadUserData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (userId != null) {
            db.collection(Constants.KEY_USERS)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString(Constants.KEY_NAME);
                            String email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            String encodedImage = documentSnapshot.getString(Constants.KEY_IMAGE);

                            textFullName.setText(fullName != null ? fullName : "Không có tên");
                            textEmail.setText(email != null ? email : "Không có email");

                            if (encodedImage != null) {
                                Bitmap bitmap = decodeImage(encodedImage);
                                profileImage.setImageBitmap(bitmap);
                            }
                        } else {
                            showToast("Không tìm thấy thông tin người dùng");
                        }
                    })
                    .addOnFailureListener(e -> showToast("Lỗi tải thông tin: " ));
        } else {
            showToast("Không tìm thấy ID người dùng");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImage.setImageBitmap(bitmap);
                String encodedImage = encodeImage(bitmap);
                updateUserImage(encodedImage);
            } catch (IOException e) {
                showToast("Lỗi chọn ảnh: ");
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] bytes = outputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Bitmap decodeImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void updateUserImage(String encodedImage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (userId != null) {
            db.collection(Constants.KEY_USERS)
                    .document(userId)
                    .update(Constants.KEY_IMAGE, encodedImage)
                    .addOnSuccessListener(unused -> showToast("Cập nhật ảnh đại diện thành công"))
                    .addOnFailureListener(e -> showToast("Lỗi cập nhật ảnh: "));
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
