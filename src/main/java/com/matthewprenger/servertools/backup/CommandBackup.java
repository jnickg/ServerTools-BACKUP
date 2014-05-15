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

import com.matthewprenger.servertools.core.command.ServerToolsCommand;
import com.matthewprenger.servertools.core.util.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

public class CommandBackup extends ServerToolsCommand {

    public CommandBackup(String defaultName) {
        super(defaultName);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/" + name;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        try {
            BackupHandler.instance.doBackup();
        } catch (IOException e) {
            e.printStackTrace();
            sender.addChatMessage(Util.getChatComponent("Backup Failed", EnumChatFormatting.RED));
        }

        sender.addChatMessage(Util.getChatComponent("Started Backup", EnumChatFormatting.GREEN));
    }
}
