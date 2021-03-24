package io.github.chrislo27.paintbox.util.gdxutils

import com.badlogic.gdx.files.FileHandle


fun FileHandle.copyHandle(): FileHandle =
        FileHandle(this.file())
