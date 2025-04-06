package hu.mobilalk.gazorabejelentes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etMeterNumber;
    private EditText etPassword;
    private EditText etPasswordConfirm;
    private Button btnRegister;
    private TextView tvLoginPrompt;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etMeterNumber = findViewById(R.id.etMeterNumber);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginPrompt = findViewById(R.id.tvLoginPrompt);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    btnRegister.setEnabled(false);

                    String email = etEmail.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();

                    createAccount(email, password);
                }
            }
        });

        tvLoginPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        });
    }

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            saveUserData(user.getUid());
                        } else {
                            Toast.makeText(RegisterActivity.this, "Regisztráció sikertelen: " +
                                            task.getException().getLocalizedMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserData(String userId) {
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", etFullName.getText().toString().trim());
        user.put("email", etEmail.getText().toString().trim());
        user.put("meterNumber", etMeterNumber.getText().toString().trim());

        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Sikeres regisztráció!",
                                    Toast.LENGTH_SHORT).show();
                            Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(loginIntent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Hiba a felhasználói adatok mentésekor: " +
                                            task.getException().getLocalizedMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        String fullName = etFullName.getText().toString().trim();
        if (fullName.isEmpty()) {
            etFullName.setError(getString(R.string.error_required_field));
            valid = false;
        }

        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_required_field));
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        String meterNumber = etMeterNumber.getText().toString().trim();
        if (meterNumber.isEmpty()) {
            etMeterNumber.setError(getString(R.string.error_required_field));
            valid = false;
        }

        String password = etPassword.getText().toString();
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_required_field));
            valid = false;
        } else if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_too_short));
            valid = false;
        }

        String passwordConfirm = etPasswordConfirm.getText().toString();
        if (passwordConfirm.isEmpty()) {
            etPasswordConfirm.setError(getString(R.string.error_required_field));
            valid = false;
        } else if (!passwordConfirm.equals(password)) {
            etPasswordConfirm.setError(getString(R.string.error_passwords_dont_match));
            valid = false;
        }

        return valid;
    }
}