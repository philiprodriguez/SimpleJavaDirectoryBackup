import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/*
    This is a class whose job is to provide an easy way to determine if the application is already running, and to
    make a footprint of this application's running state.
 */
public class ApplicationInstanceManager {
    public static void CheckForInstance(String id) {
        try {
            File file = new File(id + ".lock");
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock == null) {
                System.err.println("Exiting since application already running!");
                System.exit(1);
            } else {
                // We now hold the lock...
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.err.println("Failed to release lock on file!");
                        }
                    }
                }));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            System.err.println("Exiting since application seemingly already running!");
            System.exit(1);
        }
    }
}
