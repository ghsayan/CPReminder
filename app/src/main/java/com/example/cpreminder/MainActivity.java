package com.example.cpreminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private int remTime=15;
    private Toolbar toolbar;
    SharedPreferences sharedPreferences;
    HashMap<Integer,Integer> rem=new HashMap<>();
    DatabaseHelper db=new DatabaseHelper(this); //Database object initialised
    List<Contest> ctList =new ArrayList<>(); //stores contest details in a list of dataclass Contest
    TableLayout tableLayout;
    TableRow tableRow;
    TextView tvName;
    TextView tvDate;
    TextView tvTime;
    CheckBox cbNotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("cpreminder", Context.MODE_PRIVATE);

        rem.put(5,R.id.rem5);
        rem.put(15,R.id.rem15);
        rem.put(30,R.id.rem30);
        rem.put(60,R.id.rem60);

        toolbar=findViewById(R.id.toolbar); //Set up toolbar
        setSupportActionBar(toolbar);
        getRemTime(); //Update the pre reminder time

        db.deleteOldData(); //Remove past contests
        scrapData(); //Collect contest data from all websites
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items,menu);
        MenuItem mt =menu.findItem(rem.get(remTime)); //Mark the chosen remTime as true in toolbar
        mt.setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){ //Set functionality for each item in toolbar
            case R.id.ibRefresh:
                onRefresh();
                break;

            case R.id.rem5:
                item.setChecked(true);
                sharedPreferences.edit().putInt("remTime",5).apply();
                getRemTime();
                onRefresh();
                break;

            case R.id.rem15:
                item.setChecked(true);
                sharedPreferences.edit().putInt("remTime",15).apply();
                getRemTime();
                onRefresh();
                break;

            case R.id.rem30:
                item.setChecked(true);
                sharedPreferences.edit().putInt("remTime",30).apply();
                getRemTime();
                onRefresh();
                break;

            case R.id.rem60:
                item.setChecked(true);
                sharedPreferences.edit().putInt("remTime",60).apply();
                getRemTime();
                onRefresh();
                break;
        }
        return true;
    }

    private void getRemTime(){
        remTime=sharedPreferences.getInt("remTime",15); //Update remTime value from shared preference db
    }

    private void onRefresh(){
        Toast.makeText(MainActivity.this, "Refreshing...", Toast.LENGTH_SHORT).show();
        tableLayout=findViewById(R.id.tableLayout);
        tableLayout.removeViews(1, Math.max(0, tableLayout.getChildCount() - 1)); //Remove all rows except header
        db.deleteTable(); //Remove past contests
        scrapData(); //Collect contest data from all websites
    }

    private void scrapData(){
        //Collect Codeforces contest data
        new CodeforcesData().getCodeforces(new CodeforcesData.UpdateList(){
            //Returns contest list after its completely fetched
            @Override
            public void onSuccess(List<Contest> cf){
                ctList.addAll(cf);
                for(Contest c:ctList){
                    db.insertData(c); //Insert all list objects to database
                }
                showData(); // Add all rows from database into tableLayout
            }
            @Override
            public void onError(String e){
                Log.i("test",e);
            }
        },this);

        // TODO: Implement other websites
    }

    private void showData() {
        // Remove the Loading data row
        tableLayout=findViewById(R.id.tableLayout);
        tableRow = findViewById(R.id.trLoading);
        tableLayout.removeView(tableRow);

        List<Contest> contestList=db.readData(); // create a Contest class list from the values from database
        for(Contest ct:contestList){
            addTableRow(ct); // add each row into the tableLayout
        }
    }

    private void addTableRow(Contest ct){

        //Set the layout ids from table_row.xml and add each row with new data into tableLayout of activity_main.xml
        tableLayout=findViewById(R.id.tableLayout);
        tableRow=(TableRow) getLayoutInflater().inflate(R.layout.table_row,null);
        tvName=tableRow.findViewById(R.id.tvName);
        tvTime=tableRow.findViewById(R.id.tvTime);
        tvDate=tableRow.findViewById(R.id.tvDate);
        cbNotify=tableRow.findViewById(R.id.cbNotify);

        tvName.setText(ct.getName());

        String[] time=ct.getTime().split(":");
        tvTime.setText(time[0]+":"+time[1]);

        //Only show day and month
        String[] date = ct.getDate().split("-");
        tvDate.setText(date[2]+"/"+date[1]);
        cbNotify.setChecked(ct.isNotify());
        cbNotify.setTag(ct.getID());

        tableLayout.addView(tableRow);

        //Set onCheckListener for all the checkboxes in the column and change the values respectively in database according to their ID (Auto Increment)
        cbNotify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.updateData((int) buttonView.getTag(), isChecked ? 1 : 0);
            setAlarm(ct,isChecked,time,date);
        });
    }

    private void setAlarm(Contest ct, boolean isChecked, String[] time, String[] date){

        if(isChecked){
            Intent intent = new Intent(MainActivity.this,AlarmReceiver.class);
            intent.putExtra("id",ct.getID());
            intent.putExtra("name",ct.getName());
            intent.putExtra("rem",remTime);
            //Pending intent with the id of that specific contest
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,ct.getID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            //Set date and time in calendar object
            Calendar calendar= Calendar.getInstance();
            calendar.set(Integer.parseInt(date[0]), Integer.parseInt(date[1])-1, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]),0);
            calendar.add(Calendar.MINUTE,-remTime);
            alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
        }

        else{
            Intent intent = new Intent(MainActivity.this,AlarmReceiver.class);
            intent.putExtra("id",ct.getID());
            intent.putExtra("name",ct.getName());
            //Get the same pending intent used to create the alarm
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,ct.getID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent); //Removes the alarm if unchecked
        }

    }
}