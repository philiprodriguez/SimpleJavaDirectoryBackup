import java.util.ArrayList;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Main {
    private static Logger logger = new Logger(null, -1);

    private static ApplicationArguments applicationArguments;

    // Effectively an instance of the Timer class, see https://docs.oracle.com/javase/7/docs/api/java/util/Timer.html
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        parseArgs(args);
        printArgs();
        scheduledExecutorService.schedule(new CopyIterationRunnable(applicationArguments, scheduledExecutorService, logger), 0, TimeUnit.SECONDS);
    }

    private static void printArgs() {
        logger.log("Simple Java Directory Backup");
        logger.log("Working Directory: " + System.getProperty("user.dir"));
        logger.log("Source: " + applicationArguments.getSource().getAbsolutePath());
        logger.log("Destinations: ");
        for (File dest : applicationArguments.getDestinations()) {
            logger.log("    " + dest.getAbsolutePath());
        }
        logger.log("Continuous Mode: " + applicationArguments.isContinuousMode());
        logger.log("Repeat Delay (in seconds): " + applicationArguments.getRepeatDelayInSeconds());
        logger.log("Keep Count: " + applicationArguments.getKeepCount());
        logger.log("Log File Prefix: " + logger.getFilePrefix());
        logger.log("Time: " + applicationArguments.getTime());
    }

    private static void parseArgs(String[] args) {
        File sourceDir = null;
        ArrayList<File> destinationDirs = new ArrayList<File>();
        int repeatDelayInSeconds = -1;
        int keepCount = -1;
        Time time = null;
        boolean continuousMode = false;

        for (int a = 0; a < args.length; a += 2) {
            if (args[a].equals("-s")) {
                if (sourceDir == null) {
                    sourceDir = new File(args[a + 1]);
                } else {
                    System.err.println("Error parsing arguments! Source cannot be set more than once!");
                    printUsage();
                    System.exit(1);
                }
            } else if (args[a].equals("-d")) {
                destinationDirs.add(new File(args[a + 1]));
            } else if (args[a].equals("-t")) {
                if (time == null) {
                    time = new Time(args[a + 1]);
                } else {
                    System.err.println("Error parsing arguments! Time cannot be set more than once!");
                    printUsage();
                    System.exit(1);
                }
            } else if (args[a].equals("-r")) {
                if (repeatDelayInSeconds == -1) {
                    repeatDelayInSeconds = Integer.parseInt(args[a + 1]);
                } else {
                    System.err.println("Error parsing arguments! Repeat delay cannot be set more than once!");
                    printUsage();
                    System.exit(1);
                }
            } else if (args[a].equals("-k")) {
                if (keepCount == -1) {
                    keepCount = Integer.parseInt(args[a + 1]);
                } else {
                    System.err.println("Error parsing arguments! Keep count cannot be set more than once!");
                    printUsage();
                    System.exit(1);
                }
            } else if (args[a].equals("-c")) {
                repeatDelayInSeconds = Integer.parseInt(args[a + 1]);
                continuousMode = true;
            } else if (args[a].equals("-l")) {
                // Currently, just hardcode a 1MB max log file size.
                logger = new Logger(args[a + 1], 1024 * 1024);
            } else if (args[a].equals("-id")) {
                // Perform check on this id!
                ApplicationInstanceManager.CheckForInstance(args[a+1]);
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

        applicationArguments = new ApplicationArguments(sourceDir, destinationDirs.toArray(new File[]{}), repeatDelayInSeconds, keepCount, time, continuousMode);
    }

    private static void printUsage() {
        System.out.println("See README.md at https://github.com/philiprodriguez/SimpleJavaDirectoryBackup for usage information.");
    }
}
