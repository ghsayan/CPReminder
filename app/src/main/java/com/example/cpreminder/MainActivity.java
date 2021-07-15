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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper db=new DatabaseHelper(this); //Database object initialised
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

        new ScrapData().execute(); //Asynctask class to scrap the contest schedules from different websites
    }

    private class ScrapData extends AsyncTask<Void, Void, Void> {
        List<Contest> ctList =new ArrayList<>(); //stores contest details in a list of dataclass Contest

        @Override
        protected Void doInBackground(Void... voids) {
            db.deleteOldData();
            getCodeforces();

            for(Contest c:ctList){
                db.insertData(c); //Insert all list objects to database
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            // Remove the Loading data row
            tableLayout=findViewById(R.id.tableLayout);
            tableRow = findViewById(R.id.trLoading);
            tableLayout.removeView(tableRow);

            // Add all rows from database into tableLayout
            showData();
        }

        protected void getCodeforces(){
            //Fetch name time and date from codeforces website
            try {
                String cfUrl="https://codeforces.com/contests";
                Document doc = Jsoup.connect(cfUrl).get();

                //Access all the rows in Current or upcoming contests table
                Elements rows = doc.select("div.datatable").first() //take only the first 'datatable' class for current contest
                        .select("table")
                        .select("tbody")
                        .select("tr");

                //Skip first row as it has header names
                for(Element row : rows.subList(1,rows.size())){
                    Elements td = row.select("td");

                    //Using DateTimeFormatter convert the string to displayable format
                    DateTimeFormatter formatter
                            = DateTimeFormatter.ofPattern("MMM/dd/yyyy HH:mm", Locale.ENGLISH);
                    DateTimeFormatter formatted
                            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                    String formattedDate = LocalDateTime.parse(td.get(2).text(), formatter)
                            .atOffset(ZoneOffset.ofHours(3)) // Offset set to Moscow,Russia
                            .atZoneSameInstant(ZoneId.systemDefault()) // Converts to system time zone
                            .format(formatted); // Change to different format

                    // name is the text from table-data 'td[0]' and time is 'td[2]'
                    // formattedDate is split into two strings for date and time
                    Contest ct =new Contest(0,td.get(0).text(),formattedDate.split(" ")[0],formattedDate.split(" ")[1],false);
                    ctList.add(ct);
                }
//                Contest ct1 =new Contest(0,"Cf Custom contest 1","2021-07-01","11:00",false);
//                ctList.add(ct1);
            } catch (IOException e){
                Log.i("text",e.getMessage());
            }
        }
    }

    private void showData() {
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
            if(isChecked){
                Intent intent = new Intent(MainActivity.this,AlarmReceiver.class);
                intent.putExtra("id",ct.getID());
                intent.putExtra("name",ct.getName());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,ct.getID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                Calendar calendar= Calendar.getInstance();
                calendar.set(Integer.parseInt(date[0]), Integer.parseInt(date[1])-1, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]),0);
                alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
            }
            else{
                Intent intent = new Intent(MainActivity.this,AlarmReceiver.class);
                intent.putExtra("id",ct.getID());
                intent.putExtra("name",ct.getName());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,ct.getID(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
            }
        });
    }
}