package net.baizhi.client.visual.imgui

import imgui.ImGui
import imgui.ImVec4
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar

object NightXTheme {

    fun apply() {
        val style = ImGui.getStyle()

        style.setWindowRounding(12f)
        style.setFrameRounding(8f)
        style.setPopupRounding(8f)
        style.setScrollbarRounding(8f)
        style.setGrabRounding(6f)
        style.setTabRounding(6f)
        style.setChildRounding(0f)
        style.setWindowBorderSize(0f)
        style.setFrameBorderSize(0f)
        style.setPopupBorderSize(1f)
        style.setScrollbarSize(6f)
        style.setGrabMinSize(14f)
        style.setWindowPadding(0f, 0f)
        style.setFramePadding(10f, 7f)
        style.setItemSpacing(10f, 8f)
        style.setItemInnerSpacing(8f, 5f)
        style.setAlpha(1f)

        style.setCurveTessellationTol(1.6f)
        style.setCircleTessellationMaxError(0.12f)

        val colors = style.getColors()

        colors[ImGuiCol.Text] = ImVec4(0.95f, 0.96f, 0.98f, 1f)
        colors[ImGuiCol.TextDisabled] = ImVec4(0.50f, 0.52f, 0.58f, 1f)

        colors[ImGuiCol.WindowBg] = ImVec4(0.06f, 0.06f, 0.10f, 1f)
        colors[ImGuiCol.ChildBg] = ImVec4(0.08f, 0.08f, 0.12f, 1f)
        colors[ImGuiCol.PopupBg] = ImVec4(0.10f, 0.10f, 0.15f, 0.98f)

        colors[ImGuiCol.Border] = ImVec4(0.20f, 0.20f, 0.30f, 0.50f)
        colors[ImGuiCol.BorderShadow] = ImVec4(0f, 0f, 0f, 0f)

        colors[ImGuiCol.FrameBg] = ImVec4(0.12f, 0.12f, 0.18f, 1f)
        colors[ImGuiCol.FrameBgHovered] = ImVec4(0.16f, 0.16f, 0.24f, 1f)
        colors[ImGuiCol.FrameBgActive] = ImVec4(0.20f, 0.20f, 0.30f, 1f)

        colors[ImGuiCol.TitleBg] = ImVec4(0.06f, 0.06f, 0.10f, 1f)
        colors[ImGuiCol.TitleBgActive] = ImVec4(0.08f, 0.08f, 0.12f, 1f)
        colors[ImGuiCol.TitleBgCollapsed] = ImVec4(0.06f, 0.06f, 0.10f, 0.50f)

        colors[ImGuiCol.MenuBarBg] = ImVec4(0.10f, 0.10f, 0.15f, 1f)

        colors[ImGuiCol.ScrollbarBg] = ImVec4(0.06f, 0.06f, 0.10f, 0.60f)
        colors[ImGuiCol.ScrollbarGrab] = ImVec4(0.30f, 0.30f, 0.45f, 0.80f)
        colors[ImGuiCol.ScrollbarGrabHovered] = ImVec4(0.45f, 0.35f, 0.70f, 0.90f)
        colors[ImGuiCol.ScrollbarGrabActive] = ImVec4(0.55f, 0.40f, 0.85f, 1f)

        colors[ImGuiCol.CheckMark] = ImVec4(0.55f, 0.40f, 0.85f, 1f)

        colors[ImGuiCol.SliderGrab] = ImVec4(0.55f, 0.40f, 0.85f, 0.90f)
        colors[ImGuiCol.SliderGrabActive] = ImVec4(0.65f, 0.50f, 0.95f, 1f)

        colors[ImGuiCol.Button] = ImVec4(0.55f, 0.40f, 0.85f, 0.65f)
        colors[ImGuiCol.ButtonHovered] = ImVec4(0.55f, 0.40f, 0.85f, 0.85f)
        colors[ImGuiCol.ButtonActive] = ImVec4(0.65f, 0.50f, 0.95f, 1f)

        colors[ImGuiCol.Header] = ImVec4(0.55f, 0.40f, 0.85f, 0.40f)
        colors[ImGuiCol.HeaderHovered] = ImVec4(0.55f, 0.40f, 0.85f, 0.55f)
        colors[ImGuiCol.HeaderActive] = ImVec4(0.55f, 0.40f, 0.85f, 0.70f)

        colors[ImGuiCol.Separator] = ImVec4(0.20f, 0.20f, 0.30f, 0.50f)
        colors[ImGuiCol.SeparatorHovered] = ImVec4(0.55f, 0.40f, 0.85f, 0.60f)
        colors[ImGuiCol.SeparatorActive] = ImVec4(0.55f, 0.40f, 0.85f, 0.80f)

        colors[ImGuiCol.Tab] = ImVec4(0.10f, 0.10f, 0.15f, 1f)
        colors[ImGuiCol.TabHovered] = ImVec4(0.55f, 0.40f, 0.85f, 0.70f)
        colors[ImGuiCol.TabSelected] = ImVec4(0.55f, 0.40f, 0.85f, 0.50f)

        colors[ImGuiCol.ResizeGrip] = ImVec4(0.55f, 0.40f, 0.85f, 0.20f)
        colors[ImGuiCol.ResizeGripHovered] = ImVec4(0.55f, 0.40f, 0.85f, 0.50f)
        colors[ImGuiCol.ResizeGripActive] = ImVec4(0.55f, 0.40f, 0.85f, 0.80f)

        colors[ImGuiCol.TextSelectedBg] = ImVec4(0.55f, 0.40f, 0.85f, 0.35f)
        colors[ImGuiCol.TextLink] = ImVec4(0.55f, 0.40f, 0.85f, 1f)
        colors[ImGuiCol.ModalWindowDimBg] = ImVec4(0.02f, 0.02f, 0.04f, 0.70f)

        style.setColors(colors)
    }
}
