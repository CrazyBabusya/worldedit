// $Id$
/*
 * WorldEdit
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.bukkit.migration;

import com.sk89q.util.yaml.YAMLProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import com.nijikokun.bukkit.Permissions.Permissions;

public class NijiPermissionsResolver implements PermissionsResolver {
    private Server server;
    private Permissions api;

    public static PermissionsResolver factory(Server server, YAMLProcessor config) {
        PluginManager pluginManager = server.getPluginManager();

        Plugin plugin = pluginManager.getPlugin("Permissions");

        // Check if plugin is loaded and has Permissions interface
        if (plugin == null || !(plugin instanceof Permissions)) {
            return null;
        }

        // Check for fake permissions
        if (config.getBoolean("ignore-nijiperms-bridges", true) && isFakeNijiPerms(plugin)) {
            return null;
        }

        return new NijiPermissionsResolver(server, (Permissions) plugin);
    }

    public void load() {

    }

    public NijiPermissionsResolver(Server server, Permissions plugin) {
        this.server = server;
        this.api = plugin;
    }

    @SuppressWarnings("static-access")
    public boolean hasPermission(String name, String permission) {
        try {
            Player player = server.getPlayerExact(name);
            if (player == null) return false;
            try {
                return api.getHandler().has(player, permission);
            } catch (Throwable t) {
                return api.Security.permission(player, permission);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public boolean hasPermission(String worldName, String name, String permission) {
        try {
            try {
                return api.getHandler().has(worldName, name, permission);
            } catch (Throwable t) {
                return api.getHandler().has(server.getPlayerExact(name), permission);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("static-access")
    public boolean inGroup(String name, String group) {
        try {
            Player player = server.getPlayerExact(name);
            if (player == null) return false;
            try {
                return api.getHandler().inGroup(player.getWorld().getName(), name, group);
            } catch (Throwable t) {
                return api.Security.inGroup(name, group);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("static-access")
    public String[] getGroups(String name) {
        try {
            Player player = server.getPlayerExact(name);
            if (player == null) return new String[0];
            String[] groups = null;
            try {
                groups = api.getHandler().getGroups(player.getWorld().getName(), player.getName());
            } catch (Throwable t) {
                String group = api.Security.getGroup(player.getWorld().getName(), player.getName());
                if (group != null) groups = new String[] { group };
            }
            if (groups == null) {
                return new String[0];
            } else {
                return groups;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return new String[0];
        }
    }

    public static boolean isFakeNijiPerms(Plugin plugin) {
        PluginCommand permsCommand = Bukkit.getServer().getPluginCommand("permissions");

        return permsCommand == null || !(permsCommand.getPlugin().equals(plugin));
    }

    public String getDetectionMessage() {
        return "Permissions plugin detected! Using Permissions plugin for permissions.";
    }
}
