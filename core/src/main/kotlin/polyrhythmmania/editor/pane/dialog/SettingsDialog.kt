package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.PaintboxGame
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.editor.CameraPanningSetting
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.max


class SettingsDialog(editorPane: EditorPane) : EditorDialog(editorPane) {

    val settings: Settings = editorPane.main.settings

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.settings.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bindWidthToSelfHeight()
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptClose()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })

        val scrollPane: ScrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0f))
            this.vBar.unitIncrement.set(64f)
            this.vBar.blockIncrement.set(100f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPane.addChild(scrollPane)

        val pane = Pane()
        val vbox = VBox().apply {
            this.spacing.set(8f)
            this.margin.set(Insets(0f, 0f, 0f, 4f))
        }
        pane += vbox
        val blockHeight = 64f

        fun createCheckbox(text: String, tooltip: String?, bindingVar: Var<Boolean>): CheckBox {
            return CheckBox(binding = { Localization.getVar(text).use() }, font = editorPane.palette.musicDialogFont).apply {
                this.textLabel.textColor.set(Color.WHITE)
                this.imageNode.tint.set(Color.WHITE)
                this.bounds.height.set(blockHeight)
                if (tooltip != null) {
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar(tooltip)))
                }
                this.imageNode.padding.set(Insets(8f))
                this.checkedState.set(bindingVar.getOrCompute())
                this.checkedState.addListener {
                    bindingVar.set(it.getOrCompute())
                }
            }
        }

        fun createGenericPane(
                text: String, tooltip: String?, child: UIElement,
                font: PaintboxFont = editorPane.palette.musicDialogFont,
                percentageContent: Float = 0.5f,
        ): Pane {
            return Pane().apply {
                this.bounds.height.set(blockHeight)
                addChild(TextLabel(binding = { Localization.getVar(text).use() }, font = font).apply {
                    Anchor.TopLeft.configure(this)
                    this.textColor.set(Color.WHITE)
                    this.renderAlign.set(Align.left)
                    this.bindWidthToParent(multiplier = 1f - percentageContent)
                    if (tooltip != null) {
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar(tooltip)))
                    }
                })
                addChild(child.apply {
                    Anchor.TopRight.configure(this)
                    this.bindWidthToParent(multiplier = percentageContent)
                })
            }
        }

        fun <T> createCycleOption(text: String, tooltip: String?, bindingVar: Var<T>,
                                  items: List<T>,
                                  font: PaintboxFont = editorPane.palette.musicDialogFont,
                                  percentageContent: Float = 0.5f,
                                  itemToStringBinding: (Var.Context.(item: T) -> String)? = null): Pair<Pane, CycleControl<T>> {
            val cycle = CycleControl(items, bindingVar, itemToStringBinding)
            return createGenericPane(text, tooltip, cycle, font, percentageContent) to cycle
        }

        fun createSlider(text: String, tooltip: String?, slider: Slider,
                         font: PaintboxFont = editorPane.palette.musicDialogFont,
                         percentageContent: Float = 0.5f): Pair<Pane, Slider> {
            return createGenericPane(text, tooltip, slider, font, percentageContent) to slider
        }

        vbox.temporarilyDisableLayouts {
            vbox += createCheckbox("editorSettings.detailedMarkerUndo", "editorSettings.detailedMarkerUndo.tooltip", settings.editorDetailedMarkerUndo)
            vbox += createCheckbox("editorSettings.cameraPanOnDragEdge", "editorSettings.cameraPanOnDragEdge.tooltip", settings.editorCameraPanOnDragEdge)
            vbox += createCheckbox("editorSettings.higherAccuracyPreview", "editorSettings.higherAccuracyPreview.tooltip", settings.editorHigherAccuracyPreview)
            vbox += createCycleOption("editorSettings.cameraPanningSetting", "editorSettings.cameraPanningSetting.tooltip",
                    settings.editorPanningDuringPlayback, CameraPanningSetting.VALUES,
                    itemToStringBinding = { Localization.getVar(it.localization).use() }).first
            vbox += createCycleOption("editorSettings.autosaveInterval", "editorSettings.autosaveInterval.tooltip",
                    settings.editorAutosaveInterval, Editor.AUTOSAVE_INTERVALS,
                    itemToStringBinding = { item ->
                        Localization.getVar("editorSettings.autosaveInterval.minutes", Var {
                            listOf(item)
                        }).use()
                    }).first
            vbox += createGenericPane("editorSettings.musicWaveformOpacity", "editorSettings.musicWaveformOpacity.tooltip",
                    Pane().also { pane ->
                        val slider = Slider().also { slider ->
                            Anchor.CentreLeft.configure(slider)
                            slider.bindWidthToParent(adjust = -100f)
                            slider.bounds.height.set(32f)
                            slider.minimum.set(0f)
                            slider.maximum.set(10f)
                            slider.tickUnit.set(1f)
                            slider.setValue(settings.editorMusicWaveformOpacity.getOrCompute().toFloat())
                            slider.value.addListener {
                                settings.editorMusicWaveformOpacity.set(it.getOrCompute().toInt())
                            }
                        }
                        pane += slider
                        pane += TextLabel(binding = { "${settings.editorMusicWaveformOpacity.use() * 10}%" },
                                font = editorPane.palette.musicDialogFont).apply {
                            Anchor.TopRight.configure(this)
                            this.bounds.width.set(100f)
                            this.padding.set(Insets(4f))
                            this.textColor.set(Color.WHITE)
                            this.renderAlign.set(Align.center)
                        }
                    }, percentageContent = 0.5f)
        }
        vbox.sizeHeightToChildren(300f)

        pane.bounds.height.bind {
            max(vbox.bounds.height.useF(), pane.parent.use()?.bounds?.height?.useF() ?: 300f)
        }
        scrollPane.setContent(pane)

//        val hbox = HBox().apply {
//            Anchor.BottomCentre.configure(this)
//            this.align.set(HBox.Align.CENTRE)
//            this.spacing.set(16f)
//            this.bounds.width.set(700f)
//        }
//        bottomPane.addChild(hbox)
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        settings.persist()
        Paintbox.LOGGER.info("Settings persisted (editor)")
    }

    private inner class CycleControl<T>(val list: List<T>, bindingVar: Var<T>,
                                        itemToStringBinding: (Var.Context.(item: T) -> String)? = null)
        : Pane(), HasPressedState by HasPressedState.DefaultImpl() {

        val left: Button
        val right: Button
        val label: TextLabel

        val currentItem: Var<T> = bindingVar
        val itemToString: ReadOnlyVar<String> = if (itemToStringBinding != null) {
            Var.bind { itemToStringBinding.invoke(this, currentItem.use()) }
        } else {
            Var.bind {
                currentItem.use().toString()
            }
        }

        init {
            this.margin.set(Insets(8f))
            left = Button("").apply {
                Anchor.CentreLeft.configure(this)
                this.bindWidthToSelfHeight()
                this.skinID.set(StandardMenu.BUTTON_LONG_SKIN_ID)
                addChild(ImageNode(TextureRegion(PaintboxGame.paintboxSpritesheet.upArrow)).apply {
                    this.rotation.set(90f)
                    this.padding.set(Insets(10f))
                    this.tint.set(Color.WHITE)
                })
                this.setOnAction {
                    val index = list.indexOf(currentItem.getOrCompute())
                    val nextIndex = (index - 1 + list.size) % list.size
                    currentItem.set(list[nextIndex])
                }
            }
            right = Button("").apply {
                Anchor.CentreRight.configure(this)
                this.bindWidthToSelfHeight()
                this.skinID.set(StandardMenu.BUTTON_LONG_SKIN_ID)
                addChild(ImageNode(TextureRegion(PaintboxGame.paintboxSpritesheet.upArrow)).apply {
                    this.rotation.set(270f)
                    this.padding.set(Insets(10f))
                    this.tint.set(Color.WHITE)
                })
                this.setOnAction {
                    val index = list.indexOf(currentItem.getOrCompute())
                    val nextIndex = (index + 1) % list.size
                    currentItem.set(list[nextIndex])
                }
            }
            label = TextLabel(binding = { itemToString.use() }, font = editorPane.palette.musicDialogFont).apply {
                Anchor.Centre.configure(this)
                this.bindWidthToParent { -(bounds.height.useF() * 2) }
                this.textColor.set(Color.WHITE)
                this.textAlign.set(TextAlign.CENTRE)
                this.renderAlign.set(Align.center)
                this.markup.set(editorPane.palette.markupInstantiatorDesc)
            }

            addChild(left)
            addChild(right)
            addChild(label)

            @Suppress("LeakingThis")
            HasPressedState.DefaultImpl.addDefaultPressedStateInputListener(this)
        }
    }
}