package com.example.cpreminder;

public class Contest {
    private int ID;
    private String name;
    private String time;
    private boolean notify;

    public Contest(int ID,String name, String time, boolean notify) {
        this.ID=ID;
        this.name = name;
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

    public void setNotify(boolean notify) {
        this.notify = notify;
    }
}
