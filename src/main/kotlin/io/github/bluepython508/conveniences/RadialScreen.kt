package io.github.bluepython508.conveniences

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.options.KeyBinding
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import org.lwjgl.opengl.GL11
import kotlin.math.*

@Environment(EnvType.CLIENT)
abstract class RadialScreen(private val key: KeyBinding, private val renderHandSlot: Boolean = true, menuTitle: Text) :
    Screen(menuTitle) {
    val active: Boolean
        get() = MinecraftClient.getInstance().currentScreen === this

    private val noScreen: Boolean
        get() = MinecraftClient.getInstance().currentScreen == null

    fun activate() {
        if (noScreen) {
            MinecraftClient.getInstance().openScreen(this)
        }
    }

    private fun deactivate() {
        if (active) {
            trigger(mouseX, mouseY)
            MinecraftClient.getInstance().openScreen(null)
        }
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (key.matchesKey(keyCode, scanCode)) {
            deactivate()
            return true
        }
        return false
    }

    override fun isPauseScreen(): Boolean = false
    private var mouseX: Double = 0.0
    private var mouseY: Double = 0.0
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        this.mouseX = mouseX
        this.mouseY = mouseY
    }

    private fun trigger(mouseX: Double, mouseY: Double) {
        if (!active) return
        val mouseSlot = getMouseSlot(mouseX, mouseY)
        if (mouseSlot == -1) return
        onTrigger(mouseSlot)
        return
    }

    abstract fun onTrigger(slot: Int)

    private fun getMouseAngle(mouseX: Double, mouseY: Double): Double {
        val mouseXrel = mouseX - (width / 2)
        val mouseYrel = mouseY - (height / 2)
        return 360 - ((atan2(mouseXrel, mouseYrel).toDegrees() + 360 - 90) % 360)
    }

    private fun getMouseSlot(mouseX: Double, mouseY: Double, mouseDistanceThreshold: Int = 60): Int {
        val mouseXrel = mouseX - (width / 2)
        val mouseYrel = mouseY - (height / 2)
        val mouseDistance = hypot(mouseXrel, mouseYrel)
        if (mouseDistance < mouseDistanceThreshold) return -1
        return (ceil((getMouseAngle(mouseX, mouseY) / 360) * slotsRenderNumber).toInt() - 1).takeUnless { it >= slots } ?: -1
    }

    abstract fun getStackInSlot(slot: Int): ItemStack
    abstract val slots: Int

    open fun getColor(slot: Int, selected: Boolean, enabled: Boolean): ColourAlpha {
        if (!enabled) return UNUSED_COLOR
        if (selected) return SELECTED_COLOR
        return UNSELECTED_COLOR
    }

    private val slotsRenderNumber: Int
        get() = maxOf(slots, 3)

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
        for (slot in 0 until slotsRenderNumber) {
            trapezoidAt(
                matrices,
                slot,
                getMouseSlot(mouseX.toDouble(), mouseY.toDouble()) == slot,
                getStackInSlot(slot)
            )
        }
        if (renderHandSlot) itemRenderer.renderInGui(
            if (hasShiftDown()) MinecraftClient.getInstance().player!!.offHandStack else MinecraftClient.getInstance().player!!.mainHandStack,
            mouseX - 16,
            mouseY - 16
        )

        getMouseSlot(mouseX.toDouble(), mouseY.toDouble()).takeUnless { it == -1 }?.let { slot ->
            renderTooltip(matrices, getStackInSlot(slot).takeUnless { it.isEmpty } ?: return@let, mouseX, mouseY)
        }
    }

    private fun trapezoidAt(matrices: MatrixStack?, slot: Int, selected: Boolean, stack: ItemStack) {
        val slotWidth = 360.0 / slotsRenderNumber.toDouble()
        val firstAngle = (slotWidth * slot) % 360
        val secondAngle = (slotWidth * (slot + 1)) % 360
        trapezoidAt(
            matrices,
            firstAngle to secondAngle,
            selected,
            stack = if (slot >= slots) ItemStack.EMPTY else stack,
            slot = slot
        )
    }

    private fun trapezoidAt(
        matrices: MatrixStack?,
        angles: Pair<Double, Double>,
        selected: Boolean,
        stack: ItemStack,
        slot: Int
    ) {
        val internalRadius = 0.2 * min(width, height)
        val depth = 0.25 * min(width, height)

        val color = getColor(slot, selected, slot <= slots)

        matrices!!

        renderTriangle(
            matrices,
            cos(angles.second.toRadians()) * internalRadius + (width / 2) to sin(angles.second.toRadians()) * internalRadius + (height / 2),
            cos(angles.second.toRadians()) * (internalRadius + depth) + (width / 2) to sin(angles.second.toRadians()) * (internalRadius + depth) + (height / 2),
            cos(angles.first.toRadians()) * (internalRadius + depth) + (width / 2) to sin(angles.first.toRadians()) * (internalRadius + depth) + (height / 2),
            color
        )
        renderTriangle(
            matrices,
            cos(angles.second.toRadians()) * internalRadius + (width / 2) to sin(angles.second.toRadians()) * internalRadius + (height / 2),
            cos(angles.first.toRadians()) * (internalRadius + depth) + (width / 2) to sin(angles.first.toRadians()) * (internalRadius + depth) + (height / 2),
            cos(angles.first.toRadians()) * internalRadius + (width / 2) to sin(angles.first.toRadians()) * internalRadius + (height / 2),
            color
        )

        val slotWidth = 360.0 / slotsRenderNumber.toDouble()
        val angleCentre = (angles.first + (slotWidth / 2)) % 360
        itemRenderer.renderInGui(
            stack,
            ((cos(angleCentre.toRadians()) * (internalRadius + (depth / 4))) + (width / 2)).toInt() - 8,
            ((sin(angleCentre.toRadians()) * (internalRadius + (depth / 4))) + (height / 2)).toInt() - 8
        )
    }

    private fun renderTriangle(
        matrices: MatrixStack,
        v: Pair<Double, Double>,
        v2: Pair<Double, Double>,
        v3: Pair<Double, Double>,
        color: ColourAlpha
    ) {
        matrices.push()
        RenderSystem.enableBlend()
        RenderSystem.disableTexture()
        RenderSystem.defaultBlendFunc()

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer

        buffer.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR)
        buffer.vertex(v.first, v.second, zOffset.toDouble()).color(color)
            .next()
        buffer.vertex(v2.first, v2.second, zOffset.toDouble()).color(color)
            .next()
        buffer.vertex(v3.first, v3.second, zOffset.toDouble()).color(color)
            .next()
        tessellator.draw()
        RenderSystem.enableTexture()
        RenderSystem.disableBlend()
        matrices.pop()
    }
}

val SELECTED_COLOR: ColourAlpha = ColourAlpha(0, 0, 0, 100)
val UNSELECTED_COLOR: ColourAlpha = ColourAlpha(0, 0, 0, 200)

val UNUSED_COLOR: ColourAlpha = ColourAlpha(0, 0, 0, 10)