package com.wynnlab.api

import org.bukkit.entity.Player

fun Player.setWynnClass(wynnClass: String) {
    data.setString("class", wynnClass)
}