package com.project.audioguide.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.project.audioguide.R
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale

// Synthesize to a file so MediaPlayer can pause and resume at the exact audio position.
internal class SightNarration(
    private val context: Context,
    private val onPlaybackChanged: () -> Unit,
    private val onError: (Int) -> Unit
) {
    private class Recording(val text: String) {
        var offset = 0
        var nextOffset = 0
    }

    private val main = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val audioFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(audioAttributes)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change -> if (change < 0) pause() }, main)
        .build()

    private var engine: TextToSpeech? = null
    private var engineReady = false
    private var closed = false
    private var recording: Recording? = null
    private var utteranceSequence = 0L
    private var pendingFile: File? = null
    private var player: MediaPlayer? = null
    private var playerReady = false
    private var hasAudioFocus = false

    var isPlaying = false
        private set

    val canResume: Boolean
        get() = recording != null

    fun play(text: String?) {
        if (isPlaying) return
        if (text != null && text != recording?.text) {
            clearRecording()
            recording = Recording(text)
        }
        if (recording == null) return

        isPlaying = true
        onPlaybackChanged()
        when {
            playerReady -> startPlayer()
            player != null || pendingFile != null -> Unit
            engineReady -> synthesize()
            engine == null -> initializeEngine()
        }
    }

    fun pause() {
        if (!isPlaying) return
        isPlaying = false
        // A prepared player has started only after we acquired audio focus.
        if (playerReady && hasAudioFocus) player?.pause()
        abandonAudioFocus()
        onPlaybackChanged()
    }

    fun close() {
        closed = true
        clearRecording()
        engine?.shutdown()
        engine = null
    }

    private fun initializeEngine() {
        engine = TextToSpeech(context) { result -> main.post { onEngineInitialized(result) } }
    }

    private fun onEngineInitialized(result: Int) {
        if (closed) return
        val tts = engine ?: return
        if (result != TextToSpeech.SUCCESS) {
            tts.shutdown()
            engine = null
            fail(R.string.guide_audio_error)
            return
        }

        val russian = Locale.forLanguageTag("ru-RU")
        if (tts.setLanguage(russian) < TextToSpeech.LANG_AVAILABLE) {
            tts.shutdown()
            engine = null
            fail(R.string.guide_audio_language_unavailable)
            return
        }

        val offlineVoice = tts.voices
            ?.asSequence()
            ?.filter {
                it.locale.language == russian.language &&
                    !it.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
            }
            ?.maxWithOrNull(compareBy<Voice>({ it.locale.country == russian.country }, { it.quality }))
        if (offlineVoice != null) tts.voice = offlineVoice

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                main.post {
                    if (utteranceId == utteranceSequence.toString()) {
                        pendingFile?.let(::preparePlayer)
                    }
                }
            }

            @Deprecated("Required by UtteranceProgressListener")
            override fun onError(utteranceId: String) {
                onError(utteranceId, TextToSpeech.ERROR)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                main.post {
                    if (utteranceId == utteranceSequence.toString() && pendingFile != null) {
                        Log.w(TAG, "Speech synthesis failed: $errorCode")
                        val message = when {
                            errorCode == TextToSpeech.ERROR_NOT_INSTALLED_YET ->
                                R.string.guide_audio_language_unavailable
                            tts.voice?.isNetworkConnectionRequired == true ->
                                R.string.guide_audio_network_error
                            else -> R.string.guide_audio_error
                        }
                        fail(message)
                    }
                }
            }
        })
        engineReady = true
        if (isPlaying) synthesize()
    }

    private fun synthesize() {
        val current = recording ?: return
        val text = current.text
        val start = current.offset
        var end = minOf(text.length, start + TextToSpeech.getMaxSpeechInputLength())
        if (end < text.length) {
            val space = text.lastIndexOf(' ', end - 1)
            if (space > start) {
                end = space
            } else if (Character.isHighSurrogate(text[end - 1]) && Character.isLowSurrogate(text[end])) {
                end--
            }
        }
        current.nextOffset = end
        while (current.nextOffset < text.length && text[current.nextOffset].isWhitespace()) {
            current.nextOffset++
        }

        try {
            val file = File.createTempFile("narration_", ".wav", context.cacheDir)
            pendingFile = file
            val utteranceId = (++utteranceSequence).toString()
            if (engine?.synthesizeToFile(text.substring(start, end), null, file, utteranceId) != TextToSpeech.SUCCESS) {
                fail(R.string.guide_audio_error)
            }
        } catch (error: IOException) {
            Log.w(TAG, "Could not create speech file", error)
            fail(R.string.guide_audio_error)
        }
    }

    private fun preparePlayer(file: File) {
        pendingFile = null
        try {
            val mediaPlayer = MediaPlayer().also { player = it }
            mediaPlayer.setAudioAttributes(audioAttributes)
            FileInputStream(file).use { mediaPlayer.setDataSource(it.fd) }
            mediaPlayer.setOnPreparedListener {
                if (player === it) {
                    playerReady = true
                    if (isPlaying) startPlayer()
                }
            }
            mediaPlayer.setOnCompletionListener {
                if (player === it) finishSegment()
            }
            mediaPlayer.setOnErrorListener { failedPlayer, what, extra ->
                if (player === failedPlayer) {
                    Log.w(TAG, "Speech playback failed: $what, $extra")
                    fail(R.string.guide_audio_error)
                }
                true
            }
            mediaPlayer.prepareAsync()
        } catch (error: IOException) {
            Log.w(TAG, "Could not open speech file", error)
            fail(R.string.guide_audio_error)
        } finally {
            file.delete()
        }
    }

    private fun startPlayer() {
        if (!hasAudioFocus) {
            hasAudioFocus = audioManager.requestAudioFocus(audioFocus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        if (hasAudioFocus) {
            player?.start()
        } else {
            pause()
            onError(R.string.guide_audio_error)
        }
    }

    private fun finishSegment() {
        releasePlayer()
        val current = recording ?: return
        current.offset = current.nextOffset
        if (current.offset == current.text.length) {
            clearRecording()
            onPlaybackChanged()
        } else if (isPlaying) {
            synthesize()
        }
    }

    private fun fail(messageId: Int) {
        clearRecording()
        onPlaybackChanged()
        onError(messageId)
    }

    private fun clearRecording() {
        isPlaying = false
        recording = null
        ++utteranceSequence
        if (pendingFile != null) {
            engine?.stop()
            pendingFile?.delete()
            pendingFile = null
        }
        releasePlayer()
        abandonAudioFocus()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        playerReady = false
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioManager.abandonAudioFocusRequest(audioFocus)
            hasAudioFocus = false
        }
    }

    private companion object {
        const val TAG = "SightNarration"
    }
}
