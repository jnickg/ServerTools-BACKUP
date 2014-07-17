package com.matthewprenger.servertools.backup.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class ConfigurationHandler {

    private static final Logger log = LogManager.getLogger(ConfigurationHandler.class.getSimpleName());

    public static String backupDirPath = "backup";
    public static String filenameTemplate = "%MONTH-%DAY-%YEAR_%HOUR-%MINUTE-%SECOND";
    public static int[] dimensionBlackList = new int[]{};
    public static boolean backupEmptyWorlds = false;
    public static String[] fileBlackList = new String[]{"level.dat_new"};
    public static String[] dirBlackList = new String[]{};

    public static int backupLifespan = -1;
    public static int backupDirMaxSize = -1;
    public static int backupMaxNumber = -1;

    public static boolean enableAutoBackup = false;
    public static int autoBackupInterval = 1440;

    public static boolean sendMessagesToOps = true;
    public static boolean sendMessagesToUsers = false;
    public static String[] messageWhiteList = new String[]{};

    public static Configuration config;

    public static void init(File cfgFile) {

        log.info("Loading configuration");

        config = new Configuration(cfgFile);

        try {
            config.load();
        } catch (Exception e) {
            log.error("Failed to load configuration, deleting and trying again", e);

            cfgFile.delete();

            config.load();
        }

        String category;
        Property prop;

        {
            category = "general";
            config.setCategoryComment(category, "General settings about backups");

            prop = config.get(category, "Backup Directory", backupDirPath);
            prop.comment = "The path to the directory where backups are stored";
            backupDirPath = prop.getString();

            prop = config.get(category, "Filename Template", filenameTemplate);
            prop.comment = "The template for backup filenames. Wildcards are: %MONTH %DAY %YEAR %HOUR %MINUTE %SECOND";
            filenameTemplate = prop.getString();

            prop = config.get(category, "File Blacklist", fileBlackList);
            prop.comment = "A list of filenames to not back up";
            fileBlackList = prop.getStringList();

            prop = config.get(category, "Directory Blacklist", dirBlackList);
            prop.comment = "A list of directory names to not back up";
            dirBlackList = prop.getStringList();

            prop = config.get(category, "Dimension Blacklist", dimensionBlackList);
            prop.comment = "A list of dimension IDs to not back up";
            dimensionBlackList = prop.getIntList();

            prop = config.get(category, "Backup Empty Worlds", backupEmptyWorlds);
            prop.comment = "If worlds should be backed up when no one is in them";
            backupEmptyWorlds = prop.getBoolean(backupEmptyWorlds);
        }
        {
            category = "History";
            config.setCategoryComment(category, "Settings related to keeping old backups");

            prop = config.get(category, "Backup Lifespan", backupLifespan);
            prop.comment = "How long to keep a backup in days. Set to -1 to disable";
            backupLifespan = prop.getInt();

            prop = config.get(category, "Max BackupDir Size", backupDirMaxSize);
            prop.comment = "The maximum size of the worlds backup directory in megabytes. Set to -1 to disable";
            backupDirMaxSize = prop.getInt();

            prop = config.get(category, "Max Number Backups", backupMaxNumber);
            prop.comment = "The maximum number of bakups to keep per world. Set to -1 to disable";
            backupMaxNumber = prop.getInt();
        }
        {
            category = "Auto Backup";
            config.setCategoryComment(category, "Settings about automatic backups");

            prop = config.get(category, "Enable AutoBackups", enableAutoBackup);
            prop.comment = "If backups should run automatically on a schedule";
            enableAutoBackup = prop.getBoolean(enableAutoBackup);

            prop = config.get(category, "AutoBackup Interval", autoBackupInterval);
            prop.comment = "The interval in minutes for the auto backup to occur";
            autoBackupInterval = prop.getInt(autoBackupInterval);
        }
        {
            category = "Messages";
            config.setCategoryComment(category, "Settings related to messages sent by the backup system");

            prop = config.get(category, "Send Messages to OPs", sendMessagesToOps);
            prop.comment = "If backup messages should be sent to server operators";
            sendMessagesToOps = prop.getBoolean(sendMessagesToOps);

            prop = config.get(category, "Send Messages to Users", sendMessagesToUsers);
            prop.comment = "If backup messages should be sent to non-ops";
            sendMessagesToUsers = prop.getBoolean(sendMessagesToUsers);

            prop = config.get(category, "Message Whitelist", messageWhiteList);
            prop.comment = "List of usernames to always send backup messages to";
            messageWhiteList = prop.getStringList();
        }

        if (config.hasChanged()) {
            config.save();
        }
    }
}
