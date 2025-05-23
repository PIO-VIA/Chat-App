import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final int PORT = 5000;
    private static ConcurrentHashMap<Integer, DataOutputStream> clients = new ConcurrentHashMap<>();
    private static int clientCount = 0;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started... Waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                int clientId = clientCount++;
                System.out.println("Client " + clientId + " connected.");
                clients.put(clientId, new DataOutputStream(socket.getOutputStream()));

                new Thread(new ClientHandler(socket, clientId)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private int clientId;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                while (true) {
                    // Read image size
                    int imageSize = inputStream.readInt();
                    if (imageSize <= 0) break;

                    // Read image data
                    byte[] imageData = new byte[imageSize];
                    inputStream.readFully(imageData);

                    // Read audio size
                    int audioSize = inputStream.readInt();
                    if (audioSize <= 0) break;

                    // Read audio data
                    byte[] audioData = new byte[audioSize];
                    inputStream.readFully(audioData);

                    // Send to all other clients
                    for (int id : clients.keySet()) {
                        if (id != clientId) {
                            clients.get(id).writeInt(imageSize);
                            clients.get(id).write(imageData);
                            clients.get(id).writeInt(audioSize);
                            clients.get(id).write(audioData);
                            clients.get(id).flush();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Client " + clientId + " disconnected.");
                clients.remove(clientId);
            }
        }
    }
}

