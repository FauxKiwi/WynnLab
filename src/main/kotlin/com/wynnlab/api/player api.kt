@file:JvmName("PlayerAPI")

package com.wynnlab.api

import com.wynnlab.PREFIX
import com.wynnlab.WynnClass
import com.wynnlab.events.SpellCastEvent
import com.wynnlab.items.WynnItem
import com.wynnlab.localization.Language
import com.wynnlab.plugin
import com.wynnlab.random
import com.wynnlab.util.RefreshRunnable
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Effect
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player

fun Player.sendWynnMessage(key: String, vararg format_args: Any?) =
    sendMessage(PREFIX + getLocalizedText(key, *format_args))

fun Player.getLocalizedText(key: String, vararg format_args: Any?) =
    Language[locale.toLowerCase()].getMessage(key, *format_args)

fun Player.sendWynnMessageNonNls(message: String) {
    sendMessage(PREFIX + message)
}

fun Player.setWynnClass(wynnClass: String) {
    data.setString("class", wynnClass)
    updatePrefix()
}

fun Player.getWynnClass() = data.getString("class")

fun Player.hasWeaponInHand(): Boolean? {
    return getWynnClass()?.let { it == (inventory.itemInMainHand.getClassReq() ?: return null) }
}

fun Player.checkWeapon() =
    hasWeaponInHand()?.also { if (!it) sendMessage("§cYou cannot use this weapon!") } == true

val Player.attackSpeed get() = if (hasWeaponInHand() == true) inventory.itemInMainHand.getAttackSpeed() else null

fun Player.cooldown(): Boolean {
    val attackSpeed = attackSpeed ?: return false
    return if ("cooldown" !in scoreboardTags) {
        addScoreboardTag("cooldown")
        Bukkit.getScheduler().runTaskLater(plugin, Runnable { removeScoreboardTag("cooldown") }, attackSpeed.cooldown.toLong())
        setCooldown(inventory.itemInMainHand.type, attackSpeed.cooldown)
        false
    } else
        true
}

var Player.isCloneClass
get() = "clone" in scoreboardTags
set(value) { if (value) addScoreboardTag("clone") else removeScoreboardTag("clone") }

val Player.invertedControls get() = getWynnClass()?.let { WynnClass[it] }?.invertedControls ?: false

fun Player.castSpell(id: Int) {
    Bukkit.getPluginManager().callEvent(SpellCastEvent(this, id))
}

fun Player.addLeftClick(invertedControls: Boolean = false) {
    if (this.invertedControls && !invertedControls) {
        addRightClick(true)
        return
    }
    when {
        "rrx" in scoreboardTags -> {
            scoreboardTags.remove("rrx")
            updateActionBar(if (invertedControls) "§a§nL§r-§a§nL§r-§a§nR" else "§a§nR§r-§a§nR§r-§a§nL")
            castSpell(4)
        }
        "rlx" in scoreboardTags -> {
            scoreboardTags.remove("rlx")
            updateActionBar(if (invertedControls) "§a§nL§r-§a§nR§r-§a§nR" else "§a§nR§r-§a§nL§r-§a§nL")
            castSpell(3)
        }
        "rxx" in scoreboardTags -> {
            scoreboardTags.remove("rxx")
            scoreboardTags.add("rlx")
            updateActionBar(if (invertedControls) "§a§nL§r-§a§nR§r-§n?" else "§a§nR§r-§a§nL§r-§n?")
        }
        else -> {
            castSpell(0)
            return
        }
    }
    scheduleCancelSpellClicks()
    playEffect(location, Effect.CLICK1, null)
}

fun Player.addRightClick(invertedControls: Boolean = false) {
    if (this.invertedControls && !invertedControls) {
        addLeftClick(true)
        return
    }
    when {
        "rrx" in scoreboardTags -> {
            scoreboardTags.remove("rrx")
            updateActionBar(if (invertedControls) "§a§nL§r-§a§nL§r-§a§nL" else "§a§nR§r-§a§nR§r-§a§nR")
            castSpell(2)
        }
        "rlx" in scoreboardTags -> {
            scoreboardTags.remove("rlx")
            updateActionBar(if (invertedControls) "§a§nL§r-§a§nR§r-§a§nL" else "§a§nR§r-§a§nL§r-§a§nR")
            castSpell(1)
        }
        "rxx" in scoreboardTags -> {
            scoreboardTags.remove("rxx")
            scoreboardTags.add("rrx")
            updateActionBar(if (invertedControls) "§a§nL§r-§a§nL§r-§n?" else "§a§nR§r-§a§nR§r-§n?")
        }
        else -> {
            scoreboardTags.add("rxx")
            updateActionBar(if (invertedControls) "§a§nL§r-§n?§r-§n?" else "§a§nR§r-§n?§r-§n?")
        }
    }
    scheduleCancelSpellClicks()
    playEffect(location, Effect.CLICK1, null)
}

private fun Player.scheduleCancelSpellClicks() {
    RefreshRunnable(data, "cancel_spell") { cancelSpellClicks() }.schedule(20L)
}

fun Player.cancelSpellClicks() {
    if (removeScoreboardTag("rxx")) return
    if (removeScoreboardTag("rrx")) return
    removeScoreboardTag("rlx")
}

fun Player.updateActionBar(msg: String) {
    sendWynnActionBar(msg)
    if ("action_bar" !in scoreboardTags)
        addScoreboardTag("action_bar")
    RefreshRunnable(data, "action_bar") {
        removeScoreboardTag("action_bar")
        standardActionBar()
    }.schedule(20L)
}

fun Player.standardActionBar() {
    if ("action_bar" !in scoreboardTags) {
        sendWynnActionBar("")
    }
}

private fun Player.sendWynnActionBar(msg: String) {
    val health = "§4[§c❤ ${health.toInt()}/${getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value?.toInt()}§4]§r"
    val mana = "§3[§b✺ $foodLevel/20§3]§r"
    val mlD2 = (ChatColor.stripColor(msg)!!.length) / 2
    sendActionBar(buildString {
        append(health)
        repeat(20 - health.length + 8 - mlD2) { append(' ') }
        append(msg)
        append("§r")
        repeat(20 - mana.length + 8 - mlD2) { append(' ') }
        append(mana)
    })
}

var Player.prefix: String
get() = prefixes[this] ?: ""
set(value) {
    prefixes[this] = value
    updatePrefix()
}

val prefixes = hashMapOf<Player, String>()

fun Player.wynnPrefix(): String {
    val classId = getWynnClass() ?: return "§r"
    val classPrefix = if (isCloneClass) Language.en_us.getMessage("classes.$classId.cloneName")
        else Language.en_us.getMessage("classes.$classId.className")
    return "§7[106/${classPrefix.substring(0..1)}${data.getString("guild_tag")?.let { "/$it" } ?: ""}] §r"
}

fun Player.updatePrefix() {
    setDisplayName(wynnPrefix()+prefix+name)
}

val Player.wynnEquipment get() = inventory.let { inv -> arrayOf(
    if (hasWeaponInHand() == true) inv.itemInMainHand else null,
    inv.helmet?.takeIfType(WynnItem.Type.HELMET),
    inv.chestplate?.takeIfType(WynnItem.Type.CHESTPLATE),
    inv.leggings?.takeIfType(WynnItem.Type.LEGGINGS),
    inv.boots?.takeIfType(WynnItem.Type.BOOTS),
    inv.getItem(9)?.takeIfType(WynnItem.Type.RING),
    inv.getItem(10)?.takeIfType(WynnItem.Type.RING),
    inv.getItem(11)?.takeIfType(WynnItem.Type.BRACELET),
    inv.getItem(12)?.takeIfType(WynnItem.Type.NECKLACE)
) }

fun Player.getFirstWeaponSlot() = inventory.let {
    for (i in 0..5) {
        if (((it.getItem(i) ?: continue).itemMeta ?: continue).data.getString("class_req") == getWynnClass())
            return i
    }
    return@let -1
}

fun Player.getId(key: String): Int {
    var sum = 0
    for (item in wynnEquipment) {
        if (item == null) continue
        sum += ((item.itemMeta ?: continue).data.getContainer("ids") ?: continue).getInt(key) ?: continue
    }
    return sum
}

fun Player.getSkill(index: Int): Int {
    return 0
}

fun skillPercentage(points: Int): Double = 0.0

// Damage Type: 0 -> Melee Neutral, 1 -> Melee Elemental, 2 -> Spell Neutral, 3 -> Spell Elemental
val standardConversion = doubleArrayOf(1.0, .0, .0, .0, .0, .0)
private val noDamage = doubleArrayOf(.0, .0, .0, .0, .0, .0)
fun Player.getDamage(melee: Boolean, multiplier: Double = 1.0, conversion: DoubleArray = standardConversion): DoubleArray {
    val result = noDamage

    // Melee Neutral = (Base Dam) * (1 + (IDs) - (Def)) + (Raw Melee)
    // Melee Elemental = (Base Dam) * (1 + (IDs)) - ((Ele Def) * (1 + (Ele Def %)))
    // Spell Neutral = (Base Dam) * (1 + (IDs) - (Def)) * (Att Speed) * (Spell Base Multiplier) + (Raw Spell) * (Spell Base Multiplier)
    // Spell Elemental = [(Base Dam) * (1 + (IDs)) - ((Ele Def) * (1 + (Ele Def %)))] * (Att Speed) * (Spell Base Multiplier)

    val strength = skillPercentage(getSkill(0))
    val dexterity = if (random.nextDouble() < skillPercentage(getSkill(1))) 1.0 else .0

    val damageRanges = if (hasWeaponInHand() ?: return noDamage)
        inventory.itemInMainHand.itemMeta.data.getIntArray("damage") ?: return noDamage
    else return noDamage

    val damages = DoubleArray(6) { i ->
        (damageRanges[i * 2 + 1] - damageRanges[i * 2].let { if (it > 0) random.nextInt(it) else it } + damageRanges[i * 2]).toDouble()
    }

    repeat(6) { i ->
        damages[i] = if (i > 0) damages[0] * conversion[i] + damages[i] else damages[0] * conversion[0]
    }

    val ids = DoubleArray(6) { i ->
        var value = strength + dexterity
        value += if (melee)
            getId("spell_damage") / 100.0
        else
            getId("damage_bonus") / 100.0
        if (i > 0)
            value += getId("bonus__damage") / 100.0
        value
    }

    if (melee) {
        result[0] = damages[0] * (1 + ids[0] /*- def*/) + getId("damage_bonus_raw")
        repeat(5) { i ->
            result[i + 1] = damages[i + 1] * (1 + ids[i + 1]) /*- def*/
        }
    } else {
        val attackSpeedSpellMultiplier = attackSpeed!!.spellMultiplier
        result[0] = (damages[0] * (1 + ids[0] /*- def*/) * attackSpeedSpellMultiplier + getId("spell_damage_raw")) * multiplier
        repeat(5) { i ->
            result[i + 1] = (damages[i + 1] * (1 + ids[i + 1]) /*- def*/) * attackSpeedSpellMultiplier * multiplier
        }
    }

    return result
}

fun Player.getArmorHealth(): Int {
    var sum = 0
    for (item in wynnEquipment) {
        if (item == null) continue
        sum += (item.itemMeta ?: continue).data.getInt("health") ?: continue
    }
    return sum
}