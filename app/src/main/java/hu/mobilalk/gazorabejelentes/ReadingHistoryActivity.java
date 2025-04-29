package hu.mobilalk.gazorabejelentes;

import android.app.AlertDialog;
import android.app.Dialog;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReadingHistoryActivity extends AppCompatActivity implements ReadingAdapter.OnReadingListener {

    private static final String TAG = "ReadingHistoryActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageButton btnBack;
    private RecyclerView rvReadingHistoryFull;
    private TextView tvNoReadings;
    private ReadingAdapter adapter;
    private List<MeterReading> readings;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        currentUserId = currentUser.getUid();

        initViews();

        setupListeners();

        loadMeterReadings();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvReadingHistoryFull = findViewById(R.id.rvReadingHistoryFull);
        tvNoReadings = findViewById(R.id.tvNoReadings);

        readings = new ArrayList<>();
        adapter = new ReadingAdapter(this, readings, this);
        rvReadingHistoryFull.setLayoutManager(new LinearLayoutManager(this));
        rvReadingHistoryFull.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
                            readings.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MeterReading reading = document.toObject(MeterReading.class);
                                reading.setId(document.getId());
                                readings.add(reading);
                            }

                            updateUI();
                        } else {
                            Log.d(TAG, "Error getting readings: ", task.getException());
                            Toast.makeText(ReadingHistoryActivity.this, "Hiba a mérőállások betöltésekor", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUI() {
        if (readings.isEmpty()) {
            tvNoReadings.setVisibility(View.VISIBLE);
            rvReadingHistoryFull.setVisibility(View.GONE);
        } else {
            tvNoReadings.setVisibility(View.GONE);
            rvReadingHistoryFull.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onEditClick(int position) {
        showEditDialog(readings.get(position));
    }

    @Override
    public void onDeleteClick(int position) {
        showDeleteConfirmationDialog(readings.get(position));
    }

    private void showEditDialog(final MeterReading reading) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_reading);
        dialog.setCancelable(true);

        TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
        EditText etDialogReading = dialog.findViewById(R.id.etDialogReading);
        Button btnDialogCancel = dialog.findViewById(R.id.btnDialogCancel);
        Button btnDialogSave = dialog.findViewById(R.id.btnDialogSave);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.", Locale.getDefault());
        String dateStr = dateFormat.format(reading.getDateAsDate());

        tvDialogDate.setText(dateStr);
        etDialogReading.setText(String.valueOf(reading.getReading()));

        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnDialogSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String readingStr = etDialogReading.getText().toString().trim();

                if (TextUtils.isEmpty(readingStr)) {
                    etDialogReading.setError("Kérjük, adja meg a mérőállást");
                    return;
                }

                try {
                    double newReading = Double.parseDouble(readingStr);
                    updateReading(reading, newReading);
                } catch (NumberFormatException e) {
                    etDialogReading.setError("Érvénytelen szám formátum");
                }
            }
        });

        dialog.show();
    }

    private void updateReading(MeterReading reading, double newReading) {
        reading.setReading(newReading);

        db.collection("readings").document(reading.getId())
                .update("reading", newReading)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ReadingHistoryActivity.this, R.string.reading_updated, Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ReadingHistoryActivity.this, R.string.update_error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating reading", e);
                    }
                });
    }

    private void showDeleteConfirmationDialog(final MeterReading reading) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_reading);
        builder.setMessage(R.string.delete_confirmation);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            deleteReading(reading);
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void deleteReading(MeterReading reading) {
        db.collection("readings").document(reading.getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ReadingHistoryActivity.this, R.string.reading_deleted, Toast.LENGTH_SHORT).show();
                        readings.remove(reading);
                        updateUI();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ReadingHistoryActivity.this, R.string.delete_error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error deleting reading", e);
                    }
                });
    }
}