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
First compile the thing:
```
javac -cp "src/:lib/commons-io-2.6.jar" src/Main.java
```

Then, run the thing:
```
java -cp "src/:lib/commons-io-2.6.jar" Main -s /path/to/source/dir -d /path/to/destination1 -d /path/to/destination2 -r 600 -k 3 -w 1200
```

In the example above, a copy of the folder "/path/to/source/dir" will be made every 600 seconds (ten minutes) to the two destination paths. A maximum of three copies will be kept (so on the fourth copy, the oldest copy will get deleted first). The first copy will not occur for 1200 seconds (20 minutes).


# Arguments

`-s [source directory]` sets the source directory.
`-d [destination directory]` sets a destination directory. Multiple of these can be included.
`-t [time]` sets the time to try a copy, formatted in 24-hour time like hh:mm:ss. Shouldn't be used with the r or c flags.
`-r [delay in seconds]` sets the repeat delay. That is, sets how long to wait between attempts to copy. Shouldn't be used with the t or c flags.
`-k [keep count integer]` sets the keep count.
`-w [delay in seconds]` sets the initial wait delay.
`-c [delay in seconds]` sets continuous mode with a delay of some seconds between successful copies. Shouldn't be used with the t or r flags.
`-l [y/n]` sets whether or not to log output to a text file. The file will always be in the working directory.
