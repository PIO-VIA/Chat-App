package org.personnal.client.call;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Gestionnaire des périphériques audio
 * Permet de lister, sélectionner et configurer les périphériques audio
 */
public class AudioDeviceManager {

    private static final String PREF_MIC_DEVICE = "preferred_mic_device";
    private static final String PREF_SPEAKER_DEVICE = "preferred_speaker_device";
    private static final String PREF_MIC_VOLUME = "mic_volume";
    private static final String PREF_SPEAKER_VOLUME = "speaker_volume";

    private static final Preferences prefs = Preferences.userNodeForPackage(AudioDeviceManager.class);

    /**
     * Informations sur un périphérique audio
     */
    public static class AudioDeviceInfo {
        private final String name;
        private final Mixer.Info mixerInfo;
        private final boolean isInput;

        public AudioDeviceInfo(String name, Mixer.Info mixerInfo, boolean isInput) {
            this.name = name;
            this.mixerInfo = mixerInfo;
            this.isInput = isInput;
        }

        public String getName() {
            return name;
        }

        public Mixer.Info getMixerInfo() {
            return mixerInfo;
        }

        public boolean isInput() {
            return isInput;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Obtient la liste des périphériques d'entrée (microphones)
     * @return Liste des périphériques d'entrée disponibles
     */
    public static List<AudioDeviceInfo> getInputDevices() {
        List<AudioDeviceInfo> devices = new ArrayList<>();

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info lineInfo = new Line.Info(TargetDataLine.class);

            if (mixer.isLineSupported(lineInfo)) {
                String name = info.getName() + " - " + info.getDescription();
                devices.add(new AudioDeviceInfo(name, info, true));
            }
        }

        return devices;
    }

    /**
     * Obtient la liste des périphériques de sortie (haut-parleurs)
     * @return Liste des périphériques de sortie disponibles
     */
    public static List<AudioDeviceInfo> getOutputDevices() {
        List<AudioDeviceInfo> devices = new ArrayList<>();

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info lineInfo = new Line.Info(SourceDataLine.class);

            if (mixer.isLineSupported(lineInfo)) {
                String name = info.getName() + " - " + info.getDescription();
                devices.add(new AudioDeviceInfo(name, info, false));
            }
        }

        return devices;
    }

    /**
     * Obtient le périphérique d'entrée préféré
     * @param format Format audio souhaité
     * @return Ligne de capture audio configurée
     * @throws LineUnavailableException si le périphérique n'est pas disponible
     */
    public static TargetDataLine getPreferredInputDevice(AudioFormat format) throws LineUnavailableException {
        String preferredDevice = prefs.get(PREF_MIC_DEVICE, null);

        if (preferredDevice != null) {
            // Essayer d'utiliser le périphérique préféré
            try {
                for (AudioDeviceInfo device : getInputDevices()) {
                    if (device.getName().equals(preferredDevice)) {
                        Mixer mixer = AudioSystem.getMixer(device.getMixerInfo());
                        TargetDataLine line = (TargetDataLine) mixer.getLine(new Line.Info(TargetDataLine.class));
                        line.open(format);
                        return line;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'ouverture du périphérique d'entrée préféré: " + e.getMessage());
                // Continuer avec le périphérique par défaut
            }
        }

        // Utiliser le périphérique par défaut si le préféré n'est pas disponible
        TargetDataLine line = AudioSystem.getTargetDataLine(format);
        line.open(format);
        return line;
    }

    /**
     * Obtient le périphérique de sortie préféré
     * @param format Format audio souhaité
     * @return Ligne de sortie audio configurée
     * @throws LineUnavailableException si le périphérique n'est pas disponible
     */
    public static SourceDataLine getPreferredOutputDevice(AudioFormat format) throws LineUnavailableException {
        String preferredDevice = prefs.get(PREF_SPEAKER_DEVICE, null);

        if (preferredDevice != null) {
            // Essayer d'utiliser le périphérique préféré
            try {
                for (AudioDeviceInfo device : getOutputDevices()) {
                    if (device.getName().equals(preferredDevice)) {
                        Mixer mixer = AudioSystem.getMixer(device.getMixerInfo());
                        SourceDataLine line = (SourceDataLine) mixer.getLine(new Line.Info(SourceDataLine.class));
                        line.open(format);
                        return line;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'ouverture du périphérique de sortie préféré: " + e.getMessage());
                // Continuer avec le périphérique par défaut
            }
        }

        // Utiliser le périphérique par défaut si le préféré n'est pas disponible
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        return line;
    }

    /**
     * Définit le périphérique d'entrée préféré
     * @param device Périphérique à définir comme préféré
     */
    public static void setPreferredInputDevice(AudioDeviceInfo device) {
        if (device != null && device.isInput()) {
            prefs.put(PREF_MIC_DEVICE, device.getName());
        }
    }

    /**
     * Définit le périphérique de sortie préféré
     * @param device Périphérique à définir comme préféré
     */
    public static void setPreferredOutputDevice(AudioDeviceInfo device) {
        if (device != null && !device.isInput()) {
            prefs.put(PREF_SPEAKER_DEVICE, device.getName());
        }
    }

    /**
     * Règle le volume du microphone
     * @param volume Valeur entre 0.0 et 1.0
     */
    public static void setMicrophoneVolume(float volume) {
        if (volume >= 0.0f && volume <= 1.0f) {
            prefs.putFloat(PREF_MIC_VOLUME, volume);
        }
    }

    /**
     * Règle le volume du haut-parleur
     * @param volume Valeur entre 0.0 et 1.0
     */
    public static void setSpeakerVolume(float volume) {
        if (volume >= 0.0f && volume <= 1.0f) {
            prefs.putFloat(PREF_SPEAKER_VOLUME, volume);
        }
    }

    /**
     * Obtient le volume du microphone
     * @return Valeur entre 0.0 et 1.0
     */
    public static float getMicrophoneVolume() {
        return prefs.getFloat(PREF_MIC_VOLUME, 1.0f);
    }

    /**
     * Obtient le volume du haut-parleur
     * @return Valeur entre 0.0 et 1.0
     */
    public static float getSpeakerVolume() {
        return prefs.getFloat(PREF_SPEAKER_VOLUME, 1.0f);
    }

    /**
     * Applique le volume à un échantillon audio
     * @param sample Échantillon audio
     * @param volume Volume à appliquer (0.0 à 1.0)
     * @return Échantillon modifié
     */
    public static byte[] applyVolume(byte[] sample, float volume) {
        // Pour un format PCM 16 bits signé little-endian
        byte[] result = new byte[sample.length];

        for (int i = 0; i < sample.length; i += 2) {
            if (i + 1 < sample.length) {
                // Convertir les 2 octets en un short (16 bits)
                short audioSample = (short) ((sample[i+1] << 8) | (sample[i] & 0xFF));

                // Appliquer le volume
                audioSample = (short) (audioSample * volume);

                // Reconvertir en octets
                result[i] = (byte) (audioSample & 0xFF);
                result[i+1] = (byte) ((audioSample >> 8) & 0xFF);
            }
        }

        return result;
    }

    /**
     * Détecte si un échantillon audio contient du silence
     * @param sample Échantillon audio
     * @param threshold Seuil de détection (0.0 à 1.0)
     * @return true si l'échantillon est considéré comme du silence
     */
    public static boolean isSilence(byte[] sample, float threshold) {
        // Calculer la valeur RMS (Root Mean Square) de l'échantillon
        long sum = 0;

        for (int i = 0; i < sample.length; i += 2) {
            if (i + 1 < sample.length) {
                short audioSample = (short) ((sample[i+1] << 8) | (sample[i] & 0xFF));
                sum += audioSample * audioSample;
            }
        }

        // Calculer la valeur RMS
        double rms = Math.sqrt(sum / (sample.length / 2));

        // Normaliser entre 0 et 1
        double normalizedRms = rms / 32768.0; // 32768 est la valeur max pour un short (16 bits)

        // Comparer au seuil
        return normalizedRms < threshold;
    }
}