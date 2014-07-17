package com.matthewprenger.servertools.backup;

import com.matthewprenger.servertools.backup.config.ConfigurationHandler;

public class BackupTimer {

    private static long lastBackup = 0;

    public static void update() {

        long dif = System.currentTimeMillis() - lastBackup;

        if (dif >= ConfigurationHandler.autoBackupInterval * 60 * 60) {
            BackupHandler.doBackups();
            lastBackup = System.currentTimeMillis();
        }
    }
}
