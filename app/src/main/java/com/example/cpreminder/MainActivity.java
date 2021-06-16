package com.example.cpreminder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper db;
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

        scrapData();
        showData();

    }

    private void scrapData() {
        db=new DatabaseHelper(this);

        //Scrap data from website

//        Contest contest =new Contest(0,"Codeforces Round #120","5/5/21", "12:00 PM", false);
//        db.insertData(contest);
    }

    private void showData() {
        db=new DatabaseHelper(this);
        List<Contest> contestList=db.readData();
        for(Contest ct:contestList){
            addTableRow(ct.getID(),ct.getName(),ct.getDate(),ct.getTime(),ct.isNotify());
        }
    }

    private void addTableRow(int ID,String name,String date,String time,boolean notify){
        tableLayout=findViewById(R.id.tableLayout);
        tableRow=(TableRow) getLayoutInflater().inflate(R.layout.table_row,null);
        tvName=tableRow.findViewById(R.id.tvName);
        tvTime=tableRow.findViewById(R.id.tvTime);
        tvDate=tableRow.findViewById(R.id.tvDate);
        cbNotify=tableRow.findViewById(R.id.cbNotify);

        tvName.setText(name);
        tvTime.setText(time);
        tvDate.setText(date);
        cbNotify.setChecked(notify);
        cbNotify.setTag(ID);
        tableLayout.addView(tableRow);

        cbNotify.setOnCheckedChangeListener((buttonView, isChecked) -> db.updateData((int) buttonView.getTag(), isChecked?1:0));
    }

}