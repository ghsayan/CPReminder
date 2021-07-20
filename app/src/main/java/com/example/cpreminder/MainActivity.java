package com.example.cpreminder;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
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

        db.deleteOldData(); //Remove past contests
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
            //Pending intent with the id of that specific contest
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,ct.getID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            //Set date and time in calendar object
            Calendar calendar= Calendar.getInstance();
            calendar.set(Integer.parseInt(date[0]), Integer.parseInt(date[1])-1, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]),0);
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