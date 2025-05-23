import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Client1 {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final String SERVER_IP = "192.168.1.178";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            VideoCapture capture = new VideoCapture(0);
            Mat frame = new Mat();
            MatOfByte buffer = new MatOfByte();

            // Audio setup
            AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, true);
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            // Thread to receive video and audio
            new Thread(() -> {
                try {
                    while (true) {
                        int imageSize = inputStream.readInt();
                        if (imageSize <= 0) break;

                        byte[] imageData = new byte[imageSize];
                        inputStream.readFully(imageData);

                        MatOfByte receivedBuffer = new MatOfByte(imageData);
                        Mat receivedFrame = Imgcodecs.imdecode(receivedBuffer, Imgcodecs.IMREAD_UNCHANGED);
                        if (!receivedFrame.empty()) {
                            HighGui.imshow("Video Call", receivedFrame);
                        }

                        int audioSize = inputStream.readInt();
                        if (audioSize > 0) {
                            byte[] audioData = new byte[audioSize];
                            inputStream.readFully(audioData);
                            speakers.write(audioData, 0, audioData.length);
                        }

                        HighGui.waitKey(30);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Thread to send video and audio
            new Thread(() -> {
                try {
                    byte[] audioBuffer = new byte[4096];

                    while (capture.isOpened()) {
                        capture.read(frame);
                        if (frame.empty()) break;

                        Imgcodecs.imencode(".jpg", frame, buffer);
                        byte[] imageData = buffer.toArray();

                        outputStream.writeInt(imageData.length);
                        outputStream.write(imageData);

                        int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                        outputStream.writeInt(bytesRead);
                        outputStream.write(audioBuffer, 0, bytesRead);

                        outputStream.flush();
                    }

                    capture.release();
                    socket.close();
                    HighGui.destroyAllWindows();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


