package hu.mobilalk.gazorabejelentes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hu.mobilalk.gazorabejelentes.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageButton btnLogout;
    private ImageButton btnHistory;
    private Button btnSubmitReading;
    private EditText etMeterReading;
    private TextView tvWelcome;
    private TextView tvLastReading;
    private FloatingActionButton fab;
    private RecyclerView rvReadingHistory;
    private Button btnViewAllHistory;

    private String currentUserId;
    private String userMeterNumber;
    private MeterReading lastReading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        currentUserId = currentUser.getUid();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        initViews();

        setupListeners();

        loadUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadMeterReadings();
        }
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        btnHistory = findViewById(R.id.btnHistory);
        btnSubmitReading = findViewById(R.id.btnSubmitReading);
        etMeterReading = findViewById(R.id.etMeterReading);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvLastReading = findViewById(R.id.tvLastReading);
        fab = findViewById(R.id.fab);
        rvReadingHistory = findViewById(R.id.rvReadingHistory);
        btnViewAllHistory = findViewById(R.id.btnViewAllHistory);

        rvReadingHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openReadingHistory();
            }
        });

        btnSubmitReading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitMeterReading();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.etMeterReading.requestFocus();
            }
        });

        btnViewAllHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openReadingHistory();
            }
        });
    }

    private void openReadingHistory() {
        Intent historyIntent = new Intent(MainActivity.this, ReadingHistoryActivity.class);
        startActivity(historyIntent);
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String fullName = document.getString("fullName");
                                userMeterNumber = document.getString("meterNumber");

                                tvWelcome.setText(getString(R.string.welcome_user, fullName));

                                loadMeterReadings();
                            } else {
                                Log.d(TAG, "No such document");
                                Toast.makeText(MainActivity.this, "Felhasználói adatok nem találhatóak", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                            Toast.makeText(MainActivity.this, "Hiba a felhasználói adatok betöltésekor", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadMeterReadings() {
        db.collection("readings")
                .whereEqualTo("userId", currentUserId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<MeterReading> readings = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MeterReading reading = document.toObject(MeterReading.class);
                                reading.setId(document.getId());
                                readings.add(reading);
                            }

                            updateReadingsUI(readings);
                        } else {
                            Log.d(TAG, "Error getting readings: ", task.getException());
                            Toast.makeText(MainActivity.this, "Hiba a mérőállások betöltésekor", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateReadingsUI(List<MeterReading> readings) {
        if (readings.isEmpty()) {
            tvLastReading.setText(R.string.no_readings_yet);
        } else {
            lastReading = readings.get(0);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.", Locale.getDefault());
            String dateStr = dateFormat.format(lastReading.getDateAsDate());

            tvLastReading.setText(getString(R.string.last_reading_value, dateStr, lastReading.getReading()));

            Toast.makeText(this, "Összesen " + readings.size() + " mérőállás betöltve", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitMeterReading() {
        String readingStr = etMeterReading.getText().toString().trim();

        if (TextUtils.isEmpty(readingStr)) {
            etMeterReading.setError("Kérjük, adja meg a mérőállást");
            return;
        }

        try {
            double reading = Double.parseDouble(readingStr);

            if (lastReading != null && reading <= lastReading.getReading()) {
                etMeterReading.setError("Az új mérőállás nem lehet kisebb vagy egyenlő az előzőnél");
                return;
            }

            MeterReading newReading = new MeterReading(
                    null,
                    reading,
                    new Date(),
                    currentUserId,
                    userMeterNumber
            );

            db.collection("readings")
                    .add(newReading)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            Toast.makeText(MainActivity.this, "Mérőállás sikeresen rögzítve!", Toast.LENGTH_SHORT).show();

                            etMeterReading.setText("");

                            loadMeterReadings();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                            Toast.makeText(MainActivity.this, "Hiba a mérőállás mentésekor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (NumberFormatException e) {
            etMeterReading.setError("Érvénytelen szám formátum");
        }
    }

    private void logoutUser() {
        mAuth.signOut();

        Toast.makeText(MainActivity.this, "Sikeres kijelentkezés!", Toast.LENGTH_SHORT).show();

        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}