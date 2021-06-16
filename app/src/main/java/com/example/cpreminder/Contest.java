package com.example.cpreminder;

public class Contest {
    private int ID;
    private String name;
    private String date;
    private String time;
    private boolean notify;

    public Contest(int ID,String name, String date, String time, boolean notify) {
        this.ID=ID;
        this.name = name;
        this.date = date;
        this.time = time;
        this.notify = notify;
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public boolean isNotify() {
        return notify;
    }

    public String getDate() {
        return date;
    }
}
