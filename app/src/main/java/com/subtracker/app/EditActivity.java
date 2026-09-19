package com.subtracker.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class EditActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "subscription_id";

    private static final String[] PERIODS = {
            "Ежемесячно",
            "Раз в год",
            "Еженедельно",
            "Раз в квартал",
            "Раз в полгода"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private SubscriptionDbHelper db;
    private Subscription editing;

    private TextInputEditText nameInput, priceInput, dateInput;
    private MaterialAutoCompleteTextView periodInput;
    private TextView errorText;
    private MaterialButton deleteButton;
    private LocalDate chosenDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        db = new SubscriptionDbHelper(this);

        nameInput = findViewById(R.id.nameInput);
        priceInput = findViewById(R.id.priceInput);
        periodInput = findViewById(R.id.periodInput);
        dateInput = findViewById(R.id.dateInput);
        errorText = findViewById(R.id.errorText);
        deleteButton = findViewById(R.id.deleteButton);

        ArrayAdapter<String> periodAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, PERIODS);
        periodInput.setAdapter(periodAdapter);

        long editId = getIntent().getLongExtra(EXTRA_ID, -1);
        if (editId >= 0) {
            editing = db.get(editId);
            if (editing == null) {
                finish();
                return;
            }
            setTitle(R.string.add);
            nameInput.setText(editing.name);
            priceInput.setText(SubscriptionAdapter.formatMoney(editing.price));
            periodInput.setText(PERIODS[Math.min(editing.period, PERIODS.length - 1)], false);
            dateInput.setText(formatDate(editing.nextCharge));
            chosenDate = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(editing.nextCharge), ZoneId.systemDefault());
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            chosenDate = LocalDate.now().plusDays(1);
            dateInput.setText(formatChosen(chosenDate));
            periodInput.setText(PERIODS[0], false);
        }

        dateInput.setOnClickListener(v -> {
            LocalDate base = chosenDate != null ? chosenDate : LocalDate.now();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (DatePicker picker, int y, int m, int d) -> {
                        chosenDate = LocalDate.of(y, m + 1, d);
                        dateInput.setText(formatChosen(chosenDate));
                    },
                    base.getYear(), base.getMonthValue() - 1, base.getDayOfMonth());
            dialog.show();
        });

        MaterialButton saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> save());

        deleteButton.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Удалить подписку")
                        .setMessage("Удалить «" + editing.name + "»?")
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            ReminderScheduler.cancel(this, editing.id);
                            db.delete(editing.id);
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());
    }

    private void save() {
        errorText.setVisibility(View.GONE);

        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        if (name.isEmpty()) {
            showError("Укажите название сервиса");
            return;
        }

        String priceStr = priceInput.getText() != null ? priceInput.getText().toString().trim() : "";
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            showError("Укажите корректную цену");
            return;
        }
        if (price <= 0) {
            showError("Цена должна быть больше нуля");
            return;
        }

        int period;
        Object selected = periodInput.getText() == null ? null : periodInput.getText().toString();
        if (selected == null) {
            showError("Выберите период списания");
            return;
        }
        if (selected.equals(PERIODS[1])) period = Subscription.PERIOD_YEARLY;
        else if (selected.equals(PERIODS[2])) period = Subscription.PERIOD_WEEKLY;
        else if (selected.equals(PERIODS[3])) period = Subscription.PERIOD_QUARTERLY;
        else if (selected.equals(PERIODS[4])) period = Subscription.PERIOD_HALF_YEAR;
        else period = Subscription.PERIOD_MONTHLY;

        if (chosenDate == null) {
            showError("Выберите дату следующего списания");
            return;
        }

        Subscription s = editing != null ? editing : new Subscription();
        s.name = name;
        s.price = price;
        s.period = period;
        s.day = chosenDate.getDayOfMonth();
        s.nextCharge = ZonedDateTime.of(chosenDate.atTime(9, 0),
                ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (editing == null) {
            s.id = db.insert(s);
        } else {
            db.update(s);
        }
        ReminderScheduler.scheduleOne(this, s);
        setResult(RESULT_OK);
        finish();
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private static String formatChosen(LocalDate date) {
        return DATE_FMT.format(date);
    }

    private static String formatDate(long millis) {
        return DATE_FMT.format(LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
    }
}