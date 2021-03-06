package com.example.cpreminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME="CPDatabase.db";
    public static final String TABLE_NAME="cpTable";
    public static final String COL_1="ID";
    public static final String COL_2="Name";
    public static final String COL_3="Date";
    public static final String COL_4="Time";
    public static final String COL_5="Notify";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+TABLE_NAME+"(ID INTEGER PRIMARY KEY AUTOINCREMENT , Name TEXT , Date TEXT , Time TEXT , Notify BOOLEAN , UNIQUE (Name, Date, Time) ON CONFLICT IGNORE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public void insertData(Contest ct) {
        SQLiteDatabase db=this.getWritableDatabase();

        ContentValues contentValues=new ContentValues();
        contentValues.put(COL_2,ct.getName());
        contentValues.put(COL_3,ct.getDate());
        contentValues.put(COL_4,ct.getTime());
        contentValues.put(COL_5,ct.isNotify());

        long res=db.insert(TABLE_NAME,null,contentValues);
        if (res == -1){
            Log.i("SQL","Database write error");
        }
    }

    public List<Contest> readData(){
        List<Contest> contestList=new ArrayList<>();
        SQLiteDatabase db=this.getReadableDatabase();

        Cursor cursor=db.rawQuery("SELECT * FROM "+TABLE_NAME,null);
        if(cursor.moveToFirst()){
            do{
                contestList.add(new Contest(cursor.getInt(0),cursor.getString(1),cursor.getString(2),cursor.getString(3), cursor.getInt(4) == 1));
            }while(cursor.moveToNext());
        }
        else{
            Log.i("SQL","Database read error");
        }
        cursor.close();
        db.close();
        return contestList;
    }

    public void updateData(int ID,int check){
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("UPDATE "+TABLE_NAME+" SET notify = "+check+" WHERE ID = "+ID);
    }

    public void deleteOldData(){
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM "+TABLE_NAME+" WHERE (Date < date('now'))");
    }

    public void deleteTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_NAME);
        db.close();
    }

    public long getRowsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, TABLE_NAME);
    }
}
