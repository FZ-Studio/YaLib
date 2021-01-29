package com.rabbitown.yalib.module.locale

import org.bukkit.plugin.Plugin

/**
 * Represents a i18n plugin.
 *
 * @author Yoooooory
 */
interface I18NPlugin : Plugin {
    val locale: Locale
}