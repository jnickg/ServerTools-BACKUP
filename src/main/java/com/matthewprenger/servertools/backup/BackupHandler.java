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

import com.google.common.base.Strings;
import com.matthewprenger.servertools.core.util.FileUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.DimensionManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

public class BackupHandler {

    private static final String FILE_EXTENSION = ".zip";
    private static final BackupFileNameFilter backupFileNameFilter = new BackupFileNameFilter();

    protected File backupDir;
    protected File worldDir;

    protected Timer backupTimer;

    public static BackupHandler instance;

    public BackupHandler() {

        ServerToolsBackup.log.info("Initializing ServerTools Backup Handler");

        if (Strings.isNullOrEmpty(BackupConfig.backupDirPath))
            throw new IllegalArgumentException("The configured backup path is not set");

        backupDir = new File(BackupConfig.backupDirPath);
        worldDir = DimensionManager.getWorld(0).getChunkSaveLocation();

        if (backupDir.exists() && backupDir.isFile())
            throw new IllegalArgumentException("File exists with name of configured backup path, can't create backup directory");

        ServerToolsBackup.log.trace(String.format("Backup Directory: %s", backupDir.getAbsolutePath()));

        backupDir.mkdirs();

        backupTimer = new Timer(true);

        if (BackupConfig.enableAutoBackup) {

            ServerToolsBackup.log.info(String.format("Initializing ServerTools AutoBackup to run every %s minutes", BackupConfig.autoBackupInterval));

            int interval = BackupConfig.autoBackupInterval * 60 * 1000;

            if (interval <= 0)
                throw new IllegalArgumentException("Autobackup interval must be greater than 0");

            try {
                backupTimer.scheduleAtFixedRate(
                        new Backup(worldDir, backupDir), interval, interval
                );
            } catch (IOException e) {
                ServerToolsBackup.log.fatal("Failed to initialize autobackup", e);
            }
        }

        instance = this;
    }

    /**
     * Run the server backup
     *
     * @throws java.io.IOException if an IOException happened
     */
    public void doBackup() throws IOException {

        backupTimer.schedule(
                new Backup(worldDir, backupDir), 0
        );
    }

    /**
     * Get the backup filename with wildcards replaced for the current date and time
     *
     * @return the name of the backup
     */
    public static String getBackupName() {

        Calendar cal = Calendar.getInstance();

        Integer year = cal.get(Calendar.YEAR);
        Integer month = cal.get(Calendar.MONTH) + 1; // WHY DOES JAVA THINK JANUARY IS 0 ?!?!?
        Integer day = cal.get(Calendar.DATE);
        Integer hour = cal.get(Calendar.HOUR_OF_DAY);
        Integer minute = cal.get(Calendar.MINUTE);
        Integer second = cal.get(Calendar.SECOND);

        return BackupConfig.backupFileNameTemplate.replaceAll("%YEAR", year.toString())
                .replaceAll("%MONTH", month.toString())
                .replaceAll("%DAY", day.toString())
                .replaceAll("%HOUR", hour.toString())
                .replaceAll("%MINUTE", minute.toString())
                .replaceAll("%SECOND", second.toString()) + FILE_EXTENSION;
    }

    /**
     * Check the backup directories for files older than the maximum backup age
     * set in the config and deletes them.
     *
     * Does nothing if config setting is set to -1
     */
    public void checkForOldBackups() {

        if (BackupConfig.backupLifespanDays == -1)
            return;

        ServerToolsBackup.log.info("Checking backup directory for old backups");

        for (File file : backupDir.listFiles(backupFileNameFilter)) {
            if (file.isFile()) {
                int age = (int) ((new Date().getTime() - file.lastModified()) / 24 / 60 / 60 / 1000);

                ServerToolsBackup.log.trace(String.format("Found backup file %s; %s days old", file.getName(), age));

                if (age > BackupConfig.backupLifespanDays) {
                    ServerToolsBackup.log.info(String.format("Deleting old backup file: %s", file.getName()));
                    if (file.delete())
                        ServerToolsBackup.log.trace(String.format("Successfully deleted file %s", file.getName()));
                    else
                        ServerToolsBackup.log.warn(String.format("Failed to delete file %s", file.getName()));
                }
            }
        }
    }

    /**
     * Check the size of the backup directory and delete the
     * oldest file if the directory is larger than the config option
     */
    public void checkBackupDirSize() {

        if (BackupConfig.backupDirMaxSize == -1)
            return;

        ServerToolsBackup.log.trace("Checking size of the backup directory");

        ServerToolsBackup.log.trace(String.format("Backup directory size: %s MB", FileUtils.getFolderSize(backupDir) / org.apache.commons.io.FileUtils.ONE_MB));

        while (FileUtils.getFolderSize(backupDir) / org.apache.commons.io.FileUtils.ONE_MB > BackupConfig.backupDirMaxSize) {

            File oldestFile = FileUtils.getOldestFile(backupDir);

            if (oldestFile != null) {
                ServerToolsBackup.log.trace(String.format("Deleting oldest file: %s", oldestFile.getName()));
                oldestFile.delete();
            }
        }
    }

    /**
     * Check the number of backups in the backup directory and delete old ones if necessary
     */
    public void checkNumberBackups() {

        if (BackupConfig.backupMaxNumber == -1)
            return;

        ServerToolsBackup.log.trace("Checking number of backups in backup directory");

        ServerToolsBackup.log.trace(String.format("%s backups exist", getNumberBackups()));

        while (getNumberBackups() > BackupConfig.backupMaxNumber) {
            File oldestFile = FileUtils.getOldestFile(backupDir);
            if (oldestFile != null) {
                ServerToolsBackup.log.info(String.format("Deleting oldest backup file: %s", oldestFile.getName()));
                oldestFile.delete();
            }
        }
    }

    /**
     * Get the number of backups in the backup directory
     *
     * @return the number of backups
     */
    private int getNumberBackups() {

        int number = 0;

        if (backupDir.exists() && backupDir.isDirectory()) {

            File[] files = backupDir.listFiles(backupFileNameFilter);

            for (File ignored : files) {
                number++;
            }
        }

        return number;
    }

    /**
     * Send a backup related message to all users that should get backup messages
     *
     * @param component a {@link net.minecraft.util.ChatComponentText} to send
     */
    public static void sendBackupMessage(IChatComponent component) {

        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP playerMP = (EntityPlayerMP) obj;

            if (playerMP != null) {
                if (BackupConfig.sendBackupMessageToOps && MinecraftServer.getServer().getConfigurationManager().func_152596_g(playerMP.getGameProfile()))
                    playerMP.addChatComponentMessage(component);
                else if (BackupConfig.sendBackupMessageToUsers)
                    playerMP.addChatComponentMessage(component);
                else if (BackupConfig.backupMessageWhitelist.contains(playerMP.getCommandSenderName()))
                    playerMP.addChatComponentMessage(component);
            }
        }

        MinecraftServer.getServer().addChatMessage(component);
    }

    private static class BackupFileNameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(FILE_EXTENSION);
        }
    }
}
