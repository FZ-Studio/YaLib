package com.rabbitown.yalib.command

import com.rabbitown.yalib.command.entity.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import java.util.regex.Pattern

/**
 * @author Yoooooory
 */
internal class CommandProcessor() : TabExecutor {

    private val argRegex = Regex("\\{(\\w+)(: ?(.+))?}")

    private val remotes = mutableListOf<RemoteEntity>()

    constructor(remote: CommandRemote) : this() {
        addRemote(remote)
    }

    fun addRemote(remote: CommandRemote) {
        remotes += RemoteEntity(remote)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val running = CommandRunning(sender, command, label, args).apply { pathArgMap["start"] = System.nanoTime() }
        val sb = StringBuilder(if (args.isEmpty()) "" else args[0])
        for (i in 1 until args.size) sb.append(" ${args[i]}")
        // Looking for an effective remote. (detect paths)
        val remote = remotes.firstOrNull { remote ->
            val path = remote.remote.path
            if (path.isEmpty()) return@firstOrNull true
            val matcher = handlerPattern(remote, path.replace(argRegex) { it.groups[3]?.value ?: ".+" })
                .matcher(sb.toString())
            if (matcher.find() && matcher.start() == 0) {
                // For an effective remote, check whether the sender is accessible to it.
                when (CommandResult.getCommandResult(remote.remote, sender)) {
                    CommandResult.SUCCESS -> {
                        running.putArguments(path, sb.toString())
                        sb.replace(0, sb.length, sb.substring(matcher.end()))
                        return@firstOrNull true
                    }
                    CommandResult.FAILED_SENDER_MISMATCH ->
                        remote.runDefaultSenderDeniedHandler(running)
                    CommandResult.FAILED_PERMISSION_REQUIRED ->
                        remote.runDefaultPermissionDeniedHandler(running)
                }
                return true
            } else false
        } ?: return false
        // Looking for an effective action. (detect paths)
        val action = remote.actions.firstOrNull out@{ handler ->
            // Magic code, DO NOT edit it!!
            (handler.action as Array<*>).firstOrNull { action ->
                val path = "${if (handler.path.isEmpty()) "" else "${handler.path} "}$action"
                if (path.isEmpty()) return@firstOrNull true
                if (sb.toString().trim()
                        .matches(handlerRegex(handler, path.replace(argRegex) { it.groups[3]?.value ?: ".+" }))
                ) {
                    // For an effective action, check whether the sender is accessible to it.
                    when (CommandResult.getCommandResult(handler, sender)) {
                        CommandResult.SUCCESS -> {
                            running.putArguments(path, sb.toString())
                            return@firstOrNull true
                        }
                        CommandResult.FAILED_SENDER_MISMATCH -> AccessHandler.getAccessibleOrNull(
                            handler.senderDeniedHandlers + remote.defaultSenderDeniedHandlers, sender
                        )?.invoke(remote.remote, handler, running)
                        CommandResult.FAILED_PERMISSION_REQUIRED -> AccessHandler.getAccessibleOrNull(
                            handler.permissionDeniedHandlers + remote.defaultPermissionDeniedHandlers, sender
                        )?.invoke(remote.remote, handler, running)
                    }
                    return true
                } else false
            } != null // means @action found a matched action, then the handler is what we need.
        } ?: return false
        // Running command.
        action.invoke(remote.remote, running)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, alias: String, args: Array<out String>
    ): List<String> {
        val running = CommandRunning(sender, command, alias, args).apply { pathArgMap["start"] = System.nanoTime() }
        val sb = StringBuilder(if (args.isEmpty()) "" else args[0])
        for (i in 1 until args.size) sb.append(" ${args[i]}")
        var remotePath = ""
        val split = sb.replace(Regex("\".*\" "), ". ").trim().split(" ")
        if (split.size < 0) return emptyList()
        var index = 0
        // Looking for an effective remote. (detect paths)
        val remote = remotes.firstOrNull { remote ->
            remotePath = remote.remote.path + " "
            val iter = split.listIterator()
            if (remotePath.isEmpty()) return@firstOrNull true
            remotePath.replace(argRegex) { it.groups[3]?.value ?: ".+" }.split(" ").forEach {
                if (iter.hasNext()) {
                    index++
                    if (!iter.next().matches(handlerRegex(remote, it))) {
                        index = 0
                        return@firstOrNull false
                    }
                }
                return remote.runDefaultCompleter(index, remotePath.trim(), running)
            }
            return@firstOrNull true
        } ?: return emptyList()
        // Looking for an effective action. (detect paths)
        remote.actions.firstOrNull out@{ handler ->
            // Magic code, DO NOT edit it!!
            (handler.action as Array<*>).firstOrNull { action ->
                action as String
                val iter = split.listIterator(index)
                val oriIndex = index
                if (action.isEmpty()) return@firstOrNull true
                action.replace(argRegex) { it.groups[3]?.value ?: ".+" }.split(" ").forEach {
                    if (iter.hasNext()) {
                        index++
                        if (!iter.next().matches(handlerRegex(handler, it))) {
                            index = oriIndex
                            return@firstOrNull false
                        }
                    }
                    return CompleterHandler.getAccessibleOrNull(handler.completers, sender)
                        ?.invoke(index, "$remotePath$action", remote.remote, running) ?: emptyList()
                }
                return@firstOrNull false
            } != null // means @action found a matched action, then the handler is what we need.
        }
        return remote.runDefaultCompleter(index, remotePath.trim(), running)
    }

    private fun handlerRegex(handler: MainHandler, regex: String) =
        if (handler.ignoreCase) Regex(regex, RegexOption.IGNORE_CASE) else Regex(regex)

    private fun handlerPattern(handler: MainHandler, regex: String) =
        if (handler.ignoreCase) Pattern.compile(regex, Pattern.CASE_INSENSITIVE) else Pattern.compile(regex)

}