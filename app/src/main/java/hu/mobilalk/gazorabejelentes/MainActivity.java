package hu.mobilalk.gazorabejelentes;

import android.annotation.SuppressLint;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import android.Manifest;
import android.os.Build;


import hu.mobilalk.gazorabejelentes.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

    private Button btnTakePhoto;
    private Button btnSetReminder;
    private Uri photoUri;
    private NotificationHelper notificationHelper;
    private AlarmHelper alarmHelper;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        currentUserId = currentUser.getUid();

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        notificationHelper = new NotificationHelper(this);

        alarmHelper = new AlarmHelper(this);

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

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSetReminder = findViewById(R.id.btnSetReminder);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ScheduleExactAlarm")
            @Override
            public void onClick(View v) {
                alarmHelper.setTestReminder();
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

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission();
            }
        });

        btnSetReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCalendarPermission();
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
                            notificationHelper.showReadingSavedNotification(reading);

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

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestCalendarPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }
                if (allGranted) {
                    setCalendarReminder();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    savePhotoToReading();
                }
            });


    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
            }
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("MainActivity", "Error creating image file", ex);
                Toast.makeText(this, R.string.error_saving_photo, Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                try {
                    String authorities = getApplicationContext().getPackageName() + ".fileprovider";
                    photoUri = FileProvider.getUriForFile(this,
                            authorities,
                            photoFile);

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    takePictureLauncher.launch(takePictureIntent);

                } catch (Exception e) {
                    Log.e("MainActivity", "Error launching camera", e);
                    Toast.makeText(this, "Hiba a kamera indításakor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "Virtuális eszközön nem jeleníthető meg a kamera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void savePhotoToReading() {
        Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show();
    }

    private void checkCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CALENDAR)) {
                Toast.makeText(this, R.string.permission_calendar_rationale, Toast.LENGTH_LONG).show();
            }

            requestCalendarPermissionLauncher.launch(new String[]{
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
            });
        } else {
            setCalendarReminder();
        }
    }

    private void setCalendarReminder() {
        Calendar beginTime = Calendar.getInstance();
        beginTime.add(Calendar.MONTH, 1);
        beginTime.set(Calendar.DAY_OF_MONTH, 1);
        beginTime.set(Calendar.HOUR_OF_DAY, 10);
        beginTime.set(Calendar.MINUTE, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault());
        String reminderDate = dateFormat.format(beginTime.getTime());

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.Events.TITLE, getString(R.string.reminder_title))
                .putExtra(CalendarContract.Events.DESCRIPTION, getString(R.string.reminder_description))
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

        try {
            startActivity(intent);
            Toast.makeText(this, getString(R.string.reminder_set, reminderDate), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_setting_reminder, Toast.LENGTH_SHORT).show();
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