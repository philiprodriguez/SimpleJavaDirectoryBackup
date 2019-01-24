import java.util.Calendar;
import java.util.GregorianCalendar;

/*
    A class for representing a time of the day precise to the second.
 */
public class Time {
    int hour, minute, second;

    public Time(long millis) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(millis);
        this.hour = gc.get(Calendar.HOUR_OF_DAY);
        this.minute = gc.get(Calendar.MINUTE);
        this.second = gc.get(Calendar.SECOND);
    }

    public Time(String time24Hour) {
        String[] parts = time24Hour.split(":");
        this.hour = Integer.parseInt(parts[0]);
        this.minute = Integer.parseInt(parts[1]);
        this.second = Integer.parseInt(parts[2]);
    }

    public long getMillis() {
        return 1000 * second + 60000 * minute + (60 * 60000) * hour;
    }

    public long getWaitPeriod(Time nextTime) {
        long ct = getMillis();
        long nt = nextTime.getMillis();
        if (ct >= nt) {
            // We have all of nt plus the rest of today
            return 86400000 - ct + nt;
        } else {
            // Strictly less than means we wrapped to the next day, so just compute difference
            return nt - ct;
        }
    }

    public String toString() {
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
}