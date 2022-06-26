package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import paintbox.util.Vector3Stack
import paintbox.util.gdxutils.drawUV
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputScore
import polyrhythmmania.engine.input.InputTimingRestriction
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import polyrhythmmania.world.tileset.TintedSubregion
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt


open class SimpleRenderedEntity(world: World) : Entity(world) {
    
    protected open fun getRenderVec(): Vector3 {
        return this.position
    }
    
    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(getRenderVec()))
        val packedColor = batch.packedColor
        renderSimple(renderer, batch, tileset, convertedVec)
        Vector3Stack.pop()
        batch.packedColor = packedColor
    }
    
    protected open fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
    }
    
    protected fun drawTintedRegion(batch: SpriteBatch, vec: Vector3, tileset: Tileset, tintedRegion: TintedRegion,
                                   offsetX: Float, offsetY: Float, renderWidth: Float, renderHeight: Float,
                                   tintColor: Color? = null) {
        if (renderWidth == 0f || renderHeight == 0f) return
        
        val tilesetRegion = tileset.getTilesetRegionForTinted(tintedRegion)
        var offX = offsetX
        var offY = offsetY
        var drawWidth = renderWidth
        var drawHeight = renderHeight
        val spacingObj = tilesetRegion.spacing
        
        if (spacingObj.spacing > 0 && spacingObj.normalWidth > 0 && spacingObj.normalHeight > 0) {
            val spacing = spacingObj.spacing
            val totalNormalWidth = spacingObj.normalWidth + spacing * 2
            val totalNormalHeight = spacingObj.normalHeight + spacing * 2
            val totalNormalWidthRatio = totalNormalWidth.toFloat() / spacingObj.normalWidth
            val totalNormalHeightRatio = totalNormalHeight.toFloat() / spacingObj.normalHeight
            
            offX -= spacing.toFloat() / totalNormalWidth
            offY -= spacing.toFloat() / totalNormalHeight
            
            drawWidth *= totalNormalWidthRatio
            drawHeight *= totalNormalHeightRatio
        }
        
        batch.color = tintColor ?: tintedRegion.color.getOrCompute()
        // Compute special UV regions for TintedSubregion
        val texture = tilesetRegion.texture
        var u = tilesetRegion.u
        var v = tilesetRegion.v
        var u2 = tilesetRegion.u2
        var v2 = tilesetRegion.v2
        if (tintedRegion is TintedSubregion) {
            val uSpan = u2 - u
            val vSpan = v2 - v
            u = MathUtils.lerp(u, u2, tintedRegion.u)
            v = MathUtils.lerp(v, v2, tintedRegion.v)
            u2 = u + uSpan * tintedRegion.u2
            v2 = v + vSpan * tintedRegion.v2
        }
        batch.drawUV(texture, vec.x + offX, vec.y + offY, drawWidth, drawHeight, u, v, u2, v2)
        batch.setColor(1f, 1f, 1f, 1f)
    }
    
    protected fun drawTintedRegion(batch: SpriteBatch, vec: Vector3, tileset: Tileset, tintedRegion: TintedRegion,
                                   tintColor: Color? = null) {
        drawTintedRegion(batch, vec, tileset, tintedRegion, 0f, 0f, renderWidth, renderHeight, tintColor)
    }
}

abstract class SpriteEntity(world: World) : SimpleRenderedEntity(world) {
    
    open val numLayers: Int = 1
    var tint: Color? = null
    var tintIsMultiplied: Boolean = true


    protected open val pxOffsetX: Float = 0f
    protected open val pxOffsetY: Float = 0f
    
    abstract fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion?

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val tmpColor = ColorStack.getAndPush()
        val tint = this.tint
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            if (tr != null) {
                if (tintIsMultiplied) {
                    tmpColor.set(tr.color.getOrCompute())
                    if (tint != null) {
                        tmpColor.r *= tint.r
                        tmpColor.g *= tint.g
                        tmpColor.b *= tint.b
                        tmpColor.a *= tint.a
                        // Intentionally don't clamp values.
                    }
                } else {
                    if (tint != null) tmpColor.set(tint)
                }
                drawTintedRegion(batch, vec, tileset, tr, pxOffsetX, pxOffsetY, renderWidth, renderHeight, tmpColor)
            }
        }
        ColorStack.pop()
    }
}

class EntityPlatform(world: World, val withLine: Boolean = false) : SpriteEntity(world) {
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return if (withLine) tileset.platformWithLine else tileset.platform
    }
}

open class EntityCube(world: World, val withLine: Boolean = false, val withBorder: Boolean = false)
    : SpriteEntity(world) {
    
    companion object {
        fun createCubemapIndex(x: Int, y: Int, z: Int): Long {
            val overflow = x !in Short.MIN_VALUE..Short.MAX_VALUE || y !in Short.MIN_VALUE..Short.MAX_VALUE || z !in Short.MIN_VALUE..Short.MAX_VALUE

            return 0L or (if (overflow) (1L shl 63) else 0L).toLong() or (
                    x.toShort().toLong() or (y.toShort().toLong() shl 16) or (z.toShort().toLong() shl 32)
                    )
        }
    }

    override val numLayers: Int = 6

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        // Uncomment if cube map culling is to be used
//        val cubeOccludesX = world.cubeMap[createCubemapIndex(this.position.x.roundToInt() - 1, this.position.y.roundToInt(), this.position.z.roundToInt())] != null
//        val cubeOccludesY = world.cubeMap[createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt() + 1, this.position.z.roundToInt())] != null
//        val cubeOccludesZ = world.cubeMap[createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt(), this.position.z.roundToInt() + 1)] != null
//
//        return when (index) { // Update when with non-culling
//            0 -> tileset.cubeBorder
//            1 -> if (cubeOccludesZ) null else tileset.cubeBorderZ
//            2 -> if (cubeOccludesX) null else tileset.cubeFaceX
//            3 -> if (cubeOccludesY) null else tileset.cubeFaceY
//            4 -> if (cubeOccludesZ) null else tileset.cubeFaceZ
//            5 -> if (withLine) tileset.redLine else null
//            6 -> if (withBorder) tileset.platformBorder else null
//            else -> null
//        }
        return when (index) {
            0 -> if (withBorder) tileset.cubeBorderPlatform else tileset.cubeBorder
            1 -> tileset.cubeBorderZ
            2 -> tileset.cubeFaceX
            3 -> tileset.cubeFaceY
            4 -> tileset.cubeFaceZ
            5 -> if (withLine) tileset.redLine else null
            else -> null
        }
    }
    
    fun getCubemapIndex(): Long {
        return createCubemapIndex(this.position.x.roundToInt(), this.position.y.roundToInt(), this.position.z.roundToInt())
    }
}

class EntityExplosion(
        world: World, val secondsStarted: Float,
        val renderScale: Float, val rodOffsetX: Float, val rodOffsetY: Float
) : SpriteEntity(world), TemporaryEntity {

    companion object {
        const val EXPLOSION_DURATION: Float = 8 / 60f
        
        val STATES: List<State> = listOf(
                State(0, 40f / 32f, 24f / 32f),
                State(1, 32f / 32f, 24f / 32f),
                State(2, 24f / 32f, 16f / 32f),
                State(3, 16f / 32f, 16f / 32f),
        )
    }

    data class State(val index: Int, val renderWidth: Float, val renderHeight: Float)

    private var state: State = STATES[0]
    var duration: Float = EXPLOSION_DURATION

    override val renderWidth: Float
        get() = state.renderWidth * renderScale
    override val renderHeight: Float
        get() = state.renderHeight * renderScale
    
    override val renderSortOffsetX: Float get() = 0f
    override val renderSortOffsetY: Float get() = 0f
    override val renderSortOffsetZ: Float get() = 0f
    
    private var percentageLife: Float = 0f

    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion? {
        return tileset.explosionFrames[state.index]
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (isKilled) return
        val percentage = this.percentageLife
        if (percentage < 1f) {
            val index = (percentage * STATES.size).toInt()
            state = STATES[index]

            val tr = getTintedRegion(tileset, 0)
            if (tr != null) {
                val centreOfExplosionX = 10f / 32f // X-centre of the explosion at normal 1.0 scaling is 10 px right
                val baseOfExplosionY = 3f / 32f // Base of the explosion at normal 1.0 scaling is 3 px up

                drawTintedRegion(batch, vec, tileset, tr, (centreOfExplosionX) * renderScale - (renderWidth / 2f) + rodOffsetX, baseOfExplosionY * renderScale + rodOffsetY, renderWidth, renderHeight)
            }


            // Debug bounds rendering
//            batch.setColor(0.8f, 0.8f, 1f, 0.75f)
//            batch.fillRect(vec.x, vec.y, renderWidth, renderHeight)
//            batch.setColor(0f, 0f, 1f, 0.75f)
//            batch.fillRect(vec.x, vec.y, 0.1f, 0.1f)
//            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        if (isKilled) return
        
        val secondsElapsed = engine.seconds - secondsStarted
        val percentage = (secondsElapsed / duration).coerceIn(0f, 1f)
        this.percentageLife = percentage
        if (percentage >= 1f) {
            kill()
        }
    }
}

class EntitySign(world: World, val type: Type) : SpriteEntity(world) {
    enum class Type {
        A, DPAD, BO, TA, N;
    }

    override val numLayers: Int = 2
    override val renderWidth: Float = 0.5f
    override val renderHeight: Float = 0.5f
    
    override fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion {
        return when (type) {
            Type.A -> if (index == 0) tileset.signAShadow else tileset.signA
            Type.DPAD -> if (index == 0) tileset.signDpadShadow else tileset.signDpad
            Type.BO -> if (index == 0) tileset.signBoShadow else tileset.signBo
            Type.TA -> if (index == 0) tileset.signTaShadow else tileset.signTa
            Type.N -> if (index == 0) tileset.signNShadow else tileset.signN
        }
    }
}

class EntityInputFeedback(world: World, val end: End, baseColor: Color, val inputScore: InputScore, val flashIndex: Int)
    : SimpleRenderedEntity(world) {
    
    companion object {
        val ACE_COLOUR: Color = Color.valueOf("FFF800")
        val GOOD_COLOUR: Color = Color.valueOf("6DE23B")
        val BARELY_COLOUR: Color = Color.valueOf("FF7C26")
        val MISS_COLOUR: Color = Color.valueOf("E82727")
    }
    
    enum class End {
        LEFT, MIDDLE, RIGHT;
    }
    
    private val originalColor: Color = baseColor.cpy()
    private val currentColor: Color = baseColor.cpy()
    
    private fun getBaseColorToUse(engine: Engine): Color {
        val inputter = engine.inputter
        val restriction = inputter.inputChallenge.restriction
        return if (restriction == InputTimingRestriction.ACES_ONLY && this.inputScore != InputScore.ACE) {
            MISS_COLOUR
        } else if (restriction == InputTimingRestriction.NO_BARELY && this.inputScore == InputScore.BARELY) {
            MISS_COLOUR
        } else {
            originalColor
        }
    }

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        super.renderSimple(renderer, batch, tileset, vec)

        val tintedRegion = when (end) {
            End.LEFT -> tileset.inputFeedbackStart
            End.MIDDLE -> tileset.inputFeedbackMiddle
            End.RIGHT -> tileset.inputFeedbackEnd
        }
        val tmpColor = ColorStack.getAndPush().set(tintedRegion.color.getOrCompute()) // tintedRegion's color is likely just white
        tmpColor.mul(this.currentColor)
        drawTintedRegion(batch, vec, tileset, tintedRegion, 0f, 0f, renderWidth, renderHeight, tmpColor)
        ColorStack.pop()
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        
        updateCurrentColor(engine)
    }
    
    fun updateCurrentColor(engine: Engine) {
        val updatedBaseColor = getBaseColorToUse(engine)
        val currentSec = engine.seconds
        val flashSec = engine.inputter.inputFeedbackFlashes[flashIndex]
        val flashTime = 0.25f
        if (currentSec - flashSec < flashTime) {
            val percentage = ((currentSec - flashSec) / flashTime).coerceIn(0f, 1f)
            currentColor.set(updatedBaseColor).lerp(Color.WHITE, 1f - percentage)
        } else {
            currentColor.set(updatedBaseColor)
        }
    }
}

class EntityInputIndicator(world: World, var isDpad: Boolean)
    : SimpleRenderedEntity(world) {

    var visible: Boolean = true

    override val renderWidth: Float = 16f / 32f
    override val renderHeight: Float = 16f / 32f
    
    private var lastBeat: Float = 0f

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        if (!visible || !world.worldSettings.showInputIndicators) return
        val tintedRegion = if (isDpad) tileset.indicatorDpad else tileset.indicatorA
        val renderWidth = this.renderWidth
        val renderHeight = this.renderHeight

        val bumpHeight = 2f / 32f
        val beat = lastBeat
        val bumpTime = 0.28f
        val normalizedBeat = if (beat < 0f) (beat + floor(beat).absoluteValue) else (beat)
        val bumpAmt = (1f - (normalizedBeat % 1f).coerceIn(0f, bumpTime) / bumpTime)//.coerceIn(0f, 1f)

        drawTintedRegion(batch, vec, tileset, tintedRegion, -renderWidth / 2f, (bumpAmt) * bumpHeight - (2f / 32f),
                renderWidth, renderHeight)
    }

    override fun engineUpdate(engine: Engine, beat: Float, seconds: Float) {
        super.engineUpdate(engine, beat, seconds)
        this.lastBeat = beat
    }
}
