package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.R;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import android.util.Base64;

public class RegisterActivity extends AppCompatActivity {

    TextView login;
    EditText fullName, email, pass, confirmPass;
    Button RegisterBtn;
    PreferenceManager preferenceManager;
    RoundedImageView imageProfile;
    String encodedImage;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());

        fullName = findViewById(R.id.inputFullName);
        email = findViewById(R.id.inputEmail);
        pass = findViewById(R.id.password);
        confirmPass = findViewById(R.id.confirmPassword);
        RegisterBtn = findViewById(R.id.register_button);
        login = findViewById(R.id.login);

        login.setOnClickListener(v -> {
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(i);
        });

        RegisterBtn.setOnClickListener(v -> {
            if (isValidSignUp()) {
                signUpWithEmailAndPassword();
            }
        });

        imageProfile = findViewById(R.id.imageProfile);
        imageProfile.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(i);
        });
    }

    private void signUpWithEmailAndPassword() {
        String userEmail = email.getText().toString().trim();
        String password = pass.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(userEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Gửi email xác thực (tùy chọn)
                            user.sendEmailVerification()
                                    .addOnSuccessListener(aVoid -> {
                                        showToast("Email xác thực đã được gửi đến " + userEmail);
                                        saveUserDataToFirestore(user.getUid());
                                    })
                                    .addOnFailureListener(e -> {
                                        showToast("Gửi email xác thực thất bại: " + e.getMessage());
                                        saveUserDataToFirestore(user.getUid()); // Vẫn lưu dữ liệu dù gửi email thất bại
                                    });
                        }
                    } else {
                        showToast("Đăng ký thất bại: " + task.getException().getMessage());
                    }
                });
    }

    private void saveUserDataToFirestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, fullName.getText().toString().trim());
        user.put(Constants.KEY_EMAIL, email.getText().toString().trim());

        user.put(Constants.KEY_IMAGE, encodedImage);

        db.collection(Constants.KEY_USERS)
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Không đặt KEY_IS_SIGNED_IN thành true ở đây
                    showToast("Đăng ký thành công! Vui lòng đăng nhập.");
                    Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .addOnFailureListener(e -> {
                    showToast("Lưu dữ liệu thất bại: ");
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignUp() {
        boolean isValid = true;
        if (TextUtils.isEmpty(fullName.getText().toString().trim())) {
            fullName.setError("Vui lòng nhập họ tên");
            fullName.requestFocus();
            isValid = false;
        } else {
            fullName.setError(null);
        }

        if (TextUtils.isEmpty(email.getText().toString().trim())) {
            email.setError("Vui lòng nhập email");
            email.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
            email.setError("Email không hợp lệ");
            email.requestFocus();
            isValid = false;
        } else {
            email.setError(null);
        }

        if (TextUtils.isEmpty(pass.getText().toString().trim())) {
            pass.setError("Vui lòng nhập mật khẩu");
            pass.requestFocus();
            isValid = false;
        } else if (pass.getText().toString().length() < 6) {
            pass.setError("Mật khẩu phải từ 6 ký tự trở lên");
            pass.requestFocus();
            isValid = false;
        } else {
            pass.setError(null);
        }

        if (TextUtils.isEmpty(confirmPass.getText().toString().trim())) {
            confirmPass.setError("Vui lòng xác nhận mật khẩu");
            confirmPass.requestFocus();
            isValid = false;
        } else if (!pass.getText().toString().equals(confirmPass.getText().toString())) {
            confirmPass.setError("Mật khẩu xác nhận không trùng khớp");
            confirmPass.requestFocus();
            isValid = false;
        } else {
            confirmPass.setError(null);
        }

        return isValid;
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
}