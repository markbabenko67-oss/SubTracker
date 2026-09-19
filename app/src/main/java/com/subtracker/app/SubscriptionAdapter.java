package com.subtracker.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.Holder> {

    public interface OnItemClick {
        void onClick(Subscription s);
    }

    private static final DecimalFormat PRICE = new DecimalFormat("#.##");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM");

    private final Context context;
    private final List<Subscription> items;
    private final OnItemClick listener;

    public SubscriptionAdapter(Context context, List<Subscription> items, OnItemClick listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Subscription s = items.get(position);
        h.name.setText(s.name);
        h.price.setText(formatMoney(s.price) + getPeriodSuffix(s.period));
        h.period.setText(getPeriodLabel(s.period));

        int days = Subscription.daysUntil(s.nextCharge);
        String line;
        if (days <= 0) {
            line = context.getString(R.string.today);
        } else if (days == 1) {
            line = context.getString(R.string.tomorrow);
        } else {
            line = context.getString(R.string.in_days, days);
        }
        ZonedDateTime charge = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(s.nextCharge),
                java.time.ZoneId.systemDefault());
        h.nextCharge.setText(context.getString(R.string.next_charge) + ": "
                + charge.format(DATE) + " · " + line);

        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static String formatMoney(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return PRICE.format(value);
    }

    public static String getPeriodLabel(int period) {
        switch (period) {
            case Subscription.PERIOD_YEARLY: return " / год";
            case Subscription.PERIOD_WEEKLY: return " / нед";
            case Subscription.PERIOD_QUARTERLY: return " / квартал";
            case Subscription.PERIOD_HALF_YEAR: return " / полгода";
            default: return " / мес";
        }
    }

    public static String getPeriodSuffix(int period) {
        return getPeriodLabel(period);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name, price, period, nextCharge;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            price = itemView.findViewById(R.id.price);
            period = itemView.findViewById(R.id.period);
            nextCharge = itemView.findViewById(R.id.nextCharge);
        }
    }
}