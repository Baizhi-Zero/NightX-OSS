package net.aspw.client.features.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.aspw.client.Launch
import net.aspw.client.config.FileManager
import net.aspw.client.event.Listenable
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.impl.combat.*
import net.aspw.client.features.module.impl.movement.*
import net.aspw.client.features.module.impl.player.*
import net.aspw.client.features.module.impl.other.FastPlace
import net.aspw.client.features.module.impl.targets.AntiBots
import net.aspw.client.features.module.impl.targets.AntiTeams
import net.aspw.client.features.module.impl.exploit.Disabler
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.IntegerValue
import net.aspw.client.value.ListValue
import net.aspw.client.utils.MinecraftInstance
import net.aspw.client.utils.misc.RandomUtils
import java.io.File
import java.io.FileReader
import java.io.PrintWriter

object PresetManager : MinecraftInstance(), Listenable {

    private val presetsDir: File
        get() {
            val dir = File(Launch.fileManager.dir, "presets")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun applyGrimPreset() {

        val forceOff = listOf(
            Speed::class.java,
            Flight::class.java,
            LongJump::class.java,
            Jesus::class.java,
            Disabler::class.java
        )
        for (clazz in forceOff) {
            val mod = Launch.moduleManager.getModule(clazz) ?: continue
            if (mod.state) mod.toggle()
        }

        setModule(NoSlow::class.java) {
            setEnum("Mode", "Vanilla")
            set("BlockForwardMultiplier", 1.0f)
            set("BlockStrafeMultiplier", 1.0f)
            state(true)
        }

        setModule(KillAura::class.java) {
            set("Target-Range", 3.0f)
            set("Attack-Range", 3.0f)
            set("MaxCPS", 9)
            set("MinCPS", 7)
            setEnum("RotationMode", "GrimAC")
            set("MaxTurnSpeed", 180f)
            set("MinTurnSpeed", 180f)
            set("Angle-Tick", 1)
            set("FailRate", 0f)
            set("Fov", 180f)
            setEnum("AutoBlock", "Fake")
            setEnum("Swing", "Packet")
            set("Criticals", false)
            setEnum("Particle", "Vanilla")
            setEnum("TargetMode", "Single")
            setEnum("Priority", "Distance")
            set("WallCheck", false)
            set("NoInvAttack", true)
            set("StopSprint", true)
            state(true)
        }

        setModule(Criticals::class.java) {
            setEnum("Mode", "Packet")
            set("HurtTime", 10)
            set("OnlyAura", true)
            state(true)
        }
        setModule(PacketCriticals::class.java) {
            setEnum("Mode", "Packet")
            set("HurtTime", 10)
            set("OnlyKillAura", true)
        }

        setModule(AntiVelocity::class.java) {
            setEnum("Mode", "GrimReverse")
            set("Horizontal", 0f)
            set("Vertical", 0f)
            set("Chance", 60f)
            state(true)
        }
        setModule(Velocity::class.java) {
            setEnum("Mode", "Grim")
            set("GrimStop", true)
            set("GrimDelay", 2)
            set("OnlyCombat", true)
            state(true)
        }

        setModule(Scaffold::class.java) {
            setEnum("TowerWhen", "Moving")
            setEnum("TowerMode", "GrimAC")
            setEnum("RotationMode", "GrimAC")
            set("PlaceableDelay", true)
            set("MinDelay", 80)
            set("MaxDelay", 120)
            setEnum("SprintMode", "Off")
            set("Timer", 1.0f)
            set("SafeWalk", true)
            setEnum("Place-Condition", "Always")
            state(true)
        }

        setModule(Sprint::class.java) { set("Silent", true); state(true) }
        setModule(AutoClicker::class.java) {
            set("Cooldown-Check", true)
            set("Left", true)
            set("Left-MaxCPS", 11)
            set("Left-MinCPS", 8)
            state(true)
        }
        setModule(NoFall::class.java) { setEnum("Type", "Packet"); state(true) }
        setModule(HitBox::class.java) { set("Size", 0.0f); state(false) }
        setModule(KeepSprint::class.java) { state(true) }
        setModule(Timer::class.java) { set("Speed", 1.0f); state(false) }
        setModule(AutoBlock::class.java) { state(false) }
        setModule(WTap::class.java) {
            setEnum("Mode", "Legit")
            set("Delay", 4)
            state(true)
        }
        setModule(BackTrack::class.java) {
            set("ResetOnVelocity", true)
            set("ResetOnLagging", true)
            state(false)
        }
        setModule(LegitVelocity::class.java) { state(false) }
        setModule(AntiFireBall::class.java) {
            set("MaxTurnSpeed", 180f)
            set("MinTurnSpeed", 180f)
            state(true)
        }

        setModule(AntiBots::class.java) {
            set("Tab", true)
            setEnum("TabMode", "Contains")
            set("LivingTime", true)
            set("LivingTimeTicks", 40)
            set("Ground", true)
            set("Ping", true)
            set("Health", true)
            set("DuplicateInWorld", true)
            state(true)
        }
        setModule(AntiTeams::class.java) {
            set("ScoreboardTeam", true)
            set("Color", true)
            state(true)
        }

        setModule(AutoArmor::class.java) {
            set("MinPercent", 15)
            set("Delay", 200)
            set("OnlyWhenAttacked", true)
            set("SilentSwap", true)
            state(true)
        }
        setModule(FastPlace::class.java) { set("Speed", 3); state(true) }

        setModule(InvMove::class.java) { setEnum("Mode", "Vanilla"); state(true) }
        setModule(Step::class.java) { setEnum("Mode", "Vanilla"); state(false) }
        setModule(AntiVoid::class.java) { setEnum("SetBack-Mode", "FlyFlag"); state(true) }

        setModule(Stealer::class.java) {
            set("MaxDelay", 120)
            set("MinDelay", 80)
            state(true)
        }
        setModule(InvManager::class.java) {
            set("MaxDelay", 120)
            set("MinDelay", 80)
            set("NoCombat", true)
            state(true)
        }
    }

    fun applyHvHPreset() {
        val file = File(presetsDir, "hvh.json")
        if (file.exists()) {
            loadPresetFromFile(file)
        } else {
            applyDefaultHvH()
        }
    }

    fun savePreset(name: String) {
        val file = File(presetsDir, "${sanitizeName(name)}.json")
        val jsonObject = JsonObject()
        for (module in Launch.moduleManager.modules) {
            if (module.values.isEmpty()) continue
            val jsonModule = JsonObject()
            for (value in module.values) {
                jsonModule.add(value.name, value.toJson())
            }
            jsonObject.add(module.name, jsonModule)
        }
        writeJson(file, jsonObject)
    }

    fun saveSpecificPreset(name: String, moduleNames: Set<String>) {
        val file = File(presetsDir, "${sanitizeName(name)}.json")
        val jsonObject = JsonObject()
        for (module in Launch.moduleManager.modules) {
            if (module.name !in moduleNames || module.values.isEmpty()) continue
            val jsonModule = JsonObject()
            for (value in module.values) {
                jsonModule.add(value.name, value.toJson())
            }
            jsonObject.add(module.name, jsonModule)
        }
        writeJson(file, jsonObject)
    }

    fun loadPreset(name: String): Boolean {
        val file = File(presetsDir, "${sanitizeName(name)}.json")
        if (!file.exists()) return false
        return loadPresetFromFile(file)
    }

    fun deletePreset(name: String) {
        val file = File(presetsDir, "${sanitizeName(name)}.json")
        if (file.exists()) file.delete()
    }

    fun listPresets(): List<String> {
        return presetsDir.listFiles()
            ?.filter { it.extension.equals("json", true) }
            ?.map { it.nameWithoutExtension }
            ?.filter { !it.equals("hvh", true) }
            ?: emptyList()
    }

    private fun loadPresetFromFile(file: File): Boolean {
        val jsonElement = try {
            JsonParser().parse(FileReader(file))
        } catch (e: Exception) { return false }

        if (jsonElement == null || jsonElement.isJsonNull) return false
        val jsonObject = jsonElement.asJsonObject

        for (entry in jsonObject.entrySet()) {
            val module = Launch.moduleManager.getModule(entry.key)
            if (module == null) continue
            val jsonModule = entry.value.asJsonObject
            for (value in module.values) {
                val element = jsonModule.get(value.name)
                if (element != null) value.fromJson(element)
            }
        }
        return true
    }

    private fun writeJson(file: File, jsonObject: JsonObject) {
        val writer = PrintWriter(file)
        writer.println(FileManager.PRETTY_GSON.toJson(jsonObject))
        writer.close()
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    }

    private fun applyDefaultHvH() {
        setModule(KillAura::class.java) {
            set("Target-Range", 6.0f)
            set("Attack-Range", 6.0f)
            set("MaxCPS", 20)
            set("MinCPS", 20)
            setEnum("TargetMode", "Multi")
            setEnum("RotationMode", "Standard")
            set("MaxTurnSpeed", 180f)
            set("MinTurnSpeed", 180f)
            setEnum("AutoBlock", "Vanilla")
            setEnum("Swing", "Packet")
            set("Criticals", true)
            setEnum("Particle", "EveryHit")
            setEnum("Priority", "Health")
            set("MultiCombo", true)
            set("Multi-Packet", 10)
            set("FailRate", 0f)
            set("Fov", 180f)
            state(true)
        }

        setModule(Criticals::class.java) {
            setEnum("Mode", "HvHPacket")
            set("HurtTime", 10)
            state(true)
        }

        setModule(AntiVelocity::class.java) {
            setEnum("Mode", "Cancel")
            set("Horizontal", 0f)
            set("Vertical", 0f)
            set("Chance", 100f)
            state(true)
        }

        setModule(Velocity::class.java) {
            setEnum("Mode", "Cancel")
            set("Horizontal", 0f)
            set("Vertical", 0f)
            set("PushReduction", 100f)
            state(true)
        }

        setModule(NoSlow::class.java) {
            setEnum("Mode", "NCP")
            set("BlockForwardMultiplier", 1.0f)
            set("BlockStrafeMultiplier", 1.0f)
            state(true)
        }

        setModule(Sprint::class.java) { set("Silent", true); state(true) }
        setModule(Timer::class.java) { set("Speed", 2.5f); state(true) }
        setModule(HitBox::class.java) { set("Size", 0.6f); state(true) }
        setModule(KeepSprint::class.java) { state(true) }
        setModule(AutoBlock::class.java) { state(true) }
        setModule(NoFall::class.java) { setEnum("Type", "Packet"); state(true) }

        setModule(Scaffold::class.java) {
            set("Timer", 1.3f)
            set("SafeWalk", true)
            setEnum("RotationMode", "Smooth")
            setEnum("SprintMode", "Off")
            state(true)
        }

        setModule(AutoClicker::class.java) {
            set("Left", true)
            set("Left-MaxCPS", 20)
            set("Left-MinCPS", 18)
            state(true)
        }

        setModule(WTap::class.java) {
            setEnum("Mode", "FullPacket")
            set("Delay", 1)
            state(true)
        }

        setModule(AntiFireBall::class.java) {
            set("MaxTurnSpeed", 180f)
            set("MinTurnSpeed", 180f)
            state(true)
        }

        setModule(PacketCriticals::class.java) {
            setEnum("Mode", "HvHPacket")
            state(true)
        }

        setModule(ArmorBreaker::class.java) { set("Packets", 1000); state(true) }

        setModule(KillAuraBlatant::class.java) {
            set("Reach", 6.0f)
            set("APS", 20)
            set("MinAPS", 20)
            setEnum("RotationMode", "Blatant")
            set("TargetHUD", true)
            set("AutoBlock", true)
            state(true)
        }

        setModule(InvMove::class.java) {
            setEnum("Mode", "Vanilla")
            set("NoDetectable", true)
            state(true)
        }

        setModule(FastPlace::class.java) { set("Speed", 0); state(true) }

        setModule(AutoArmor::class.java) {
            set("MinPercent", 0)
            set("Delay", 50)
            set("SilentSwap", true)
            state(true)
        }
    }

    private class ValueSetter(private val module: Module) {
        fun set(name: String, value: Any) {
            for (v in module.values) {
                if (!v.name.equals(name, true)) continue
                when (v) {
                    is BoolValue -> if (value is Boolean) v.set(value)
                    is FloatValue -> if (value is Number) v.set(value.toFloat())
                    is IntegerValue -> if (value is Number) v.set(value.toInt())
                    is ListValue -> if (value is String) v.changeValue(value)
                }
                return
            }
        }

        fun setEnum(name: String, value: String) {
            for (v in module.values) {
                if (!v.name.equals(name, true)) continue
                if (v is ListValue) {
                    v.changeValue(value)
                    return
                }
            }
        }

        fun state(on: Boolean) {
            if (module.state != on) module.toggle()
        }
    }

    private fun setModule(moduleClass: Class<out Module>, block: ValueSetter.() -> Unit) {
        val module = Launch.moduleManager.getModule(moduleClass) ?: return
        ValueSetter(module).block()
    }

    override fun handleEvents(): Boolean = true
}
