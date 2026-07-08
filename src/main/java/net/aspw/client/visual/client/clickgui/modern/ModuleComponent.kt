package net.aspw.client.visual.client.clickgui.modern



import net.aspw.client.features.module.Module

import net.aspw.client.value.*

import net.aspw.client.visual.font.smooth.FontLoaders

import org.lwjgl.input.Keyboard

import java.awt.Color



class ModuleComponent(val module: Module) {



    var x = 0f

    var y = 0f

    var width = 0f



    private var hoverProgress = 0f

    private var toggleProgress = 0f

    private var expandProgress = 0f

    private var animDelta = 0f



    var expanded = false

    var currentHeight = 36f



    private var keybinding = false

    private var keybindProgress = 0f



    private val accent1 = ModernClickGui.accent1

    private val accent2 = ModernClickGui.accent2

    private val textSecondary = ModernClickGui.textSecondary



    private val values = module.values.filter { it.canDisplay() }

    private val valueRows = values.map { ValueRow(this, it) }



    init {

        currentHeight = 36f

    }



    fun render(mx: Int, my: Int, delta: Float) {

        animDelta = delta

        val hovered = ModernUtils.isHovered(mx, my, x, y, width, 36f)

        val settingsHovered = expanded && ModernUtils.isHovered(mx, my, x, y + 36f, width, settingsHeight())

        val anyHover = hovered || settingsHovered



        hoverProgress = ModernUtils.animate(hoverProgress, if (anyHover) 1f else 0f, 8f, delta)

        toggleProgress = ModernUtils.animate(toggleProgress, if (module.state) 1f else 0f, 8f, delta)



        val targetExpand = if (expanded) 1f else 0f

        expandProgress = ModernUtils.animate(expandProgress, targetExpand, 6f, delta)

        keybindProgress = ModernUtils.animate(keybindProgress, if (keybinding) 1f else 0f, 10f, delta)



        val cardH = 36f

        val settingsH = settingsHeight() * expandProgress

        val totalH = cardH + settingsH + 4f

        currentHeight = totalH



        val glowAlpha = maxOf(hoverProgress * 0.5f, toggleProgress * 0.3f)

        if (glowAlpha > 0.01f) {

            val glowColor = ModernUtils.lerpColor(accent1, accent2, toggleProgress)

            val gc = Color(glowColor.red, glowColor.green, glowColor.blue, (glowAlpha * 255).toInt())

            ModernUtils.drawGlow(x, y, width, totalH, 8f, gc)

        }



        val bgColor = ModernUtils.lerpColor(ModernClickGui.cardColor, ModernClickGui.hoverColor, hoverProgress)

        ModernUtils.drawRoundedRect(x, y, width, cardH, 8f, bgColor)



        if (module.state) {

            ModernUtils.drawGradientRoundedRect(x, y, 3f, cardH, 1.5f, accent1, accent2)

        }



        FontLoaders.SF18.drawString(

            module.spacedName,

            x + 12f, y + (cardH - FontLoaders.SF18.getHeight()) / 2f,

            if (module.state) accent1.rgb else Color(190, 195, 205).rgb

        )



        if (keybinding) {

            val kbY = y + (cardH - 14f) / 2f

            ModernUtils.drawRoundedRect(x + width - 88f, kbY, 80f, 14f, 4f, Color(59, 130, 246, 80))

            FontLoaders.SF15.drawString("Bind...", x + width - 84f, kbY + 2f, Color.WHITE.rgb)

        } else {

            val keyName = Keyboard.getKeyName(module.keyBind)

            if (keyName.isNotEmpty()) {

                val kbY = y + (cardH - 14f) / 2f

                val kbHovered = ModernUtils.isHovered(mx, my, x + width - 88f, kbY, 80f, 14f)

                ModernUtils.drawRoundedRect(

                    x + width - 88f, kbY, 80f, 14f, 4f,

                    if (kbHovered) Color(59, 130, 246, 60) else Color(42, 46, 56, 200)

                )

                FontLoaders.SF15.drawString(

                    keyName, x + width - 84f, kbY + 2f,

                    if (kbHovered) Color.WHITE.rgb else textSecondary.rgb

                )

            }



            drawToggleSwitch(mx, my)

        }



        if (values.isNotEmpty()) {

            val expandX = x + width - 14f

            val expandY = y + cardH / 2f - 3f

            val hasExpand = ModernUtils.isHovered(mx, my, expandX - 6f, expandY - 4f, 14f, 14f)

            FontLoaders.SF16.drawString(

                if (expanded) "▼" else "▶", expandX, expandY,

                if (hasExpand) Color.WHITE.rgb else textSecondary.rgb

            )

        }



        if (expandProgress > 0.005f) {

            ModernUtils.drawRoundedRect(x, y + cardH + 2f, width, settingsH, 8f, Color(24, 27, 36, 200))

            ModernUtils.scissorStart(x, y + cardH + 2f, width, settingsH)

            var vY = y + cardH + 8f

            for (row in valueRows) {

                row.render(mx, my, vY, expandProgress)

                vY += 24f

            }

            ModernUtils.scissorEnd()

        }

    }



    private fun drawToggleSwitch(mx: Int, my: Int) {

        val toggleX = x + width - 46f

        val toggleY = y + (36f - 14f) / 2f

        val toggleW = 28f

        val toggleH = 14f



        val knobPos = toggleProgress * (toggleW - 12f)

        val bgColor = ModernUtils.lerpColor(

            Color(50, 54, 66), Color(59, 130, 246), toggleProgress

        )



        ModernUtils.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2f, bgColor)

        ModernUtils.drawCircle(toggleX + 6f + knobPos, toggleY + toggleH / 2f, 4f, Color.WHITE)



        if (toggleProgress > 0.1f && toggleProgress < 1f) {

            ModernUtils.drawGlow(

                toggleX, toggleY, toggleW, toggleH, toggleH / 2f,

                Color(59, 130, 246, (toggleProgress * 50).toInt())

            )

        }

    }



    private fun settingsHeight(): Float {

        return (valueRows.size * 24f).coerceAtLeast(0f)

    }



    fun mouseClicked(mx: Int, my: Int, button: Int, yOff: Float) {

        val cardH = 36f

        val settingsH = settingsHeight() * expandProgress

        val totalH = cardH + settingsH + 4f



        if (mx < x || mx > x + width || my < yOff || my > yOff + totalH) return



        val localY = my - yOff



        if (keybinding) {

            if (localY < 36f) {

                if (button == 0 && !ModernUtils.isHovered(

                        mx, my,

                        x + width - 88f, yOff + (36f - 14f) / 2f, 80f, 14f

                    )

                ) {

                    keybinding = false

                    return

                }

            }

            return

        }



        if (localY < 36f) {

            if (button == 0) {

                if (ModernUtils.isHovered(

                        mx, my,

                        x + width - 88f, yOff + (36f - 14f) / 2f, 80f, 14f

                    ) && Keyboard.getKeyName(module.keyBind).isNotEmpty()

                ) {

                    keybinding = true

                    return

                }

                if (ModernUtils.isHovered(mx, my, x + 12f, yOff, width - 100f, 36f)) {

                    module.toggle()

                    return

                }

            }

            if (button == 2) {

                keybinding = true

                return

            }

            if (button == 1) {

                if (values.isNotEmpty()) {

                    expanded = !expanded

                }

                return

            }

        }



        if (localY > 36f && expandProgress > 0.5f) {

            val vIndex = ((localY - 40f) / 24f).toInt().coerceIn(0, valueRows.size - 1)

            if (vIndex in valueRows.indices) {

                val vY = yOff + 40f + vIndex * 24f

                valueRows[vIndex].mouseClicked(mx, my, button, vY)

            }

        }

    }



    fun mouseReleased(mx: Int, my: Int, button: Int, yOff: Float) {

        val localY = my - yOff

        if (localY > 36f && expandProgress > 0.5f) {

            val vIndex = ((localY - 40f) / 24f).toInt()

            if (vIndex in valueRows.indices) {

                valueRows[vIndex].mouseReleased(mx, my, button)

            }

        }

    }



    fun keyTyped(typedChar: Char, keyCode: Int) {

        if (keybinding) {

            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {

                module.keyBind = 0

            } else if (keyCode != Keyboard.KEY_NONE) {

                module.keyBind = keyCode

            }

            keybinding = false

        }

        for (row in valueRows) {

            row.keyTyped(typedChar, keyCode)

        }

    }



    class ValueRow(private val comp: ModuleComponent, private val value: Value<*>) {



        fun render(mx: Int, my: Int, y: Float, alpha: Float) {

            val rowAlpha = alpha.coerceIn(0f, 1f)



            when (value) {

                is BoolValue -> renderBool(mx, my, y, value, rowAlpha)

                is FloatValue -> renderFloat(mx, my, y, value, rowAlpha)

                is IntegerValue -> renderInt(mx, my, y, value, rowAlpha)

                is ListValue -> renderList(mx, my, y, value, rowAlpha)

                else -> renderText(mx, my, y, value, rowAlpha)

            }

        }



        private fun renderBool(mx: Int, my: Int, y: Float, v: BoolValue, alpha: Float) {

            FontLoaders.SF16.drawString(

                v.name, comp.x + 12f, y + 3f,

                Color(210, 215, 225, (alpha * 255).toInt()).rgb

            )



            val toggleX = comp.x + comp.width - 48f

            val toggleY = y + 2f

            val valProgress = if (v.get()) 1f else 0f

            val knobPos = valProgress * 16f

            val bgColor = ModernUtils.lerpColor(

                Color(50, 54, 66), Color(59, 130, 246), valProgress

            )



            ModernUtils.drawRoundedRect(toggleX, toggleY, 24f, 12f, 6f, bgColor)

            ModernUtils.drawCircle(toggleX + 6f + knobPos, toggleY + 6f, 3.5f, Color.WHITE)

        }



        private fun renderFloat(mx: Int, my: Int, y: Float, v: FloatValue, alpha: Float) {

            val labelColor = Color(210, 215, 225, (alpha * 255).toInt())

            FontLoaders.SF16.drawString(v.name, comp.x + 12f, y + 1f, labelColor.rgb)



            val sliderX = comp.x + comp.width - 110f

            val sliderY = y + 9f

            val sliderW = 100f

            val sliderH = 4f

            val fill = ((v.get() - v.minimum) / (v.maximum - v.minimum)).coerceIn(0f, 1f)



            ModernUtils.drawRoundedRect(sliderX, sliderY, sliderW, sliderH, 2f, Color(42, 46, 56, 255))



            if (fill > 0f) {

                ModernUtils.drawGradientRoundedRect(

                    sliderX, sliderY, sliderW * fill, sliderH, 2f,

                    Color(59, 130, 246, 230),

                    Color(139, 92, 246, 230)

                )

            }



            val knobX = sliderX + sliderW * fill

            val knobHovered = ModernUtils.isHovered(

                mx, my, sliderX - 3f, sliderY - 3f, sliderW + 6f, sliderH + 6f

            )

            ModernUtils.drawCircle(knobX, sliderY + sliderH / 2f, if (knobHovered) 5f else 4f, Color.WHITE)



            val display = String.format("%.1f", v.get())

            FontLoaders.SF15.drawString(

                display, comp.x + comp.width - 98f, y + 11f,

                Color(160, 165, 180, (alpha * 255).toInt()).rgb

            )

        }



        private fun renderInt(mx: Int, my: Int, y: Float, v: IntegerValue, alpha: Float) {

            val labelColor = Color(210, 215, 225, (alpha * 255).toInt())

            FontLoaders.SF16.drawString(v.name, comp.x + 12f, y + 1f, labelColor.rgb)



            val sliderX = comp.x + comp.width - 110f

            val sliderY = y + 9f

            val sliderW = 100f

            val sliderH = 4f

            val fill = ((v.get() - v.minimum).toFloat() / (v.maximum - v.minimum).toFloat()).coerceIn(0f, 1f)



            ModernUtils.drawRoundedRect(sliderX, sliderY, sliderW, sliderH, 2f, Color(42, 46, 56, 255))



            if (fill > 0f) {

                ModernUtils.drawGradientRoundedRect(

                    sliderX, sliderY, sliderW * fill, sliderH, 2f,

                    Color(59, 130, 246, 230),

                    Color(139, 92, 246, 230)

                )

            }



            val knobX = sliderX + sliderW * fill

            val knobHovered = ModernUtils.isHovered(

                mx, my, sliderX - 3f, sliderY - 3f, sliderW + 6f, sliderH + 6f

            )

            ModernUtils.drawCircle(knobX, sliderY + sliderH / 2f, if (knobHovered) 5f else 4f, Color.WHITE)



            FontLoaders.SF15.drawString(

                v.get().toString(), comp.x + comp.width - 98f, y + 11f,

                Color(160, 165, 180, (alpha * 255).toInt()).rgb

            )

        }



        private fun renderList(mx: Int, my: Int, y: Float, v: ListValue, alpha: Float) {

            FontLoaders.SF16.drawString(

                v.name, comp.x + 12f, y + 3f,

                Color(210, 215, 225, (alpha * 255).toInt()).rgb

            )



            val valueX = comp.x + comp.width - 100f

            val valueW = 90f

            val valueH = 14f

            val valueHovered = ModernUtils.isHovered(mx, my, valueX, y + 1f, valueW, valueH)



            ModernUtils.drawRoundedRect(

                valueX, y + 1f, valueW, valueH, 4f,

                if (valueHovered) Color(59, 130, 246, 50) else Color(42, 46, 56, 200)

            )



            val display = v.get()

            val textW = FontLoaders.SF15.getStringWidth(display)

            FontLoaders.SF15.drawString(

                display,

                valueX + valueW / 2f - textW / 2f,

                y + 3f,

                if (valueHovered) Color.WHITE.rgb else Color(180, 185, 200, (alpha * 255).toInt()).rgb

            )

        }



        private fun renderText(mx: Int, my: Int, y: Float, v: Value<*>, alpha: Float) {

            FontLoaders.SF16.drawString(

                "${v.name}: §7${v.get()}",

                comp.x + 12f, y + 3f,

                Color(210, 215, 225, (alpha * 255).toInt()).rgb

            )

        }



        fun mouseClicked(mx: Int, my: Int, button: Int, vY: Float) {

            when (value) {

                is BoolValue -> {

                    val toggleX = comp.x + comp.width - 48f

                    val toggleY = vY + 2f

                    if (ModernUtils.isHovered(mx, my, toggleX, toggleY, 24f, 12f)) {

                        value.changeValue(!value.get())

                    }

                }

                is ListValue -> {

                    val valueX = comp.x + comp.width - 100f

                    val valueY = vY + 1f

                    if (ModernUtils.isHovered(mx, my, valueX, valueY, 90f, 14f)) {

                        value.nextValue()

                    }

                }

                is FloatValue -> {

                    val sliderX = comp.x + comp.width - 110f

                    val sliderY = vY + 9f

                    val sliderW = 100f

                    if (ModernUtils.isHovered(mx, my, sliderX - 3f, sliderY - 5f, sliderW + 6f, 14f)) {

                        val fill = ((mx - sliderX) / sliderW).coerceIn(0f, 1f)

                        val newVal = value.minimum + (value.maximum - value.minimum) * fill

                        value.changeValue(newVal)

                    }

                }

                is IntegerValue -> {

                    val sliderX = comp.x + comp.width - 110f

                    val sliderY = vY + 9f

                    val sliderW = 100f

                    if (ModernUtils.isHovered(mx, my, sliderX - 3f, sliderY - 5f, sliderW + 6f, 14f)) {

                        val fill = ((mx - sliderX) / sliderW).coerceIn(0f, 1f)

                        val newVal = (value.minimum + (value.maximum - value.minimum) * fill).toInt()

                        value.changeValue(newVal)

                    }

                }

            }

        }



        fun mouseReleased(mx: Int, my: Int, button: Int) {}



        fun keyTyped(typedChar: Char, keyCode: Int) {}

    }

}

