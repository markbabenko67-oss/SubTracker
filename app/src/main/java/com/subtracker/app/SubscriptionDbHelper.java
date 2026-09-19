package com.subtracker.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "subscriptions.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "subscriptions";
    private static final String COL_ID = "_id";
    private static final String COL_NAME = "name";
    private static final String COL_PRICE = "price";
    private static final String COL_PERIOD = "period";
    private static final String COL_DAY = "day";
    private static final String COL_NEXT_CHARGE = "next_charge";

    public SubscriptionDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT NOT NULL, " +
                COL_PRICE + " REAL NOT NULL, " +
                COL_PERIOD + " INTEGER NOT NULL, " +
                COL_DAY + " INTEGER NOT NULL, " +
                COL_NEXT_CHARGE + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long insert(Subscription s) {
        return getWritableDatabase().insert(TABLE, null, toValues(s));
    }

    public void update(Subscription s) {
        getWritableDatabase().update(TABLE, toValues(s), COL_ID + " = ?",
                new String[]{String.valueOf(s.id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public Subscription get(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    public List<Subscription> getAll() {
        List<Subscription> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null,
                null, null, null, null, COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                result.add(fromCursor(c));
            }
        }
        return result;
    }

    private ContentValues toValues(Subscription s) {
        ContentValues v = new ContentValues();
        v.put(COL_NAME, s.name);
        v.put(COL_PRICE, s.price);
        v.put(COL_PERIOD, s.period);
        v.put(COL_DAY, s.day);
        v.put(COL_NEXT_CHARGE, s.nextCharge);
        return v;
    }

    private Subscription fromCursor(Cursor c) {
        Subscription s = new Subscription();
        s.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
        s.name = c.getString(c.getColumnIndexOrThrow(COL_NAME));
        s.price = c.getDouble(c.getColumnIndexOrThrow(COL_PRICE));
        s.period = c.getInt(c.getColumnIndexOrThrow(COL_PERIOD));
        s.day = c.getInt(c.getColumnIndexOrThrow(COL_DAY));
        s.nextCharge = c.getLong(c.getColumnIndexOrThrow(COL_NEXT_CHARGE));
        return s;
    }
}