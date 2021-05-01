package com.wynnlab.commands.tab_completers

import com.wynnlab.plugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.io.File

object MobTabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.isEmpty())
            return emptyList()

        return completeWord(
            (File(plugin.dataFolder, "mobs").list() ?: return emptyList()).asList()
                .filter { it.endsWith(".yml") }.map { it.substring(0, it.length - 4) },
            args[0])
    }
}