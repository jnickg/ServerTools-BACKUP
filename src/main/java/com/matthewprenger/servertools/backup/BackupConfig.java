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

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BackupConfig {

    public static String backupDirPath = "backup";
    public static String backupFileNameTemplate = "%MONTH-%DAY-%YEAR_%HOUR-%MINUTE-%SECOND";
    public static int backupLifespanDays = -1;
    public static int backupDirMaxSize = -1;
    public static int backupMaxNumber = -1;
    public static final Set<String> fileBlacklist = new HashSet<>();
    public static final Set<String> directoryBlackList = new HashSet<>();
    public static boolean sendBackupMessageToOps = true;
    public static boolean sendBackupMessageToUsers = true;
    public static final Set<String> backupMessageWhitelist = new HashSet<>();
    public static boolean enableAutoBackup = false;
    public static int autoBackupInterval = 1440;

    public static void init(File file) {

        Configuration config;

        try {
            config = new Configuration(file);
        } catch (Exception e) {
            ServerToolsBackup.log.fatal("Error loading configuration, deleting and retrying", e);

            if (file.exists())
                file.delete();

            config = new Configuration(file);
        }

        config.load();

        String category;
        Property prop;
        String[] array;

        /* General Settings */
        category = "general";

        prop = config.get(category, "backupDir", backupDirPath);
        prop.comment = "This is the root location where backups will be stored";
        backupDirPath = prop.getString();

        prop = config.get(category, "filename", backupFileNameTemplate);
        prop.comment = "The template that is used for each backup file" +
                "Valid wildcards are: MONTH %DAY %YEAR %HOUR %MINUTE %SECOND";
        backupFileNameTemplate = prop.getString();

        prop = config.get(category, "daysToKeepBackups", backupLifespanDays);
        prop.comment = "The number of days that the backup will be kept for, " +
                "Set -1 to disable";
        backupLifespanDays = prop.getInt();

        prop = config.get(category, "maxBackupDirSize", backupDirMaxSize);
        prop.comment = "The maximum size of the backup directory in Megabytes, " +
                "Set to -1 to disable";
        backupDirMaxSize = prop.getInt();

        prop = config.get(category, "maxNumberBackups", backupMaxNumber);
        prop.comment = "The maximum number of backups that will be kept in the backup directory, " +
                "Set to -1 to disable";
        backupMaxNumber = prop.getInt(backupMaxNumber);

        prop = config.get(category, "sendBackupMessageToOps", sendBackupMessageToOps);
        prop.comment = "Send backup related messages to server operators";
        sendBackupMessageToOps = prop.getBoolean(sendBackupMessageToOps);

        prop = config.get(category, "sendBackupMessageToUsers", sendBackupMessageToUsers);
        prop.comment = "Send backup related messages to all users";
        sendBackupMessageToUsers = prop.getBoolean(sendBackupMessageToUsers);

        prop = config.get(category, "fileBlackList", "");
        prop.comment = "Comma separated list of files to not back up";
        array = prop.getString().split(",");
        if (array.length > 0)
            Collections.addAll(fileBlacklist, array);
        fileBlacklist.add("level.dat_new"); /* Minecraft Temp File - Causes Backup Problems */

        prop = config.get(category, "directoryBlackList", "");
        prop.comment = "Comma separated list of directory names to not back up";
        array = prop.getString().split(",");
        if (array.length > 0)
            Collections.addAll(directoryBlackList, array);

        prop = config.get(category, "backupMessageWhitelist", "");
        prop.comment = "A Comma separated list of users to always send backup related messages to";
        array = prop.getString().split(",");
        if (array.length > 0)
            Collections.addAll(backupMessageWhitelist, array);

        /* AutoBackup Settings */
        category = "autoBackup";

        prop = config.get(category, "enableAutoBackup", enableAutoBackup);
        prop.comment = "Enable automatic backups at specified intervals";
        enableAutoBackup = prop.getBoolean(enableAutoBackup);

        prop = config.get(category, "autoBackupInterval", autoBackupInterval);
        prop.comment = "The interval in minutes for the auto backup to occur";
        autoBackupInterval = prop.getInt(autoBackupInterval);

        if (config.hasChanged())
            config.save();
    }
}
