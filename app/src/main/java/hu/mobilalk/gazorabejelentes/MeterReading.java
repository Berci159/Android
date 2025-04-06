package hu.mobilalk.gazorabejelentes;

import com.google.firebase.Timestamp;
import java.util.Date;

public class MeterReading {
    private String id;
    private double reading;
    private Timestamp date;
    private String userId;
    private String meterNumber;

    public MeterReading() {}

    public MeterReading(String id, double reading, Date date, String userId, String meterNumber) {
        this.id = id;
        this.reading = reading;
        this.date = new Timestamp(date);
        this.userId = userId;
        this.meterNumber = meterNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getReading() {
        return reading;
    }

    public void setReading(double reading) {
        this.reading = reading;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public Date getDateAsDate() {
        return date.toDate();
    }

    public void setDateAsDate(Date date) {
        this.date = new Timestamp(date);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterNumber(String meterNumber) {
        this.meterNumber = meterNumber;
    }
}