package com.example.cpreminder;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CodeforcesData {

    interface UpdateList{
        void onSuccess(List<Contest> cf);
        void onError(String e);
    }

    protected void getCodeforces(UpdateList updateList, Context context){
        RequestQueue requestQueue= Volley.newRequestQueue(context); //Create a requestQueue for api fetch request
        List<Contest> cf= new ArrayList<>();
        String cfUrl="https://codeforces.com/api/contest.list?gym=false";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, cfUrl, null, response -> {
            try {
                JSONArray jsonArray = response.getJSONArray("result");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject con = jsonArray.getJSONObject(i);
                    if (con.getString("phase").equals("BEFORE")) {
                        DateTimeFormatter formatted
                                = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                        String formattedDate = Instant.ofEpochSecond(con.getLong("startTimeSeconds"))
                                .atOffset(ZoneOffset.ofHours(3)) // Offset set to Moscow,Russia
                                .atZoneSameInstant(ZoneId.systemDefault()) // Converts to system time zone
                                .format(formatted); // Change to different format
                        Contest ct = new Contest(0, con.getString("name"), formattedDate.split(" ")[0], formattedDate.split(" ")[1], false);
                        cf.add(ct);
                    } else
                        break;
                }
                Collections.reverse(cf); //Reverse the list
                updateList.onSuccess(cf);
            } catch (JSONException e) {
                updateList.onError(e.toString());
            }
        }, error -> Log.i("test",error.toString()));
        requestQueue.add(jsonObjectRequest);
    }
}
