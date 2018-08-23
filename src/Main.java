import java.util.Arrays;
import java.util.HashSet;
import java.nio.file.Files;
import java.io.File;
import org.apache.commons.io.FileUtils;

class Main {
  private static File sourceDir;
  private static HashSet<File> destinationDirs;
  private static int repeatDelayInSeconds;
  private static int keepCount;

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
            FileUtils.copyDirectory(sourceDir, timeDest);
            System.out.println("Copy complete!");
          } catch (Exception exc) {
            System.err.println("Copy failed! " + exc.getMessage());
          }
        } else {
          System.err.println("Destination directory does not exist!");
        }
      }

      // Wait
      performWait(repeatDelayInSeconds);
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
    if (repeatDelayInSeconds <= -1) {
      System.err.println("Repeat delay not set.");
      printUsage();
      System.exit(1);
    }
    if (keepCount <= -1) {
      System.err.println("Keep count not set.");
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
        System.out.println("Waited " + i + " of " + seconds + " seconds...");
      }
    } catch (InterruptedException exc) {
      System.err.println("Something went wrong! " + exc.getMessage());
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.out.println("See README.md at https://github.com/philiprodriguez/SimpleJavaDirectoryBackup for usage information.");
  }
}
