package com.matthewprenger.servertools.backup;

import com.matthewprenger.servertools.backup.config.ConfigurationHandler;
import net.minecraft.world.WorldServer;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldBackup implements IBackup {

    private Thread thread = new Thread(this);

    boolean flag;

    public final WorldServer worldServer;
    public final File destinationFile;

    private final File sourceDir;

    private final Set<String> ignoredFiles = new HashSet<>();
    private final Set<String> ignoredDirs = new HashSet<>();

    public WorldBackup(WorldServer worldServer, File destinationFile) {
        this.worldServer = worldServer;
        this.destinationFile = destinationFile;

        this.sourceDir = worldServer.getChunkSaveLocation();

        if (worldServer.provider.dimensionId == 0) { // Ignore other dimensions from this backup
            File[] files = sourceDir.listFiles();

            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("DIM")) {
                        ignoredDirs.add(f.getName());
                    }
                }
            }
        }

        Collections.addAll(ignoredDirs, ConfigurationHandler.dirBlackList);
        Collections.addAll(ignoredFiles, ConfigurationHandler.fileBlackList);
    }

    @Override
    public File getSourceDir() {
        return sourceDir;
    }

    @Override
    public File getDestinationFile() {
        return destinationFile;
    }

    public void start() {
        thread.start();
    }

    public boolean isDone() {
        return thread.isAlive();
    }

    @Override
    public void run() {
        try {

            flag = worldServer.levelSaving;
            worldServer.levelSaving = true;

            zipDirectory(sourceDir, destinationFile, ignoredDirs, ignoredFiles); // Do the actual backup

        } catch (IOException e) {
            ServerToolsBackup.log.error("Failed to do backup of DIM: " + worldServer.provider.dimensionId, e);
        } finally {
            worldServer.levelSaving = flag;

            BackupHandler.checkBackupDirSize();
            BackupHandler.checkNumberBackups();
            BackupHandler.checkForOldBackups();
        }
    }

    static void zipDirectory(File directory, File zipfile, Collection<String> ignoredDirs, Collection<String> ignoredFiles) throws IOException {
        URI baseDir = directory.toURI();
        Deque<File> queue = new LinkedList<>();
        queue.push(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.removeFirst();
                File[] dirFiles = directory.listFiles();
                if (dirFiles != null && dirFiles.length != 0) {
                    for (File child : dirFiles) {
                        if (child != null) {
                            String name = baseDir.relativize(child.toURI()).getPath();
                            if (child.isDirectory() && !ignoredDirs.contains(child.getName())) {
                                queue.push(child);
                                name = name.endsWith("/") ? name : name + "/";
                                zout.putNextEntry(new ZipEntry(name));
                            } else if (child.isFile()) {
                                if (!ignoredFiles.contains(child.getName())) {
                                    zout.putNextEntry(new ZipEntry(name));
                                    copy(child, zout);
                                    zout.closeEntry();
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            copy(in, out);
        }
    }
}
