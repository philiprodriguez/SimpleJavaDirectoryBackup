import java.io.File;
import java.util.Arrays;

public class ApplicationArguments {
    private final File source;
    private final File[] destinations;
    private final int repeatDelayInSeconds;
    private final int keepCount;
    private final Time time;
    private final boolean continuousMode;

    public ApplicationArguments(File source, File[] destinations, int repeatDelayInSeconds, int keepCount, Time time, boolean continuousMode) {
        this.source = source;
        this.destinations = Arrays.copyOf(destinations, destinations.length);
        this.repeatDelayInSeconds = repeatDelayInSeconds;
        this.keepCount = keepCount;
        if (time != null)
            this.time = new Time(time.getMillis());
        else
            this.time = null;
        this.continuousMode = continuousMode;
    }

    public File getSource() {
        return source;
    }

    public File[] getDestinations() {
        return destinations;
    }

    public int getRepeatDelayInSeconds() {
        return repeatDelayInSeconds;
    }

    public int getKeepCount() {
        return keepCount;
    }

    public Time getTime() {
        return time;
    }

    public boolean isContinuousMode() {
        return continuousMode;
    }
}
