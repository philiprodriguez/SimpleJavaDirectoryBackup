import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger {

    private final String filePrefix;
    private final int maxFileSize;
    private final AtomicInteger currentFileNumber;

    public Logger(String filePrefix, int maxFileSize) {
        this.filePrefix = filePrefix;
        this.maxFileSize = maxFileSize;
        this.currentFileNumber = new AtomicInteger(Math.max(1, getCurrentLogFileCount()));
    }

    /**
     *
     * @return The number of log fies with filePrefix as the prefix in the working directory.
     */
    private synchronized int getCurrentLogFileCount() {
        File[] filesList = new File("./").listFiles();
        if (filesList == null)
            throw new IllegalStateException("Unable to determine current file number.");
        int matchedFiles = 0;
        for (File file : filesList) {
            if (file.getName().matches(filePrefix + "([0-9])*\\.log")) {
                matchedFiles++;
            }
        }
        return matchedFiles;
    }

    /**
     *
     * @return A File representing the current File to write to.
     */
    private synchronized File getCurrentFile(int messageLength) {
        File currentFile = new File(filePrefix + currentFileNumber.get() + ".log");
        if (currentFile.length() + messageLength > maxFileSize) {
            return new File(filePrefix + currentFileNumber.incrementAndGet() + ".log");
        }
        return currentFile;
    }

    public synchronized void log(String message) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(new Date().toString());
        stringBuffer.append(": ");
        stringBuffer.append(message);

        System.out.println(stringBuffer.toString());

        if (filePrefix == null)
            return;

        try (PrintWriter printWriter = new PrintWriter(new FileWriter(getCurrentFile(stringBuffer.toString().length()), true))) {
            printWriter.println(stringBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }
}
