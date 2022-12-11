package polyrhythmmania.storymode.music

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import polyrhythmmania.soundsystem.BeadsMusic
import polyrhythmmania.soundsystem.sample.GdxAudioReader
import polyrhythmmania.soundsystem.sample.MusicSample
import kotlin.concurrent.thread

class Stem(val file: FileHandle) {
    
    val sample: MusicSample
    val beadsMusic: BeadsMusic
    
    val enoughMusicLoaded: ReadOnlyBooleanVar = BooleanVar(false)
    val musicFinishedLoading: ReadOnlyBooleanVar = BooleanVar(false)
    
    init {
        val (sample, handler) = GdxAudioReader.newDecodingMusicSample(file,
                object : GdxAudioReader.AudioLoadListener {
                    override fun progress(bytesReadSoFar: Long, bytesReadThisChunk: Int) {
                        if (bytesReadSoFar > 100_000L && !enoughMusicLoaded.get()) {
                            Gdx.app.postRunnable {
                                (enoughMusicLoaded as BooleanVar).set(true)
                            }
                        }
                    }

                    override fun onFinished(totalBytesRead: Long) {
                        Gdx.app.postRunnable {
                            (musicFinishedLoading as BooleanVar).set(true)
                        }
                    }
                })
        thread(start = true, isDaemon = true, name = "StoryMusicHandler Stem music decoder for $file", priority = 7) {
            Paintbox.LOGGER.debug("Starting story music handler stem decode for $file")
            handler.decode()
            Paintbox.LOGGER.debug("Finished story music handler stem decode for $file")
        }
        
        this.sample = sample
        this.beadsMusic = BeadsMusic(sample)
    }
}