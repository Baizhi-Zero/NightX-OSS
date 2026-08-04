package net.baizhi.client.visual.imgui

import imgui.ImDrawData
import imgui.ImGui
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Fixed-function (OpenGL 2.1) render backend for Dear ImGui, ported from
 * imgui_impl_opengl2.cpp. Uses GL11 client-side arrays so it works on the
 * same GL 2.1 context Minecraft 1.8.9 uses.
 *
 * NOTE: ImDrawData#getCmdListVtxBufferData / #getCmdListIdxBufferData both
 * write into a single shared static ByteBuffer, so each buffer must be
 * copied out immediately after the call (it is overwritten by the next one).
 */
object ImGuiImplGl2 {

    private const val VERTEX_SIZE = 20 // pos(2f) + uv(2f) + col(4B)

    private const val GL_CLIENT_ALL_ATTRIB_BITS = -1 // 0xFFFFFFFF

    private var vtxCopy = ByteBuffer.allocateDirect(0)
    private var idxCopy = ByteBuffer.allocateDirect(0)

    private fun growable(buffer: ByteBuffer, capacity: Int): ByteBuffer =
        if (buffer.capacity() >= capacity) buffer
        else ByteBuffer.allocateDirect(capacity + 16384).order(ByteOrder.nativeOrder())

    fun render() {
        val drawData = ImGui.getDrawData()
        if (!drawData.getValid() || drawData.getCmdListsCount() <= 0) return

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GL11.glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS)

        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0)

        val fbWidth = Display.getWidth()
        val fbHeight = Display.getHeight()

        GL11.glViewport(0, 0, fbWidth, fbHeight)
        GL11.glScissor(0, 0, fbWidth, fbHeight)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        val displayPosX = drawData.getDisplayPosX()
        val displayPosY = drawData.getDisplayPosY()
        val displaySizeX = drawData.getDisplaySizeX()
        val displaySizeY = drawData.getDisplaySizeY()

        // glPushAttrib only preserves the matrix MODE, not the matrix VALUES.
        // Save/restore the projection and modelview stacks explicitly so the
        // orthographic projection does not leak into subsequent game rendering.
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glOrtho(
            displayPosX.toDouble(), (displayPosX + displaySizeX).toDouble(),
            (displayPosY + displaySizeY).toDouble(), displayPosY.toDouble(),
            -1.0, 1.0
        )
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()

        GL11.glEnable(GL11.GL_BLEND)
        GL14.glBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA
        )
        GL14.glBlendEquation(GL14.GL_FUNC_ADD)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_COLOR_MATERIAL)
        GL11.glDisable(GL11.GL_FOG)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY)

        val idxSize = ImDrawData.sizeOfImDrawIdx()
        val idxType = if (idxSize == 2) GL11.GL_UNSIGNED_SHORT else GL11.GL_UNSIGNED_INT

        for (listIndex in 0 until drawData.getCmdListsCount()) {
            val vtxRaw = drawData.getCmdListVtxBufferData(listIndex)
            val vtxBytes = vtxRaw.limit()
            vtxCopy = growable(vtxCopy, vtxBytes)
            vtxCopy.clear()
            vtxCopy.put(vtxRaw)
            vtxCopy.flip()

            val idxRaw = drawData.getCmdListIdxBufferData(listIndex)
            val idxBytes = idxRaw.limit()
            idxCopy = growable(idxCopy, idxBytes)
            idxCopy.clear()
            idxCopy.put(idxRaw)
            idxCopy.flip()

            val cmdCount = drawData.getCmdListCmdBufferSize(listIndex)

            vtxCopy.position(0)
            GL11.glVertexPointer(2, GL11.GL_FLOAT, VERTEX_SIZE, vtxCopy)
            vtxCopy.position(8)
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VERTEX_SIZE, vtxCopy)
            vtxCopy.position(16)
            GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, VERTEX_SIZE, vtxCopy)

            for (cmdIndex in 0 until cmdCount) {
                val clip = ImGuiManager.scratchVec4
                drawData.getCmdListCmdBufferClipRect(clip, listIndex, cmdIndex)
                if (clip.z <= clip.x || clip.w <= clip.y) continue

                val clipX = clip.x - displayPosX
                val clipY = clip.y - displayPosY
                val clipW = clip.z - clip.x
                val clipH = clip.w - clip.y

                GL11.glScissor(
                    clipX.toInt(),
                    (fbHeight - clipY - clipH).toInt(),
                    clipW.toInt(),
                    clipH.toInt()
                )
                GL11.glEnable(GL11.GL_SCISSOR_TEST)

                val texId = drawData.getCmdListCmdBufferTextureId(listIndex, cmdIndex)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId.toInt())

                val elemCount = drawData.getCmdListCmdBufferElemCount(listIndex, cmdIndex)
                val idxOffset = drawData.getCmdListCmdBufferIdxOffset(listIndex, cmdIndex)

                idxCopy.position(idxOffset * idxSize)
                GL11.glDrawElements(GL11.GL_TRIANGLES, elemCount, idxType, idxCopy as ByteBuffer)
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()

        GL11.glPopClientAttrib()
        GL11.glPopAttrib()
    }
}
