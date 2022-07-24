package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class BlockSpotlightAdvanced(engine: Engine) : AbstractBlockSpotlight(engine, BlockSpotlightAdvanced.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }


    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightAdvanced.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(300f)
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spotlightAdvanced"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(LabelMenuItem.create("(Not implemented yet.)", editor.editorPane.palette.markup))
        }
    }

    override fun copy(): BlockSpotlightAdvanced {
        return BlockSpotlightAdvanced(engine).also {
            this.copyBaseInfoTo(it)
            
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        
    }
}