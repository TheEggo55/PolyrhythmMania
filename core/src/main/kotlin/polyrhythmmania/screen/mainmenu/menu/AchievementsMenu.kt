package polyrhythmmania.screen.mainmenu.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.IntVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.TextRun
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.ComboBox
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.DecimalFormats
import paintbox.util.gdxutils.grey
import polyrhythmmania.Localization
import polyrhythmmania.PRMania
import polyrhythmmania.achievements.*
import polyrhythmmania.achievements.ui.Toast
import polyrhythmmania.ui.PRManiaSkins
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class AchievementsMenu(menuCol: MenuCollection) : StandardMenu(menuCol) {
    
    companion object {
        private val SHOW_IDS_WHEN_DEBUG: Boolean = PRMania.isDevVersion
        private val SHOW_ICONS_WHEN_DEBUG: Boolean = PRMania.isDevVersion
    }
    
    private sealed class ViewType {
        object AllByCategory : ViewType()
        object AllByName : ViewType()
        class Category(val category: AchievementCategory) : ViewType()
    }

    private val totalProgressLabel: TextLabel
    private val panePerCategory: Map<AchievementCategory, UIElement>
    
    init {
        this.setSize(MMMenu.WIDTH_LARGE)
        this.titleText.bind { Localization.getVar("mainMenu.achievements.title").use() }
        this.contentPane.bounds.height.set(520f)
        this.showLogo.set(false)

        val scrollPane = ScrollPane().apply {
            Anchor.TopLeft.configure(this)
            this.bindHeightToParent(-40f)

            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))

            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)

            val scrollBarSkinID = PRManiaSkins.SCROLLBAR_SKIN
            this.vBar.skinID.set(scrollBarSkinID)
            this.hBar.skinID.set(scrollBarSkinID)
            
            // Give vbar a min length
            this.minThumbSize.set(50f)
            this.vBar.unitIncrement.set(40f)
            this.vBar.blockIncrement.set(90f)
        }
        val lowerBox = Pane().apply {
            Anchor.BottomLeft.configure(this)
            this.bounds.height.set(40f)
        }
        val hbox = HBox().apply {
            this.spacing.set(8f)
            this.padding.set(Insets(2f))
            this.bindWidthToParent(adjust = -48f)
        }
        contentPane.addChild(scrollPane)
        contentPane.addChild(lowerBox)
        lowerBox.addChild(hbox)

        val vbox = VBox().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(300f)
            this.spacing.set(4f)
            this.margin.set(Insets(0f, 0f, 0f, 16f))
        }
        
        val percentFormat = DecimalFormats["#0.#"]
        val headingMarkup = Markup(mapOf(), TextRun(main.fontMainMenuHeading, ""), Markup.FontStyles.ALL_USING_DEFAULT_FONT)
        val descMarkup = Markup(mapOf(
                Markup.FONT_NAME_ITALIC to main.fontMainMenuItalic,
                Markup.FONT_NAME_BOLDITALIC to main.fontMainMenuItalic,
                "prmania_icons" to main.fontIcons,
                "rodin" to main.fontMainMenuRodin,
                "thin" to main.fontMainMenuThin
        ), TextRun(font, ""), Markup.FontStyles.ALL_USING_BOLD_ITALIC)
        val statProgressColor = "9FD677"
        val completedTextureReg = TextureRegion(AssetRegistry.get<Texture>("achievements_completed_mark"))
        panePerCategory = linkedMapOf()
        
        totalProgressLabel = TextLabel(binding = {
            AchievementsL10N.getVar("achievement.totalProgress", Var {
                val map = Achievements.fulfillmentMap.use()
                val numGotten = map.size
                val numTotal = Achievements.achievementIDMap.size
                val percentageWhole = (100f * numGotten / numTotal).coerceIn(0f, 100f)
                listOf(percentFormat.format(percentageWhole), numGotten, numTotal)
            }).use()
        }, font = main.fontMainMenuHeading).apply {
            this.textColor.set(CreditsMenu.HEADING_TEXT_COLOR)
            this.bounds.height.set(48f)
            this.padding.set(Insets(8f))
            this.setScaleXY(0.75f)
            this.renderAlign.set(Align.center)
        }
        
        fun createAchievementElement(achievement: Achievement): UIElement {
            val achievementEarned = BooleanVar { Achievements.fulfillmentMap.use()[achievement] != null }
            val entire = ActionablePane().apply {
                this.setOnAltAction {
                    if (Paintbox.debugMode.get() && PRMania.isDevVersion) {
                        main.achievementsUIOverlay.enqueueToast(Toast(achievement, Achievements.fulfillmentMap.getOrCompute()[achievement] ?: Fulfillment(Instant.now())))
                    }
                }

                this += ImageIcon(null, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                    Anchor.TopLeft.configure(this)
                    this.bindWidthToSelfHeight()
                    this.textureRegion.bind {
                        val iconID = if (achievementEarned.use() || (SHOW_ICONS_WHEN_DEBUG && Paintbox.debugMode.use())) achievement.getIconID() else "locked"
                        TextureRegion(AssetRegistry.get<PackedSheet>("achievements_icon")[iconID])
                    }
                }
                this += ImageIcon(completedTextureReg, renderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO).apply {
                    Anchor.TopRight.configure(this)
                    this.bindWidthToSelfHeight()
                    this.visible.bind { achievementEarned.use() }
                    this.tooltipElement.set(createTooltip(AchievementsL10N.getVar("achievement.unlockedTooltip", Var {
                        listOf(ZonedDateTime.ofInstant(Achievements.fulfillmentMap.use()[achievement]?.gotAt ?: Instant.EPOCH, ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME))
                    })))
                }
                this += Pane().apply {
                    Anchor.TopCentre.configure(this)
                    this.bindWidthToParent(adjustBinding = {
                        -(((parent.use()?.contentZone?.height?.use() ?: 0f) + 8f) * 2)
                    })

                    val statProgress: ReadOnlyVar<String> = if (achievement is Achievement.StatTriggered && achievement.showProgress) {
                        val formatter = achievement.stat.formatter
                        val formattedValue = formatter.format(achievement.stat.value)
                        val formattedThreshold = formatter.format(IntVar(achievement.threshold))
                        AchievementsL10N.getVar("achievement.statProgress", Var {
                            listOf(formattedValue.use(), formattedThreshold.use())
                        })
                    } else Var("")
                    this += TextLabel(binding = {
                        val stillHidden = achievement.isHidden && !achievementEarned.use()
                        val desc = if (stillHidden) {
                            "[i]${AchievementsL10N.getVar("achievement.hidden.desc").use()}[]"
                        } else "${if (achievement.isHidden) "${AchievementsL10N.getVar("achievement.hidden.desc").use()} " else ""}${achievement.getLocalizedDesc().use()}"
                        val statProgressText = if (!stillHidden) statProgress.use() else ""
                        "[color=#${achievement.rank.color.toString()} scale=1.0 lineheight=0.75]${achievement.getLocalizedName().use()} [color=#$statProgressColor scale=0.75] ${statProgressText}[] ${if (SHOW_IDS_WHEN_DEBUG && Paintbox.debugMode.use()) "[i color=GRAY scale=0.75]${achievement.id}[]" else ""}\n[][color=LIGHT_GRAY scale=0.75 lineheight=0.9]${desc}[]"
                    }).apply {
                        Anchor.TopLeft.configure(this)
                        this.setScaleXY(1f)
                        this.renderAlign.set(Align.left)
                        this.padding.set(Insets.ZERO)
                        this.markup.set(descMarkup)
                        this.doLineWrapping.set(true)
                    }
                }
            }
            return RectElement(Color.DARK_GRAY).apply {
                this.bounds.height.set(72f)
                this.padding.set(Insets(6f))
                this += entire
            }
        }

        val achievementPanes: Map<Achievement, UIElement> by lazy { Achievements.achievementIDMap.values.associateWith(::createAchievementElement) }

        AchievementCategory.VALUES.forEach { category ->
            val entireVBox = VBox().apply {
                spacing.set(vbox.spacing.get())
            }
            val achievementsInCategory = Achievements.achievementIDMap.values.filter { it.category == category }

            entireVBox += TextLabel(binding = {
                AchievementsL10N.getVar("achievement.categoryProgress", Var {
                    val map = Achievements.fulfillmentMap.use()
                    val numGotten = map.keys.count { it.category == category }
                    val numTotal = achievementsInCategory.size
                    val percentageWhole = (100f * numGotten / numTotal).coerceIn(0f, 100f)
                    listOf(AchievementsL10N.getVar(category.toLocalizationID()).use(), percentFormat.format(percentageWhole), numGotten, numTotal)
                }).use()
            }).apply {
                this.textColor.set(Color().grey(0.35f))
                this.bounds.height.set(56f)
                this.padding.set(Insets(16f, 8f, 0f, 32f))
                this.renderAlign.set(Align.bottomLeft)
                this.setScaleXY(0.75f)
                this.markup.set(headingMarkup)
            }

            achievementsInCategory.forEach { achievement ->
                entireVBox += createAchievementElement(achievement) // Intentionally not using cached ones due to scene graph layout
            }

            entireVBox.sizeHeightToChildren(10f)
            panePerCategory[category] = entireVBox
        }
        
        val viewingCategory = Var<ViewType>(ViewType.AllByCategory)
        
        fun updateCategory() {
            vbox.children.forEach(vbox::removeChild)
            vbox.temporarilyDisableLayouts {
                vbox += totalProgressLabel

                when (val v = viewingCategory.getOrCompute()) {
                    ViewType.AllByCategory -> {
                        panePerCategory.values.forEach { 
                            vbox += it
                        }
                    }
                    ViewType.AllByName -> {
                        achievementPanes.entries.sortedBy { it.key.getLocalizedName().getOrCompute().lowercase(Locale.ROOT) }
                            .forEach { 
                                vbox += it.value
                            }
                    }
                    is ViewType.Category -> {
                        vbox += panePerCategory.getValue(v.category)
                    }
                }
            }
            
            vbox.sizeHeightToChildren(100f)
            scrollPane.setContent(vbox)
        }
        
        updateCategory()
        viewingCategory.addListener {
            updateCategory()
        }

        hbox.temporarilyDisableLayouts {
            hbox += createSmallButton(binding = { Localization.getVar("common.back").use() }).apply {
                this.bounds.width.set(100f)
                this.setOnAction {
                    menuCol.popLastMenu()
                }
            }
            
            hbox += ImageIcon(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["help"])).apply {
                Anchor.BottomRight.configure(this)
                this.padding.set(Insets(2f))
                this.bounds.width.set(36f)
                this.tint.set(Color.BLACK)
                this.tooltipElement.set(createTooltip(Localization.getVar("mainMenu.achievements.help.tooltip")))
            }

            val viewTypeList = listOf(ViewType.AllByCategory, ViewType.AllByName) + AchievementCategory.VALUES.map(ViewType::Category)
            hbox += ComboBox<ViewType>(viewTypeList, viewingCategory.getOrCompute(), font = font).apply {
                this.bounds.width.set(250f)
                this.setScaleXY(0.85f)
                this.itemStringConverter.bind {
                    StringConverter { view ->
                        when (view) {
                            ViewType.AllByCategory -> AchievementsL10N.getVar("achievement.viewAll.category").use()
                            ViewType.AllByName -> AchievementsL10N.getVar("achievement.viewAll.byName").use()
                            is ViewType.Category -> AchievementsL10N.getVar(view.category.toLocalizationID()).use()
                        }
                    }
                }
                this.onItemSelected = { newItem ->
                    viewingCategory.set(newItem)
                }
            }
        }
    }

}