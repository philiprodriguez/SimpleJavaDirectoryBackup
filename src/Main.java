import java.util.Arrays;
import java.util.HashSet;
import java.nio.file.Files;
import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.commons.io.FileUtils;

class Main {
  private static File sourceDir;
  private static HashSet<File> destinationDirs;
  private static int repeatDelayInSeconds;
  private static int keepCount;
  private static Time time;

  public static void main(String[] args)
  {
    parseArgs(args);
    printArgs();
    startLooping();
  }

  private static void startLooping() {
    while(true) {
      // Perform copy operation
      for (File destination : destinationDirs) {
        System.out.println("Processing destination " + destination.getAbsolutePath().toString());
        if (destination.exists() && destination.isDirectory()) {
          try {
            // First, delete oldest if necessary!
            File[] existingFiles = destination.listFiles();
            if (existingFiles.length >= keepCount) {
              System.out.println("Too many entries in destination, removing oldest!");
              File oldest = existingFiles[0];
              for (File file : existingFiles) {
                if (!file.isDirectory())
                  throw new Exception("Invalid file located in destination directory: " + file.getName());
                if (Long.parseLong(file.getName()) < Long.parseLong(oldest.getName())) {
                  oldest = file;
                }
              }
              System.out.println("Removing " + oldest.getAbsolutePath().toString());
              FileUtils.deleteDirectory(oldest);
              System.out.println("Removed!");
            }

            // Now, put new copy!
            File timeDest = new File(destination.getAbsolutePath().toString() + "/" + System.currentTimeMillis());
            timeDest.mkdirs();
            System.out.println("Copying to " + timeDest.getAbsolutePath().toString());
            long startTime = System.currentTimeMillis();
            FileUtils.copyDirectory(sourceDir, timeDest);
            long endTime = System.currentTimeMillis();
            System.out.println("Copy complete! Took " + ((endTime-startTime)/60000) + " minutes.");
          } catch (Exception exc) {
            System.err.println("Copy failed! " + exc.getMessage());
          }
        } else {
          System.err.println("Destination directory does not exist!");
        }
      }

      // Wait until it is time for the next copy!
      if (time == null) {
        performWait(repeatDelayInSeconds);
      } else {
        // We want to wait until the next time!
        Time curTime = new Time(System.currentTimeMillis());
        long waitTimeMs = curTime.getWaitPeriod(time);
        System.out.println("Waiting for next occurrance of " + time);
        performWait((int)(waitTimeMs/1000));
      }
    }
  }

  private static void printArgs() {
    System.out.println("Simple Java Directory Backup");
    System.out.println("Source: " + sourceDir.getAbsolutePath().toString());
    System.out.println("Destinations: ");
    for (File dest : destinationDirs) {
      System.out.println("    " + dest.getAbsolutePath().toString());
    }
    System.out.println("Repeat Delay (in seconds): " + repeatDelayInSeconds);
    System.out.println("Keep Count: " + keepCount);
  }

  private static void parseArgs(String[] args) {
    sourceDir = null;
    destinationDirs = new HashSet<File>();
    repeatDelayInSeconds = -1;
    keepCount = -1;
    time = null;
    int waitInitial = 0;

    for (int a = 0; a < args.length; a += 2) {
      if (args[a].equals("-s")) {
        if (sourceDir == null) {
          sourceDir = new File(args[a+1]);
        } else {
          System.err.println("Error parsing arguments! Source cannot be set more than once!");
          printUsage();
          System.exit(1);
        }
      } else if (args[a].equals("-d")) {
        destinationDirs.add(new File(args[a+1]));
      } else if (args[a].equals("-t")) {
        if (time == null) {
          time = new Time(args[a+1]);
        } else {
          System.err.println("Error parsing arguments! Time cannot be set more than once!");
          printUsage();
          System.exit(1);
        }
      } else if (args[a].equals("-r")) {
        if (repeatDelayInSeconds == -1) {
          repeatDelayInSeconds = Integer.parseInt(args[a+1]);
        } else {
          System.err.println("Error parsing arguments! Repeat delay cannot be set more than once!");
          printUsage();
          System.exit(1);
        }
      } else if (args[a].equals("-k")) {
        if (keepCount == -1) {
          keepCount = Integer.parseInt(args[a+1]);
        } else {
          System.err.println("Error parsing arguments! Keep count cannot be set more than once!");
          printUsage();
          System.exit(1);
        }
      } else if (args[a].equals("-w")) {
        waitInitial = Integer.parseInt(args[a+1]);
      } else {
        System.err.println("Error parsing arguments! Unrecognized argument \"" + args[a] + "\"");
        printUsage();
        System.exit(1);
      }
    }

    if (sourceDir == null) {
      System.err.println("Source not set.");
      printUsage();
      System.exit(1);
    }
    if (destinationDirs.size() <= 0) {
      System.err.println("No destinations set.");
      printUsage();
      System.exit(1);
    }
    if (time == null && repeatDelayInSeconds <= -1) {
      System.err.println("Time and repeat delay not set.");
      printUsage();
      System.exit(1);
    }
    if (keepCount <= -1) {
      System.err.println("Keep count not set.");
      printUsage();
      System.exit(1);
    }
    if (time != null && repeatDelayInSeconds > -1) {
      System.err.println("Time and repeat both set, which makes no sense.");
      printUsage();
      System.exit(1);
    }

    performWait(waitInitial);
  }

  private static void performWait(int seconds) {
    if (seconds <= 0)
      return;

    System.out.println("Waiting " + seconds + " seconds...");
    try {
      for (int i = 1; i <= seconds; i++) {
        Thread.sleep(1000);
        // System.out.println("Waited " + i + " of " + seconds + " seconds...");
      }
    } catch (InterruptedException exc) {
      System.err.println("Something went wrong! " + exc.getMessage());
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.out.println("See README.md at https://github.com/philiprodriguez/SimpleJavaDirectoryBackup for usage information.");
  }

  private static class Time {
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

    private long getMillis() {
      return 1000*second+60000*minute+(60*60000)*hour;
    }

    public long getWaitPeriod(Time nextTime) {
      long ct = getMillis();
      long nt = nextTime.getMillis();
      if (ct >= nt) {
        // We have all of nt plus the rest of today
        return 86400000-ct+nt;
      } else {
        // Strictly less than means we wrapped to the next day, so just compute difference
        return nt-ct;
      }
    }

    public String toString() {
      return hour + ":" + minute + ":" + second;
    }
  }
}
