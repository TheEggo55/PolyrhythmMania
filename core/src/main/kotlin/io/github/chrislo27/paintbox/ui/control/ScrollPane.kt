package io.github.chrislo27.paintbox.ui.control

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.binding.FloatVar
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.binding.VarChangedListener
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.skin.DefaultSkins
import io.github.chrislo27.paintbox.ui.skin.Skin
import io.github.chrislo27.paintbox.ui.skin.SkinFactory
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import io.github.chrislo27.paintbox.util.gdxutils.isShiftDown


/**
 * A [ScrollPane] has content that is panned around. It also has optional [ScrollBar]s for user interactivity.
 */
open class ScrollPane : Control<ScrollPane>() {
    companion object {
        const val SKIN_ID: String = "ScrollPane"

        init {
            DefaultSkins.register(ScrollPane.SKIN_ID, SkinFactory { element: ScrollPane ->
                ScrollPaneSkin(element)
            })
        }
    }

    enum class ScrollBarPolicy {
        NEVER, ALWAYS, AS_NEEDED
    }

    val hBar: ScrollBar by lazy { createScrollBar(ScrollBar.Orientation.HORIZONTAL) }
    val vBar: ScrollBar by lazy { createScrollBar(ScrollBar.Orientation.VERTICAL) }
    val contentPane: Pane = Pane().apply {
        this.doClipping.set(true)
    }
    private val currentContent: Var<UIElement?> = Var(null)

    val hBarPolicy: Var<ScrollBarPolicy> = Var(ScrollBarPolicy.AS_NEEDED)
    val vBarPolicy: Var<ScrollBarPolicy> = Var(ScrollBarPolicy.AS_NEEDED)
    val barSize: FloatVar = FloatVar(15f)
    val minThumbSize: FloatVar = FloatVar(20f)

    private val contentWHListener: VarChangedListener<Float> = VarChangedListener {
        updateScrollBounds()
    }
    private val barPolicyListener: VarChangedListener<ScrollBarPolicy> = VarChangedListener {
        updateScrollBounds()
    }

    init {
        hBar.bounds.height.bind { barSize.use() }
        hBar.bindWidthToParent { -barSize.use() }
        Anchor.BottomLeft.configure(hBar)
        vBar.bounds.width.bind { barSize.use() }
        vBar.bindHeightToParent { -barSize.use() }
        Anchor.TopRight.configure(vBar)
        Anchor.TopLeft.configure(contentPane)
        contentPane.bindWidthToParent {
            if (vBar.apparentVisibility.use()) (-barSize.use()) else 0f
        }
        contentPane.bindHeightToParent {
            if (hBar.apparentVisibility.use()) (-barSize.use()) else 0f
        }
        contentPane.bounds.width.addListener(contentWHListener)
        contentPane.bounds.height.addListener(contentWHListener)
        barSize.addListener(contentWHListener)
        hBarPolicy.addListener(barPolicyListener)
        vBarPolicy.addListener(barPolicyListener)
        minThumbSize.addListener(contentWHListener)
        currentContent.addListener { updateScrollBounds() }
        contentPane.contentOffsetX.bind { -hBar.value.use() }
        contentPane.contentOffsetY.bind { -vBar.value.use() }

        addChild(contentPane)
        addChild(hBar)
        addChild(vBar)

        updateScrollBounds()

        this.addInputEventListener { event ->
            if (event is Scrolled) {
                val vBarAmount = if (Gdx.input.isShiftDown()) event.amountX else event.amountY
                val hBarAmount = if (Gdx.input.isShiftDown()) event.amountY else event.amountX
                
                if (vBarAmount != 0f && vBar.apparentVisibility.getOrCompute() && !vBar.apparentDisabledState.getOrCompute()) {
                    if (vBarAmount > 0) vBar.incrementBlock() else vBar.decrementBlock()
                }
                if (hBarAmount != 0f && hBar.apparentVisibility.getOrCompute() && !hBar.apparentDisabledState.getOrCompute()) {
                    if (hBarAmount > 0) hBar.incrementBlock() else hBar.decrementBlock()
                }
            }
            false
        }
    }

    protected fun updateScrollBounds() {
        val current = currentContent.getOrCompute()
        var currentW = 0f
        var currentH = 0f

        if (current != null) {
            currentW = current.bounds.width.getOrCompute()
            currentH = current.bounds.height.getOrCompute()
        }

        // Set scroll bars' visibility based on policy and currentContent bounds relative to contentPane
        val contentPaneWidth = contentPane.contentZone.width.getOrCompute()
        val contentPaneHeight = contentPane.contentZone.height.getOrCompute()
        val contentWidthDiff = currentW - contentPaneWidth
        val contentHeightDiff = currentH - contentPaneHeight
        val shouldShowHbar: Boolean = when (hBarPolicy.getOrCompute()) {
            ScrollBarPolicy.NEVER -> false
            ScrollBarPolicy.ALWAYS -> true
            ScrollBarPolicy.AS_NEEDED -> contentWidthDiff > 0f
        }
        val shouldShowVbar: Boolean = when (vBarPolicy.getOrCompute()) {
            ScrollBarPolicy.NEVER -> false
            ScrollBarPolicy.ALWAYS -> true
            ScrollBarPolicy.AS_NEEDED -> contentHeightDiff > 0f
        }

        hBar.visible.set(shouldShowHbar)
        vBar.visible.set(shouldShowVbar)

        hBar.minimum.set(0f)
        hBar.maximum.set(contentWidthDiff.coerceAtLeast(0f))
        hBar.visibleAmount.set(
                ((contentPaneWidth / currentW) * hBar.maximum.getOrCompute())
                        .coerceAtMost(hBar.maximum.getOrCompute())
                        .coerceAtLeast(minThumbSize.getOrCompute())
        )

        vBar.minimum.set(0f)
        vBar.maximum.set(contentHeightDiff.coerceAtLeast(0f))
        vBar.visibleAmount.set(
                ((contentPaneHeight / currentH) * vBar.maximum.getOrCompute())
                        .coerceAtMost(vBar.maximum.getOrCompute())
                        .coerceAtLeast(minThumbSize.getOrCompute())
        )

//        println("hbar: $shouldShowHbar  vbox: $shouldShowVbar  contentWidthDiff: $contentWidthDiff  contentHeightDiff: $contentHeightDiff  currentW: $currentW  currentH: $currentH")
    }

    protected open fun createScrollBar(orientation: ScrollBar.Orientation): ScrollBar {
        return ScrollBar(orientation)
    }

    fun setContent(element: UIElement) {
        val lastCurrent = this.currentContent.getOrCompute()
        if (lastCurrent != null) {
            this.currentContent.set(null)
            lastCurrent.bounds.width.removeListener(contentWHListener)
            lastCurrent.bounds.height.removeListener(contentWHListener)
            contentPane.removeChild(lastCurrent)
        }
        this.currentContent.set(element)
        element.bounds.x.set(0f)
        element.bounds.y.set(0f)
        element.bounds.width.addListener(contentWHListener)
        element.bounds.height.addListener(contentWHListener)
        contentPane.addChild(element)
        updateScrollBounds()
    }

    fun getContent(): UIElement? = currentContent.getOrCompute()

    override fun getDefaultSkinID(): String = ScrollPane.SKIN_ID
}

open class ScrollPaneSkin(element: ScrollPane) : Skin<ScrollPane>(element) {
    val bgColor: Var<Color> = Var(Color(0.94f, 0.94f, 0.94f, 1f))

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val contentBounds = element.contentZone
        val rectX = contentBounds.x.getOrCompute() + originX
        val rectY = originY - contentBounds.y.getOrCompute()
        val rectW = contentBounds.width.getOrCompute()
        val rectH = contentBounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor
        val opacity = element.apparentOpacity.getOrCompute()
        val tmpColor = ColorStack.getAndPush()

        tmpColor.set(bgColor.getOrCompute())
        tmpColor.a *= opacity

        batch.color = tmpColor
        batch.fillRect(rectX, rectY - rectH, rectW, rectH)

        batch.packedColor = lastPackedColor
        ColorStack.pop()
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
    }
}