package com.rabbitown.yalib

import com.rabbitown.yalib.common.Logger
import com.rabbitown.yalib.module.locale.I18NPlugin
import com.rabbitown.yalib.module.locale.ILocale
import com.rabbitown.yalib.module.locale.LocaleManager
import com.rabbitown.yalib.module.locale.impl.PrefixedLocale
import com.rabbitown.yalib.util.FileUtil
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin

/**
 * The main class.
 *
 * @author Hanbings, Yoooooory
 */
class YaLib : JavaPlugin(), I18NPlugin {

    init {
        instance = this
    }

    override fun onLoad() {
        LocaleManager.load()
        YaLibCentral.registerPlugin(this)
    }

    override fun onEnable() {
        saveDefaultConfig()
        FileUtil.saveResource(this, "data/languageData.yml")
        Logger.info("I'm in!")
    }

    override fun onDisable() {
        Logger.info("I'm out!")
    }

    override fun getNewLocale() = PrefixedLocale.newDefault(this)

    companion object {

        @JvmStatic
        lateinit var instance: YaLib
            private set

    }

}