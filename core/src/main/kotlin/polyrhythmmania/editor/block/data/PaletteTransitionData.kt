package polyrhythmmania.editor.block.data

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.binding.VarChangedListener
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.RenderAlign
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.ComboBox
import paintbox.ui.control.DecimalTextField
import paintbox.ui.control.TextField
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.DecimalFormats
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.world.tileset.PaletteTransition
import polyrhythmmania.world.tileset.TransitionCurve


class PaletteTransitionData(val defaultValue: PaletteTransition = PaletteTransition.DEFAULT) {

    val paletteTransition: Var<PaletteTransition> = Var(defaultValue)


    fun createMenuItems(editor: Editor): List<MenuItem> {
        val markup = editor.editorPane.palette.markup
        
        val curveCombobox = ComboBox(TransitionCurve.VALUES, paletteTransition.getOrCompute().transitionCurve).also { combobox ->
            combobox.markup.set(markup)
            combobox.itemStringConverter.set {
                Localization.getValue(it.localizationNameKey)
            }
            combobox.selectedItem.addListener {
                paletteTransition.set(paletteTransition.getOrCompute().copy(transitionCurve = it.getOrCompute()))
            }
        }
        val curveComboboxPane = HBox().also { hbox ->
            hbox.spacing.set(8f)
            hbox.bounds.height.set(32f)
            hbox += TextLabel(Localization.getValue("blockContextMenu.transitionCurve")).apply {
                this.markup.set(markup)
                this.renderAlign.set(Align.right)
                this.bounds.width.set(160f)
                this.tooltipElement.set(editor.editorPane.createDefaultTooltipParent(
                        VBox().apply { 
                            this.spacing.set(8f)
                            
                            val widthResizeTrigger = FloatVar(0f)
                            widthResizeTrigger.addListener {
                                this.bounds.width.set(it.getOrCompute())
                            }
                            
                            this += TextLabel(Localization.getValue("blockContextMenu.transitionCurve.tooltip")).apply { 
                                this.markup.set(markup)
                                this.textColor.set(Color.WHITE)
                                this.bounds.width.set(100f)
                                this.bounds.height.set(100f)
                                this.bounds.width.addListener { 
                                    widthResizeTrigger.set(it.getOrCompute())
                                }
                                this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.WIDTH_AND_HEIGHT))
                            }
                            this += ImageNode(binding = {
                                AssetRegistry.get<PackedSheet>("ui_icon_editor_curves")[curveCombobox.selectedItem.use().imageID]
                            }).apply { 
                                Anchor.TopCentre.xConfigure(this, 0f)
                                this.margin.set(Insets(10f, 0f, 0f, 0f))
                                this.bounds.width.set(200f)
                                this.bounds.height.set(210f)
                            }
                            this += TextLabel(binding = {
                                Localization.getValue(curveCombobox.selectedItem.use().localizationNameKey)
                            }, editor.editorPane.palette.musicDialogFontBold).apply {
                                this.textColor.set(Color.WHITE)
                                this.renderAlign.set(RenderAlign.center)
                                this.bounds.height.set(32f)
                            }
                            
                            this.autoSizeToChildren.set(true)
                            
                            val resizeParentListener: VarChangedListener<Float> = VarChangedListener { 
                                val p = parent.getOrCompute()
                                p?.sizeWidthToChildren()
                                p?.sizeHeightToChildren()
                            }
                            this.bounds.width.addListener(resizeParentListener)
                            this.bounds.height.addListener(resizeParentListener)

                            this.sizeWidthToChildren()
                            this.sizeHeightToChildren()
                        }
                ))
            }
            hbox += curveCombobox.apply {
                this.bindWidthToParent(adjust = -(160f + 8f))
            }
        }
        
        return listOf(
                LabelMenuItem.create(Localization.getValue("blockContextMenu.paletteChange.transitionDuration"), markup),
                CustomMenuItem(
                        HBox().apply {
                            this.bounds.height.set(32f)
                            this.spacing.set(4f)

                            fun createTextField(): Pair<UIElement, TextField> {
                                val textField = DecimalTextField(startingValue = paletteTransition.getOrCompute().duration, decimalFormat = DecimalFormats["0.0##"],
                                        font = editor.editorPane.palette.musicDialogFont).apply {
                                    this.minimumValue.set(0f)
                                    this.textColor.set(Color(1f, 1f, 1f, 1f))

                                    this.value.addListener {
                                        val dur = it.getOrCompute()
                                        paletteTransition.set(paletteTransition.getOrCompute().copy(duration = dur.coerceAtLeast(0f)))
                                    }
                                }

                                return RectElement(Color(0f, 0f, 0f, 1f)).apply {
                                    this.bindWidthToParent(adjust = -4f, multiplier = 0.333f)
                                    this.border.set(Insets(1f))
                                    this.borderStyle.set(SolidBorder(Color.WHITE))
                                    this.padding.set(Insets(2f))
                                    this += textField
                                } to textField
                            }

                            this += HBox().apply {
                                this.spacing.set(4f)
                                this += createTextField().first
                            }
                        }
                ),

                SeparatorMenuItem(),
                CustomMenuItem(curveComboboxPane),
                
                SeparatorMenuItem(),
                CheckBoxMenuItem.create(BooleanVar(paletteTransition.getOrCompute().pulseMode),
                        Localization.getValue("blockContextMenu.paletteChange.pulseMode"), markup).apply {
                    this.createTooltip = {
                        it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.paletteChange.pulseMode.tooltip")))
                    }
                    this.checkState.addListener {
                        paletteTransition.set(paletteTransition.getOrCompute().copy(pulseMode = it.getOrCompute()))
                    }
                },
                CheckBoxMenuItem.create(BooleanVar(paletteTransition.getOrCompute().reverse),
                        Localization.getValue("blockContextMenu.paletteChange.reverse"), markup).apply {
                    this.createTooltip = {
                        it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.paletteChange.reverse.tooltip")))
                    }
                    this.checkState.addListener {
                        paletteTransition.set(paletteTransition.getOrCompute().copy(reverse = it.getOrCompute()))
                    }
                },
        )
    }
    
    fun writeToJson(rootObj: JsonObject) {
        val pt = paletteTransition.getOrCompute()
        rootObj.add("duration", pt.duration)
        rootObj.add("pulseMode", pt.pulseMode)
        rootObj.add("reverse", pt.reverse)
    }

    fun readFromJson(rootObj: JsonObject) {
        var pt = PaletteTransition.DEFAULT
        
        val durationVal = rootObj.get("duration")
        if (durationVal != null && durationVal.isNumber) {
            pt = pt.copy(duration = durationVal.asFloat().coerceAtLeast(0f))
        }
        val pulseVal = rootObj.get("pulse")
        if (pulseVal != null && pulseVal.isBoolean) {
            pt = pt.copy(pulseMode = pulseVal.asBoolean())
        }
        val reverseVal = rootObj.get("reverse")
        if (reverseVal != null && reverseVal.isBoolean) {
            pt = pt.copy(reverse = reverseVal.asBoolean())
        }
        
        this.paletteTransition.set(pt)
    }

}