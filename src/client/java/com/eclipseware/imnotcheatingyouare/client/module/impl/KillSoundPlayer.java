package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class KillSoundPlayer {
    public interface Comdlg32Lib extends StdCallLibrary {
        boolean GetOpenFileNameW(Pointer lpofn);
    }

    public interface WinmmLib extends StdCallLibrary {
        int mciSendStringW(WString command, Pointer returnBuffer, int returnLength, Pointer callback);
    }

    private static final int OFN_PATHMUSTEXIST = 0x00000800;
    private static final int OFN_FILEMUSTEXIST = 0x00001000;
    private static final int OFN_NOCHANGEDIR = 0x00000008;
    private static final int OFN_EXPLORER = 0x00080000;

    private static final boolean IS_WINDOWS = detectWindows();
    private static final Comdlg32Lib COMDLG32 = loadWindowsLib("comdlg32", Comdlg32Lib.class);
    private static final WinmmLib WINMM = loadWindowsLib("winmm", WinmmLib.class);
    private static final AtomicBoolean DIALOG_OPEN = new AtomicBoolean(false);
    private static final AtomicInteger MCI_COUNTER = new AtomicInteger(1);

    private KillSoundPlayer() {
    }

    private static boolean detectWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("win");
    }

    private static <T extends StdCallLibrary> T loadWindowsLib(String libName, Class<T> type) {
        if (!IS_WINDOWS) {
            return null;
        }
        try {
            return Native.load(libName, type, W32APIOptions.UNICODE_OPTIONS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isDialogOpen() {
        return DIALOG_OPEN.get();
    }

    public static void openAudioFileChooserAsync(String initialPath, Consumer<File> onSelected) {
        if (!DIALOG_OPEN.compareAndSet(false, true)) {
            return;
        }
        Thread pickerThread = new Thread(() -> {
            File chosen = null;
            try {
                if (IS_WINDOWS && COMDLG32 != null) {
                    chosen = showWindowsOpenDialog(initialPath);
                }
            } catch (Throwable ignored) {
            } finally {
                DIALOG_OPEN.set(false);
            }
            if (onSelected != null) {
                onSelected.accept(chosen);
            }
        }, "KillSoundFilePicker");
        pickerThread.setDaemon(true);
        pickerThread.start();
    }

    private static File showWindowsOpenDialog(String initialPath) {
        int ptrSize = Native.POINTER_SIZE;
        int structSize = ptrSize == 8 ? 152 : 88;
        Memory ofn = new Memory(structSize);
        ofn.clear();

        int maxFileChars = 1024;
        Memory fileBuf = new Memory((long) maxFileChars * 2L);
        fileBuf.clear();

        if (initialPath != null && !initialPath.isBlank()) {
            File existing = new File(initialPath.trim());
            if (existing.isFile()) {
                writeWideString(fileBuf, existing.getAbsolutePath(), maxFileChars - 1);
            }
        }

        String filterRaw = "Audio Files (*.wav;*.ogg;*.mp3;*.flac;*.m4a)\0*.wav;*.ogg;*.mp3;*.flac;*.m4a\0All Files (*.*)\0*.*\0\0";
        Memory filterBuf = allocWideChars(filterRaw);
        Memory titleBuf = allocWideString("Select Kill Sound Audio File");

        ofn.setInt(0, structSize);
        if (ptrSize == 8) {
            ofn.setPointer(24, filterBuf);
            ofn.setInt(44, 1);
            ofn.setPointer(48, fileBuf);
            ofn.setInt(56, maxFileChars);
            ofn.setPointer(80, titleBuf);
            ofn.setInt(88, OFN_EXPLORER | OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST | OFN_NOCHANGEDIR);
        } else {
            ofn.setPointer(12, filterBuf);
            ofn.setInt(24, 1);
            ofn.setPointer(28, fileBuf);
            ofn.setInt(32, maxFileChars);
            ofn.setPointer(48, titleBuf);
            ofn.setInt(52, OFN_EXPLORER | OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST | OFN_NOCHANGEDIR);
        }

        boolean accepted = COMDLG32.GetOpenFileNameW(ofn);
        if (!accepted) {
            return null;
        }
        String selectedPath = fileBuf.getWideString(0);
        if (selectedPath == null || selectedPath.isBlank()) {
            return null;
        }
        File result = new File(selectedPath.trim());
        return result.isFile() ? result : null;
    }

    private static Memory allocWideString(String text) {
        char[] chars = (text + "\0").toCharArray();
        Memory mem = new Memory((long) chars.length * 2L);
        mem.write(0, chars, 0, chars.length);
        return mem;
    }

    private static Memory allocWideChars(String rawWithNulls) {
        char[] chars = rawWithNulls.toCharArray();
        Memory mem = new Memory((long) chars.length * 2L);
        mem.write(0, chars, 0, chars.length);
        return mem;
    }

    private static void writeWideString(Memory dest, String value, int maxChars) {
        int count = Math.min(value.length(), maxChars);
        char[] chars = new char[count + 1];
        value.getChars(0, count, chars, 0);
        chars[count] = '\0';
        dest.write(0, chars, 0, chars.length);
    }

    public static boolean playSoundAsync(String path, float volumeScale) {
        if (path == null || path.isBlank()) {
            return false;
        }
        File file = new File(path.trim());
        if (!file.isFile()) {
            return false;
        }
        float clampedVol = Math.max(0.0f, Math.min(2.0f, volumeScale));
        if (clampedVol <= 0.005f) {
            return true;
        }
        Thread audioThread = new Thread(() -> playBlocking(file, clampedVol), "KillSoundPlayback");
        audioThread.setDaemon(true);
        audioThread.start();
        return true;
    }

    private static void playBlocking(File file, float volumeScale) {
        String lower = file.getName().toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".ogg")) {
                if (playOggVorbis(file, volumeScale)) {
                    return;
                }
            }
            if (lower.endsWith(".wav") || lower.endsWith(".au") || lower.endsWith(".aiff")) {
                if (playJavaSoundSampled(file, volumeScale)) {
                    return;
                }
            }
            if (IS_WINDOWS && WINMM != null) {
                if (playWindowsMci(file, volumeScale)) {
                    return;
                }
            }
            playJavaSoundSampled(file, volumeScale);
        } catch (Throwable ignored) {
        }
    }

    private static boolean playJavaSoundSampled(File file, float volumeScale) {
        try (InputStream rawIn = new BufferedInputStream(new FileInputStream(file));
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(rawIn)) {
            AudioFormat baseFormat = sourceStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
            );
            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(decodedFormat, sourceStream)) {
                DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(pcmStream);
                applyGainControl(clip, volumeScale);
                clip.start();
                long waitMs = Math.max(250L, (clip.getMicrosecondLength() / 1000L) + 120L);
                Thread.sleep(Math.min(waitMs, 15000L));
                clip.close();
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean playOggVorbis(File file, float volumeScale) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuf = stack.mallocInt(1);
            IntBuffer sampleRateBuf = stack.mallocInt(1);
            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_filename(file.getAbsolutePath(), channelsBuf, sampleRateBuf);
            if (pcm == null) {
                return false;
            }
            try {
                int channels = Math.max(1, channelsBuf.get(0));
                int sampleRate = Math.max(8000, sampleRateBuf.get(0));
                int totalSamples = pcm.remaining();
                ByteBuffer byteBuf = ByteBuffer.allocate(totalSamples * 2);
                byteBuf.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < totalSamples; i++) {
                    int scaled = Math.round(pcm.get(i) * volumeScale);
                    short clamped = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled));
                    byteBuf.putShort(clamped);
                }
                byte[] pcmBytes = byteBuf.array();
                AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format);
                    line.start();
                    line.write(pcmBytes, 0, pcmBytes.length);
                    line.drain();
                }
                return true;
            } finally {
                MemoryUtil.memFree(pcm);
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean playWindowsMci(File file, float volumeScale) {
        String alias = "ks_" + MCI_COUNTER.getAndIncrement();
        String absPath = file.getAbsolutePath().replace("\"", "");
        int openErr = WINMM.mciSendStringW(new WString("open \"" + absPath + "\" type mpegvideo alias " + alias), null, 0, null);
        if (openErr != 0) {
            return false;
        }
        try {
            int mciVol = Math.max(0, Math.min(1000, Math.round(volumeScale * 500.0f)));
            WINMM.mciSendStringW(new WString("setaudio " + alias + " volume to " + mciVol), null, 0, null);
            int playErr = WINMM.mciSendStringW(new WString("play " + alias + " wait"), null, 0, null);
            return playErr == 0;
        } finally {
            WINMM.mciSendStringW(new WString("close " + alias), null, 0, null);
        }
    }

    private static void applyGainControl(Clip clip, float volumeScale) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float db = (float) (20.0 * Math.log10(Math.max(0.0001f, volumeScale)));
        float clampedDb = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
        gain.setValue(clampedDb);
    }
}
