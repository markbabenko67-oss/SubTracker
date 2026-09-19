package com.subtracker.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_EDIT = 100;

    private SubscriptionDbHelper db;
    private final List<Subscription> items = new ArrayList<>();
    private SubscriptionAdapter adapter;
    private TextView monthlyTotal, yearlyTotal, emptyView;
    private RecyclerView list;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new SubscriptionDbHelper(this);
        ReminderScheduler.ensureChannel(this);

        monthlyTotal = findViewById(R.id.monthlyTotal);
        yearlyTotal = findViewById(R.id.yearlyTotal);
        emptyView = findViewById(R.id.emptyView);
        list = findViewById(R.id.list);

        adapter = new SubscriptionAdapter(this, items, this::edit);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButton addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> startActivityForResult(
                new Intent(this, EditActivity.class), REQ_EDIT));

        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        items.clear();
        items.addAll(db.getAll());
        adapter.notifyDataSetChanged();

        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);

        double month = 0, year = 0;
        for (Subscription s : items) {
            month += s.monthlyCost();
            year += s.annualCost();
        }
        monthlyTotal.setText(SubscriptionAdapter.formatMoney(month) + " ₽ "
                + getString(R.string.monthly_total));
        yearlyTotal.setText(SubscriptionAdapter.formatMoney(year) + " ₽ "
                + getString(R.string.yearly_total));

        ReminderScheduler.rescheduleAll(this);
    }

    private void edit(Subscription s) {
        Intent i = new Intent(this, EditActivity.class);
        i.putExtra(EditActivity.EXTRA_ID, s.id);
        startActivityForResult(i, REQ_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT) {
            refresh();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}