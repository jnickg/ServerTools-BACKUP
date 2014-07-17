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

import com.matthewprenger.servertools.backup.config.ConfigurationHandler;
import com.matthewprenger.servertools.core.STVersion;
import com.matthewprenger.servertools.core.ServerTools;
import com.matthewprenger.servertools.core.command.CommandManager;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLFingerprintViolationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, dependencies = Reference.DEPENDENCIES, acceptableRemoteVersions = "*", certificateFingerprint = Reference.FINGERPRINT)
public class ServerToolsBackup {

    public static final Logger log = LogManager.getLogger(Reference.MOD_ID);

    @Mod.Instance
    public static ServerToolsBackup instance;

    private BackupHandler backupHandler;

    @Mod.EventHandler
    public void invalidCert(FMLFingerprintViolationEvent event) {

        log.warn("Invalid ServerTools Backup fingerprint detected: {}", event.fingerprints.toString());
        log.warn("Expected: {}", event.expectedFingerprint);
        log.warn("Unpredictable results my occur");
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        STVersion.checkVersion("@MIN_CORE@");

        File backupDir = new File(ServerTools.serverToolsDir, "backup");
        backupDir.mkdirs();

        File old = new File(backupDir, "backup.cfg");
        if (old.exists()) {
            log.info("Detected old config file, deleting");
            old.delete();
        }

        ConfigurationHandler.init(new File(backupDir, "backups.cfg"));
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {

        CommandManager.registerSTCommand(new CommandBackup("backup"));
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {

        if (backupHandler == null) backupHandler = new BackupHandler();
    }
}
