import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CopyIterationRunnable implements Runnable {

    private final ApplicationArguments applicationArguments;
    private final Logger logger;
    private final ScheduledExecutorService scheduledExecutorService;

    public CopyIterationRunnable(ApplicationArguments applicationArguments, ScheduledExecutorService scheduledExecutorService, Logger logger) {
        this.applicationArguments = applicationArguments;
        this.scheduledExecutorService = scheduledExecutorService;
        this.logger = logger;
    }

    @Override
    public void run() {
        // Perform copy operation
        boolean successfulCopyOperation = false;
        for (File destination : applicationArguments.getDestinations()) {
            logger.log("Processing destination " + destination.getAbsolutePath());
            if (destination.exists() && destination.isDirectory()) {
                try {
                    // First, delete oldest if necessary!
                    File[] existingFiles = destination.listFiles();
                    if (existingFiles.length >= applicationArguments.getKeepCount()) {
                        logger.log("Too many entries in destination, removing oldest!");
                        File oldest = existingFiles[0];
                        for (File file : existingFiles) {
                            if (!file.isDirectory())
                                throw new Exception("Invalid file located in destination directory: " + file.getName());
                            if (Long.parseLong(file.getName()) < Long.parseLong(oldest.getName())) {
                                oldest = file;
                            }
                        }
                        logger.log("Removing " + oldest.getAbsolutePath().toString());
                        FileUtils.deleteDirectory(oldest);
                        logger.log("Removed!");
                    }

                    // Now, put new copy!
                    File timeDest = new File(destination.getAbsolutePath().toString() + "/" + System.currentTimeMillis());
                    logger.log("Copying to " + timeDest.getAbsolutePath().toString());
                    long startTime = System.currentTimeMillis();

                    // Manually copy files to ignore exceptions on any single file...
                    copyDirectory(applicationArguments.getSource(), timeDest, timeDest);

                    long endTime = System.currentTimeMillis();
                    logger.log("Copy complete! Took " + ((endTime - startTime) / 60000) + " minutes.");
                    successfulCopyOperation = true;
                } catch (Exception exc) {
                    exc.printStackTrace();
                    logger.log("Copy failed! " + exc.getMessage());
                }
            } else {
                logger.log("Destination directory does not exist!");
            }
        }

        // If we're in continuousMode...
        if (applicationArguments.isContinuousMode()) {
            // AND our copy just succeeded...
            if (successfulCopyOperation) {
                logger.log("Successful continuous mode copy, scheduling next operation to happen in " + applicationArguments.getRepeatDelayInSeconds() + " seconds...");
                scheduledExecutorService.schedule(this, applicationArguments.getRepeatDelayInSeconds(), TimeUnit.SECONDS);
            } else {
                // Unsuccessful so we'll wait only for a destination to become available
                logger.log("Waiting for a destination to become available...");
                couter:
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (File destination : applicationArguments.getDestinations()) {
                        if (destination.exists() && destination.isDirectory()) {
                            logger.log("Destination available: " + destination.toString());
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break couter;
                        }
                    }
                }
                scheduledExecutorService.schedule(this, 0, TimeUnit.SECONDS);
            }
        } else if (applicationArguments.getTime() == null) {
            logger.log("Scheduled next copy to occur in " + applicationArguments.getRepeatDelayInSeconds() + " seconds...");
            scheduledExecutorService.schedule(this, applicationArguments.getRepeatDelayInSeconds(), TimeUnit.SECONDS);
        } else {
            // We want to wait until the next time!
            Time curTime = new Time(System.currentTimeMillis());
            long waitTimeMs = curTime.getWaitPeriod(applicationArguments.getTime());
            logger.log("Scheduling copy for next occurrance of " + applicationArguments.getTime());
            scheduledExecutorService.schedule(this, waitTimeMs, TimeUnit.MILLISECONDS);
        }
    }

    /*
      Copy source to destination, creating destination if needed. Ignore any
      exceptions that occur on any single file (e.g. permission issues, etc).
      Also, ignore any directories containing a file named ".sjdbignore".
    */
    private void copyDirectory(File source, File destination, File originalDestination) throws Exception {
        File[] dirFiles = source.listFiles();
        // Does this path contain an .sjdbignore file?
        for (File f : dirFiles) {
            if (f.getName().equals(".sjdbignore")) {
                // We are done! Do not copy this directory at all!
                logger.log("Ignoring directory " + source.getAbsolutePath() + " because it contains an sjdbignore file!");
                return;
            }
        }
        for (File f : dirFiles) {
            try {
                File equivalent = Paths.get(destination.toPath().toString(), f.getName()).toFile();
                if (f.isDirectory()) {
                    if (equivalent.mkdirs()) {
                        copyDirectory(f, equivalent, originalDestination);
                    } else {
                        logger.log("Failed to create directory at " + equivalent.getAbsolutePath());
                    }
                } else if (f.isFile()) {
                    FileUtils.copyFile(f, equivalent);
                } else {
                    logger.log("Ignoring unrecognized entity at " + f.getAbsolutePath());
                }
            } catch (Exception exc) {
                logger.log("Failed to copy entity at " + f.getAbsolutePath() + ": " + exc.getMessage());
                throwIfDisconnected(originalDestination, exc);
            }
        }
    }

    private static void throwIfDisconnected(File file, Exception exc) throws Exception {
        if (!file.exists() || (exc.getMessage() != null && exc.getMessage().toLowerCase().contains("read-only")) || (exc.getMessage() != null && exc.getMessage().toLowerCase().contains("input/output")))
            throw new Exception("Failed disconnected check!");
    }
}
