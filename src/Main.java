import java.util.Arrays;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import org.apache.commons.io.FileUtils;

class Main {
  private static File sourceDir;
  private static HashSet<File> destinationDirs;
  private static int repeatDelayInSeconds;
  private static int keepCount;
  private static Time time;
  private static boolean continuousMode;
  private static boolean logToFile;

  public static void main(String[] args)
  {
    parseArgs(args);
    printArgs();
    startLooping();
  }

  private static void outputMessage(String message) {
    String formattedMessage = new Date().toString() + ": " + message;
    System.out.println(formattedMessage);
    if (logToFile) {
      try (PrintWriter output = new PrintWriter(new FileWriter("SimpleJavaDirectoryBackupLog.log", true))) {
        output.println(formattedMessage);
      } catch (Exception exc) {
        exc.printStackTrace();
      }
    }
  }

  private static void startLooping() {
    while(true) {
      // Perform copy operation
      boolean successfulCopyOperation = false;
      for (File destination : destinationDirs) {
        outputMessage("Processing destination " + destination.getAbsolutePath().toString());
        if (destination.exists() && destination.isDirectory()) {
          try {
            // First, delete oldest if necessary!
            File[] existingFiles = destination.listFiles();
            if (existingFiles.length >= keepCount) {
              outputMessage("Too many entries in destination, removing oldest!");
              File oldest = existingFiles[0];
              for (File file : existingFiles) {
                if (!file.isDirectory())
                  throw new Exception("Invalid file located in destination directory: " + file.getName());
                if (Long.parseLong(file.getName()) < Long.parseLong(oldest.getName())) {
                  oldest = file;
                }
              }
              outputMessage("Removing " + oldest.getAbsolutePath().toString());
              FileUtils.deleteDirectory(oldest);
              outputMessage("Removed!");
            }

            // Now, put new copy!
            File timeDest = new File(destination.getAbsolutePath().toString() + "/" + System.currentTimeMillis());
            timeDest.mkdirs();
            outputMessage("Copying to " + timeDest.getAbsolutePath().toString());
            long startTime = System.currentTimeMillis();

            //FileUtils.copyDirectory(sourceDir, timeDest);

            // Manually copy files to ignore exceptions on any single file...
            copyDirectory(sourceDir, timeDest, timeDest);

            long endTime = System.currentTimeMillis();
            outputMessage("Copy complete! Took " + ((endTime-startTime)/60000) + " minutes.");
            successfulCopyOperation = true;
          } catch (Exception exc) {
            exc.printStackTrace();
            System.err.println("Copy failed! " + exc.getMessage());
          }
        } else {
          System.err.println("Destination directory does not exist!");
        }
      }

      // If we're in continuousMode...
      if (continuousMode) {
        // AND our copy just succeeded...
        if (successfulCopyOperation) {
          // We want to wait repeatDelayInSeconds.
          outputMessage("Successful continuous mode copy, so waiting...");
          performWait(repeatDelayInSeconds);
        } else {
          // Unsuccessful so we'll wait only for a destination to become available
          outputMessage("Waiting for a destination to become available...");
          couter: while (true) {
            performWait(1, true);
            for (File destination : destinationDirs) {
              if (destination.exists() && destination.isDirectory()) {
                outputMessage("Destination available: " + destination.toString());
                break couter;
              }
            }
          }
        }
      } else if (time == null) {
        performWait(repeatDelayInSeconds);
      } else {
        // We want to wait until the next time!
        Time curTime = new Time(System.currentTimeMillis());
        long waitTimeMs = curTime.getWaitPeriod(time);
        outputMessage("Waiting for next occurrance of " + time);
        performWait((int)(waitTimeMs/1000));
      }
    }
  }

  /*
    Copy source to destination, creating destination if needed. Ignore any
    exceptions that occur on any single file (e.g. permission issues, etc).
  */
  private static void copyDirectory(File source, File destination, File originalDestination) throws Exception {
    File[] dirFiles = source.listFiles();
    for (File f : dirFiles) {
      try {
        File equivalent = Paths.get(destination.toPath().toString(), f.getName()).toFile();
        if (f.isDirectory()) {
          if (equivalent.mkdirs()) {
            copyDirectory(f, equivalent, originalDestination);
          } else {
            outputMessage("Failed to create directory at " + equivalent.getAbsolutePath());
          }
        } else if (f.isFile()) {
          try {
            FileUtils.copyFile(f, equivalent);
          } catch (IOException exc) {
            outputMessage("Failed to copy file at " + f.getAbsolutePath() + ": " + exc.getMessage());
            throwIfNotExists(originalDestination, exc);
          }
        } else {
          outputMessage("Ignoring unrecognized entity at " + f.getAbsolutePath());
        }
      } catch (IOException exc) {
        outputMessage("Failed to copy entity at " + f.getAbsolutePath() + ": " + exc.getMessage());
        throwIfNotExists(originalDestination, exc);
      }
    }
  }

  private static void throwIfNotExists(File file, Exception exc) throws Exception {
    if (!file.exists() || exc.getMessage().toLowerCase().contains("read-only"))
      throw new Exception("Failed non-existence check!");
  }

  private static void printArgs() {
    outputMessage("Simple Java Directory Backup");
    outputMessage("Working Directory: " +  System.getProperty("user.dir"));
    outputMessage("Source: " + sourceDir.getAbsolutePath().toString());
    outputMessage("Destinations: ");
    for (File dest : destinationDirs) {
      outputMessage("    " + dest.getAbsolutePath().toString());
    }
    outputMessage("Continuous Mode: " + continuousMode);
    outputMessage("Repeat Delay (in seconds): " + repeatDelayInSeconds);
    outputMessage("Keep Count: " + keepCount);
    outputMessage("Log to File: " + logToFile);
    outputMessage("Time: " + time);
  }

  private static void parseArgs(String[] args) {
    sourceDir = null;
    destinationDirs = new HashSet<File>();
    repeatDelayInSeconds = -1;
    keepCount = -1;
    time = null;
    continuousMode = false;
    logToFile = false;
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
      } else if (args[a].equals("-c")) {
        repeatDelayInSeconds = Integer.parseInt(args[a+1]);
        continuousMode = true;
      } else if (args[a].equals("-l")) {
        logToFile = args[a+1].toLowerCase().equals("y");
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
    performWait(seconds, false);
  }

  private static void performWait(int seconds, boolean quiet) {
    if (seconds <= 0)
      return;

    if (!quiet)
      outputMessage("Waiting " + seconds + " seconds...");
    try {
      for (int i = 1; i <= seconds; i++) {
        Thread.sleep(1000);
        // outputMessage("Waited " + i + " of " + seconds + " seconds...");
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
