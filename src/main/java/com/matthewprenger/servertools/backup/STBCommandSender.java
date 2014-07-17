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

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public enum STBCommandSender implements ICommandSender {
    INSTANCE {
        @Override
        public String getCommandSenderName() {
            return "ServerTools-Backup";
        }

        @Override
        public IChatComponent func_145748_c_() {

            return new ChatComponentText(this.getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent conponent) {

            ServerToolsBackup.log.info(conponent.getUnformattedText());
        }

        @Override
        public boolean canCommandSenderUseCommand(int var1, String var2) {

            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {

            return new ChunkCoordinates(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {

            return MinecraftServer.getServer().worldServers[0];
        }
    }


}
