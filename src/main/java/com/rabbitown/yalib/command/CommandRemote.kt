package com.rabbitown.yalib.command

import com.rabbitown.yalib.YaLib
import com.rabbitown.yalib.command.annotation.*
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Represent a command remote.
 *
 * @author Yoooooory
 */
open class CommandRemote(val command: PluginCommand) : MainHandler {

    final override val path: String
    final override val ignoreCase: Boolean
    final override val access = Access.get(this::class.java)
    final override val priority = Priority.get(this::class.java)

    init {
        val annotation = Path.get(this::class.java)
        path = annotation.path
        ignoreCase = annotation.ignoreCase
    }

    constructor(
        name: String, plugin: JavaPlugin = YaLib.instance,
        aliases: List<String> = emptyList(), description: String = "No default action or description provided.",
        usage: String = "No default action or usage provided."
    ) : this(
        Commands.getCommandOrThis(
            CommandBuilder(name, plugin).aliases(aliases).description(description).usage(usage).command
        )
    )

    /** The default action of command handler, which has the lowest priority.
     *  You can override it by define a new function `alternate(params..)`
     *  with annotation `@Action(Handlers.DEFAULT)`. */
    @Action(Handlers.DEFAULT)
    @Priority(Int.MIN_VALUE)
    open fun defaultAction() = "Pong!"

    /** The default sender denied handler of command handler, which has the lowest priority.
     *  You can override it by define a new function `defaultSDH(params..)`
     *  with annotation `@SenderDeniedHandler(Handlers.DEFAULT)`. */
    @SenderDeniedHandler(Handlers.DEFAULT)
    @Priority(Int.MIN_VALUE)
    open fun defaultSenderDeniedHandler(senderType: Array<CommandSenderType>) =
        "§cOnly ${senderType.contentToString()} can use the command."

    /** The default permission denied handler of command handler, which has the lowest priority.
     *  You can override it by define a new function `defaultPDH(params..)`
     *  with annotation `@PermissionDeniedHandler(Handlers.DEFAULT)`. */
    @SenderDeniedHandler(Handlers.DEFAULT)
    @Priority(Int.MIN_VALUE)
    open fun defaultPermissionDeniedHandler(perm: Array<String>) =
        command.permissionMessage ?: "Permission required: ${perm.contentToString()}"

    /**
     * Register the remote to [CommandManager].
     *
     * @return Whether the remote was registered successfully.
     */
    fun register() = CommandManager.register(this).isNotEmpty()

}