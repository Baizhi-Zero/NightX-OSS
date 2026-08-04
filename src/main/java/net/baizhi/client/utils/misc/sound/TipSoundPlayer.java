package net.baizhi.client.utils.misc.sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.File;

public class TipSoundPlayer {
    private final File file;

    public TipSoundPlayer(File file) {
        this.file = file;
    }

    public void asyncPlay(float volume) {
        Thread thread = new Thread(() -> playSound(volume / 100F));
        thread.setDaemon(true);
        thread.start();
    }

    public void playSound(float volume) {
        Clip clip = null;
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            FloatControl controller = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float range = controller.getMaximum() - controller.getMinimum();
            float value = (range * volume) + controller.getMinimum();

            controller.setValue(value);

            final Clip playingClip = clip;
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    playingClip.close();
                }
            });

            clip.start();
        } catch (Exception ex) {
            if (clip != null) {
                try {
                    clip.close();
                } catch (Exception ignored) {
                }
            }
            System.out.println("Error with playing sound.");
            ex.printStackTrace();
        }
    }
}
