# Simple Java Directory Backup
This is supposed to be a one-night project. The goal is simple: make a tool for routinely copying some target directory from one place to another place. The purpose is simply to prevent a case where a storage device dies and you lose all your data in some important root directory.

# Functionality
 - Make routine backups of a folder
 - Set the number of copies to be kept
 - Set how often to copy the folder
 - Set what time to copy the folder
 - Set whether to always copy the folder when the destination folder becomes available (for removable storage destinations and similar)
 - Copy to more than one destination path

# Authors
Philip Rodriguez :)

# Usage
Run the JAR artifact in the out folder:
```
java -jar SimpleJavaDirectoryBackup.jar -s /home/user/Pictures -d "/media/user/USBDRIVE/backups" -c 600 -k 3 -l someLogs -id one
```

In the example above, a copy of the folder "/home/user/Pictures" will be made at most once every 600 seconds. A maximum of 3 copies will be kept. Logs files will be generated in the working directory with the prefix "someLogs". Since -c was used (continuous mode), if a copy cannot be made because the destination is unavailable at the time, then the application will "spin-wait" continuously until the destination becomes available again and then begin copying. Again, at most one copy every 600 seconds will be made. 


# Arguments

`-s [source directory]` sets the source directory.
`-d [destination directory]` sets a destination directory. Multiple of these can be included.
`-t [time]` sets the time to try a copy, formatted in 24-hour time like hh:mm:ss. Shouldn't be used with the r or c flags.
`-r [delay in seconds]` sets the repeat delay. That is, sets how long to wait between attempts to copy. Shouldn't be used with the t or c flags.
`-k [keep count integer]` sets the keep count.
`-c [delay in seconds]` sets continuous mode with a delay of some seconds between successful copies. Shouldn't be used with the t or r flags.
`-l [log file name prefix]` when used, enables logging to a text file. Sets what the prefix of the log file name should be. Max log file size is 1MB.
`-id [lock filename]` sets an id or name of sorts for this application instance. No other instance will be allowed to run in the same working directory with the same id. This is to prevent accidentally running multiple instances of the application.
