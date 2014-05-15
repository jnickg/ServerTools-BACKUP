/*
 * Copyright 2014 Matthew Prenger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.matthewprenger.servertools.backup;

import com.matthewprenger.servertools.core.util.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.WorldServer;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Backup extends TimerTask {

    private final File sourceDir;
    private final File backupDir;

    public Backup(File sourceDir, File backupDir) throws IOException {

        this.sourceDir = sourceDir;
        this.backupDir = backupDir;

        if (!sourceDir.exists())
            throw new FileNotFoundException("The given backup source path doesn't exist");

        if (!sourceDir.isDirectory())
            throw new IOException("The given backup source path is a file");

        if (backupDir.exists() && !backupDir.isDirectory())
            throw new IOException("The given backup directory exists and isn't a directory");

        backupDir.mkdirs();
    }

    @Override
    public void run() {

        String backupFileName = BackupHandler.getBackupName();

        ServerToolsBackup.log.info(String.format("Starting backup %s", backupFileName));
        BackupHandler.sendBackupMessage(new ChatComponentText("Starting Server Backup"));

        Map<Integer, Boolean> levelSavingDisabled = new HashMap<>(MinecraftServer.getServer().worldServers.length);

        try {

            for (WorldServer worldServer : MinecraftServer.getServer().worldServers) {
                levelSavingDisabled.put(worldServer.provider.dimensionId, worldServer.levelSaving);

                worldServer.levelSaving = false;
                worldServer.saveAllChunks(true, null);

            }

            // Do the actual backup
            zipDirectory(sourceDir, new File(backupDir, backupFileName));

            for (WorldServer worldServer : MinecraftServer.getServer().worldServers) {
                worldServer.levelSaving = levelSavingDisabled.get(worldServer.provider.dimensionId);
            }

            BackupHandler.sendBackupMessage(Util.getChatComponent("Server Backup Finished", EnumChatFormatting.GREEN));
            ServerToolsBackup.log.info("Backup completed successfully");

        } catch (Exception e) {
            ServerToolsBackup.log.fatal("Failed to make backup: " + backupFileName, e);
            BackupHandler.sendBackupMessage(Util.getChatComponent("Server Backup Error - Check Server Logs", EnumChatFormatting.RED));
        }

        BackupHandler.instance.checkBackupDirSize();
        BackupHandler.instance.checkForOldBackups();
        BackupHandler.instance.checkNumberBackups();
    }

    static void zipDirectory(File directory, File zipfile) throws IOException {
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
                            if (child.isDirectory() && !BackupConfig.directoryBlackList.contains(child.getName())) {
                                queue.push(child);
                                name = name.endsWith("/") ? name : name + "/";
                                zout.putNextEntry(new ZipEntry(name));
                            } else {
                                if (!BackupConfig.fileBlacklist.contains(child.getName())) {
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
