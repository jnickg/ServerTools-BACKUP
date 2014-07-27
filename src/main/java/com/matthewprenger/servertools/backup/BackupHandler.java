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
import com.matthewprenger.servertools.backup.config.ConfigurationHandler;
import com.matthewprenger.servertools.core.util.FileUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BackupHandler {

    private static final String FILE_EXTENSION = ".zip";

    protected static File backupDir;

    public static void init() {

        ServerToolsBackup.log.info("Initializing ServerTools Backup Handler");

        if (Strings.isNullOrEmpty(ConfigurationHandler.backupDirPath))
            throw new IllegalArgumentException("The configured backup path is not set");

        backupDir = new File(ConfigurationHandler.backupDirPath);

        if (backupDir.exists() && backupDir.isFile())
            throw new IllegalArgumentException("File exists with name of configured backup path, can't create backup directory");

        ServerToolsBackup.log.trace(String.format("Backup Directory: %s", backupDir.getAbsolutePath()));

        backupDir.mkdirs();
    }


    /**
     * Get the backup filename with wildcards replaced for the current date and time
     *
     * @return the name of the backup
     */
    public static String getBackupName() {

        return new SimpleDateFormat("MM-dd-yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static void doBackups() {

        outer:
        for (int dimId : DimensionManager.getIDs()) {
            for (int blackListed : ConfigurationHandler.dimensionBlackList) {
                if (dimId == blackListed)
                    continue outer;
            }
        }
    }

    public static void backupWorld(int dim) {

        WorldServer worldServer = DimensionManager.getWorld(dim);

        if (worldServer == null) {
            ServerToolsBackup.log.warn("Not backing up dimension {}, it doesn't exist!", dim);
            return;
        }

        File dimfolder = new File(backupDir, "DIM_" + dim);
        dimfolder.mkdirs();
        File backupFile = new File(dimfolder, getBackupName());

        WorldBackup worldBackup = new WorldBackup(worldServer, backupFile);

        try {
            worldServer.levelSaving = false;
            worldServer.saveAllChunks(true, null);
            worldServer.levelSaving = true;

        } catch (MinecraftException e) {
            ServerToolsBackup.log.warn("Failed to save world", e);
        }

        worldBackup.start();
    }

    public static void checkForOldBackups() {

        if (ConfigurationHandler.backupLifespan == -1)
            return;

        ServerToolsBackup.log.info("Checking backup directories for old backups");

        for (File dir : backupDir.listFiles()) {
            if (dir.isFile())
                continue;

            for (File file : dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(FILE_EXTENSION);
                }
            })) {
                if (file.isFile()) {
                    int age = (int) ((new Date().getTime() - file.lastModified()) / 24 / 60 / 60 / 1000);

                    ServerToolsBackup.log.trace(String.format("Found backup file %s; %s days old", file.getName(), age));

                    if (age > ConfigurationHandler.backupLifespan) {
                        ServerToolsBackup.log.info(String.format("Deleting old backup file: %s", file.getName()));
                        if (file.delete())
                            ServerToolsBackup.log.trace(String.format("Successfully deleted file %s", file.getName()));
                        else
                            ServerToolsBackup.log.warn(String.format("Failed to delete file %s", file.getName()));
                    }
                }
            }
        }
    }

    public static void checkBackupDirSize() {

        if (ConfigurationHandler.backupDirMaxSize == -1)
            return;

        for (File file : backupDir.listFiles()) {

            if (file.isFile())
                continue;

            ServerToolsBackup.log.trace("Checking size of the backup directory");

            ServerToolsBackup.log.trace(String.format("Backup directory size: %s MB", FileUtils.getFolderSize(file) / org.apache.commons.io.FileUtils.ONE_MB));

            while (FileUtils.getFolderSize(file) / org.apache.commons.io.FileUtils.ONE_MB > ConfigurationHandler.backupDirMaxSize) {

                File oldestFile = FileUtils.getOldestFile(file);

                if (oldestFile != null) {
                    ServerToolsBackup.log.trace(String.format("Deleting oldest file: %s", oldestFile.getName()));
                    oldestFile.delete();
                }
            }
        }
    }

    public static void checkNumberBackups() {

        if (ConfigurationHandler.backupMaxNumber == -1)
            return;

        for (File dir : backupDir.listFiles()) {

            ServerToolsBackup.log.trace("Checking number of backups in backup directory");

            ServerToolsBackup.log.trace(String.format("%s backups exist", getNumberBackups(dir)));

            while (getNumberBackups(dir) > ConfigurationHandler.backupMaxNumber) {
                File oldestFile = FileUtils.getOldestFile(dir);
                if (oldestFile != null) {
                    ServerToolsBackup.log.info(String.format("Deleting oldest backup file: %s", oldestFile.getName()));
                    oldestFile.delete();
                }
            }
        }
    }

    private static int getNumberBackups(File dir) {

        int number = 0;

        if (dir.exists() && dir.isDirectory()) {

            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(FILE_EXTENSION);
                }
            });

            for (File ignored : files) {
                number++;
            }
        }

        return number;
    }

    public static void sendBackupMessage(IChatComponent component) {

        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP playerMP = (EntityPlayerMP) obj;

            if (playerMP != null) {
                if (ConfigurationHandler.sendMessagesToOps && MinecraftServer.getServer().getConfigurationManager().func_152596_g(playerMP.getGameProfile()))
                    playerMP.addChatComponentMessage(component);
                else if (ConfigurationHandler.sendMessagesToUsers)
                    playerMP.addChatComponentMessage(component);
                else {
                    for (String user : ConfigurationHandler.messageWhiteList) {
                        if (user.equalsIgnoreCase(playerMP.getCommandSenderName())) {
                            playerMP.addChatComponentMessage(component);
                        }
                    }
                }
            }
        }

        MinecraftServer.getServer().addChatMessage(component);
    }
}
