package org.personnal.serveur;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.personnal.serveur.protocol.PeerRequest;
import org.personnal.serveur.protocol.PeerResponse;
import org.personnal.serveur.protocol.RequestType;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client de test amélioré pour tester les fonctionnalités du serveur de chat
 */
public class TestClient {

    private static final Gson gson = new Gson();
    private static String username;
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Couleurs pour les messages dans la console
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    public static void main(String[] args) {
        System.out.println(ANSI_CYAN + "=== Client de Test pour Serveur de Chat ===" + ANSI_RESET);

        String host = "localhost";
        int port = 5000;

        // Permettre de configurer l'hôte et le port
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Port invalide, utilisation du port par défaut 5000" + ANSI_RESET);
            }
        }

        System.out.println("Connexion à " + host + ":" + port);

        try (
                Socket socket = new Socket(host, port);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                Scanner scanner = new Scanner(System.in)
        ) {
            socket.setSoTimeout(30000);  // Timeout de 30 secondes
            System.out.println(ANSI_GREEN + "✅ Connecté au serveur" + ANSI_RESET);

            boolean mainMenuRunning = true;
            while (mainMenuRunning) {
                printMainMenu();
                String choix = scanner.nextLine();

                switch (choix) {
                    case "1":
                        if (handleLogin(output, input, scanner)) {
                            startSession(output, input, scanner);
                        }
                        break;
                    case "2":
                        handleRegister(output, input, scanner);
                        break;
                    case "3":
                        mainMenuRunning = false;
                        sendQuit(output, input);
                        break;
                    case "4":
                        testCheckUserExists(output, input, scanner);
                        break;
                    case "5":
                        testConnectionStatus(output, input, scanner);
                        break;
                    default:
                        System.out.println(ANSI_RED + "❌ Choix invalide !" + ANSI_RESET);
                }
            }

        } catch (IOException e) {
            System.err.println(ANSI_RED + "❌ Erreur côté client : " + e.getMessage() + ANSI_RESET);
        }
    }

    private static void printMainMenu() {
        System.out.println("\n" + ANSI_CYAN + "=== Menu Principal ===" + ANSI_RESET);
        System.out.println("1. Se connecter");
        System.out.println("2. S'inscrire");
        System.out.println("3. Quitter");
        System.out.println("4. Tester vérification d'existence d'un utilisateur");
        System.out.println("5. Tester vérification du statut de connexion");
        System.out.print("Votre choix > ");
    }

    private static void printSessionMenu() {
        System.out.println("\n" + ANSI_CYAN + "=== Menu Session [Utilisateur: " + username + "] ===" + ANSI_RESET);
        System.out.println("1. Envoyer un message");
        System.out.println("2. Envoyer un fichier");
        System.out.println("3. Vérifier si un utilisateur existe");
        System.out.println("4. Vérifier si un utilisateur est en ligne");
        System.out.println("5. Déconnexion");
        System.out.print("Votre choix > ");
    }

    private static boolean handleLogin(BufferedWriter output, BufferedReader input, Scanner scanner)
            throws IOException {
        Map<String, String> credentials = getCredentials(scanner);
        PeerRequest request = new PeerRequest(RequestType.LOGIN, credentials);

        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

        if (response.isSuccess()) {
            username = credentials.get("username");
            System.out.println(ANSI_GREEN + "✅ Connexion réussie !" + ANSI_RESET);
            return true;
        } else {
            System.out.println(ANSI_RED + "❌ " + response.getMessage() + ANSI_RESET);
            return false;
        }
    }

    private static void handleRegister(BufferedWriter output, BufferedReader input, Scanner scanner)
            throws IOException {
        Map<String, String> credentials = getCredentialsRegister(scanner);
        PeerRequest request = new PeerRequest(RequestType.REGISTER, credentials);

        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);
        if (response.isSuccess()) {
            System.out.println(ANSI_GREEN + "✅ " + response.getMessage() + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + "❌ " + response.getMessage() + ANSI_RESET);
        }
    }

    private static void sendQuit(BufferedWriter output, BufferedReader input) throws IOException {
        PeerRequest request = new PeerRequest(RequestType.DISCONNECT, null);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);
        System.out.println(ANSI_YELLOW + "👋 " + response.getMessage() + ANSI_RESET);
    }

    private static void testCheckUserExists(BufferedWriter output, BufferedReader input, Scanner scanner) throws IOException {
        System.out.print("Nom d'utilisateur à vérifier : ");
        String usernameToCheck = scanner.nextLine();

        Map<String, String> payload = new HashMap<>();
        payload.put("username", usernameToCheck);

        PeerRequest request = new PeerRequest(RequestType.CHECK_USER, payload);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

        if (response.isSuccess()) {
            System.out.println(ANSI_GREEN + "✅ L'utilisateur " + usernameToCheck + " existe." + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + "❌ L'utilisateur " + usernameToCheck + " n'existe pas." + ANSI_RESET);
        }
    }

    private static void testConnectionStatus(BufferedWriter output, BufferedReader input, Scanner scanner) throws IOException {
        System.out.print("Nom d'utilisateur à vérifier : ");
        String usernameToCheck = scanner.nextLine();

        Map<String, String> payload = new HashMap<>();
        payload.put("username", usernameToCheck);

        PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

        if (response.isSuccess()) {
            System.out.println(ANSI_GREEN + "✅ L'utilisateur " + usernameToCheck + " est en ligne." + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + "❌ L'utilisateur " + usernameToCheck + " n'est pas en ligne." + ANSI_RESET);
        }
    }

    private static void startSession(BufferedWriter output, BufferedReader input, Scanner scanner) {
        // Thread d'écoute des messages entrants
        Thread listenerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && running.get()) {
                    try {
                        String responseJson = input.readLine();
                        if (responseJson == null) break;

                        PeerResponse response = gson.fromJson(responseJson, PeerResponse.class);
                        if (response.getData() instanceof Map) {
                            // Convertir le Map en cas de message ou fichier
                            Map<String, Object> dataMap = (Map<String, Object>) response.getData();

                            if (dataMap.containsKey("content") && dataMap.containsKey("sender") || dataMap.containsKey("from")) {
                                String sender = (String) (dataMap.containsKey("from") ? dataMap.get("from") : dataMap.get("sender"));
                                String content = (String) dataMap.get("content");
                                String timestamp = LocalDateTime.now().format(timeFormatter);

                                System.out.println("\n" + ANSI_YELLOW + "[" + timestamp + "] " +
                                        ANSI_PURPLE + sender + ANSI_RESET + ": " +
                                        ANSI_BLUE + content + ANSI_RESET);
                                printPrompt();
                            } else if (dataMap.containsKey("filename") && dataMap.containsKey("from")) {
                                // Cas d'un fichier reçu
                                String sender = (String) dataMap.get("from");
                                String filename = (String) dataMap.get("filename");
                                String timestamp = LocalDateTime.now().format(timeFormatter);

                                System.out.println("\n" + ANSI_YELLOW + "[" + timestamp + "] " +
                                        ANSI_PURPLE + sender + ANSI_RESET + ": " +
                                        ANSI_GREEN + "Fichier reçu: " + filename + ANSI_RESET);

                                // Option pour sauvegarder le fichier
                                System.out.print("Voulez-vous sauvegarder ce fichier? (o/n) > ");
                                String saveCh = scanner.nextLine();
                                if (saveCh.equalsIgnoreCase("o")) {
                                    saveReceivedFile(dataMap);
                                }

                                printPrompt();
                            }
                        } else if (response.getData() instanceof List) {
                            // Si c'est une liste (cas des utilisateurs connectés)
                            System.out.println(ANSI_CYAN + "👥 Utilisateurs connectés : " + response.getData() + ANSI_RESET);
                            printPrompt();
                        } else {
                            System.out.println(ANSI_CYAN + "🧭 Réponse du serveur : " + response.getMessage() + ANSI_RESET);
                            printPrompt();
                        }
                    } catch (SocketTimeoutException e) {
                        // Timeout normal, continuer l'écoute
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println(ANSI_RED + "❌ Erreur dans le thread d'écoute : " + e.getMessage() + ANSI_RESET);
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Boucle de commandes utilisateur
        boolean sessionRunning = true;
        while (sessionRunning && running.get()) {
            printSessionMenu();
            String command = scanner.nextLine();

            try {
                switch (command) {
                    case "1":
                        sendMessage(output, scanner);
                        break;
                    case "2":
                        sendFile(output, scanner);
                        break;
                    case "3":
                        checkUserExists(output, input, scanner);
                        break;
                    case "4":
                        checkUserOnline(output, input, scanner);
                        break;
                    case "5":
                        sessionRunning = false;
                        running.set(false);
                        sendQuit(output, input);
                        break;
                    default:
                        System.out.println(ANSI_RED + "❌ Commande inconnue." + ANSI_RESET);
                }
            } catch (IOException e) {
                System.err.println(ANSI_RED + "❌ Erreur lors de l'exécution de la commande : " + e.getMessage() + ANSI_RESET);
            }
        }

        // Arrêt propre du thread d'écoute
        listenerThread.interrupt();
        try {
            listenerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sendMessage(BufferedWriter output, Scanner scanner) throws IOException {
        System.out.print("Destinataire : ");
        String receiver = scanner.nextLine();
        System.out.print("Message : ");
        String content = scanner.nextLine();

        Map<String, String> payload = new HashMap<>();
        payload.put("sender", username);
        payload.put("receiver", receiver);
        payload.put("content", content);
        payload.put("read", "false");

        PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
        sendRequest(output, request);

        String timestamp = LocalDateTime.now().format(timeFormatter);
        System.out.println(ANSI_YELLOW + "[" + timestamp + "] " +
                ANSI_PURPLE + "Vous" + ANSI_RESET + " à " +
                ANSI_PURPLE + receiver + ANSI_RESET + ": " +
                ANSI_BLUE + content + ANSI_RESET);
    }

    private static void sendFile(BufferedWriter output, Scanner scanner) throws IOException {
        System.out.print("Destinataire : ");
        String receiver = scanner.nextLine();
        System.out.print("Chemin du fichier à envoyer : ");
        String filePath = scanner.nextLine();

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println(ANSI_RED + "❌ Fichier non trouvé : " + filePath + ANSI_RESET);
            return;
        }

        // Vérifier la taille du fichier
        if (file.length() > 5 * 1024 * 1024) { // 5 MB limite
            System.out.println(ANSI_RED + "❌ Fichier trop volumineux (max 5 MB)" + ANSI_RESET);
            return;
        }

        // Lire le contenu du fichier
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String base64Content = Base64.getEncoder().encodeToString(fileContent);

        Map<String, String> payload = new HashMap<>();
        payload.put("sender", username);
        payload.put("receiver", receiver);
        payload.put("filename", file.getName());
        payload.put("content", base64Content);

        PeerRequest request = new PeerRequest(RequestType.SEND_FILE, payload);
        sendRequest(output, request);

        System.out.println(ANSI_GREEN + "📁 Fichier " + file.getName() + " envoyé à " + receiver + ANSI_RESET);
    }

    private static void saveReceivedFile(Map<String, Object> fileData) {
        try {
            String filename = (String) fileData.get("filename");
            String base64Content = (String) fileData.get("content");

            // Créer un dossier "received_files" s'il n'existe pas
            Path downloadDir = Paths.get("received_files");
            if (!Files.exists(downloadDir)) {
                Files.createDirectory(downloadDir);
            }

            // Générer un nom de fichier unique
            String uniqueFilename = System.currentTimeMillis() + "_" + filename;
            Path filePath = downloadDir.resolve(uniqueFilename);

            // Décoder et sauvegarder le contenu
            byte[] fileContent = Base64.getDecoder().decode(base64Content);
            Files.write(filePath, fileContent);

            System.out.println(ANSI_GREEN + "✅ Fichier sauvegardé dans : " + filePath.toAbsolutePath() + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "❌ Erreur lors de la sauvegarde du fichier : " + e.getMessage() + ANSI_RESET);
        }
    }

    private static void checkUserExists(BufferedWriter output, BufferedReader input, Scanner scanner) throws IOException {
        System.out.print("Nom d'utilisateur à vérifier : ");
        String usernameToCheck = scanner.nextLine();

        Map<String, String> payload = new HashMap<>();
        payload.put("username", usernameToCheck);

        PeerRequest request = new PeerRequest(RequestType.CHECK_USER, payload);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

        if (response.isSuccess()) {
            System.out.println(ANSI_GREEN + "✅ L'utilisateur " + usernameToCheck + " existe." + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + "❌ L'utilisateur " + usernameToCheck + " n'existe pas." + ANSI_RESET);
        }
    }

    private static void checkUserOnline(BufferedWriter output, BufferedReader input, Scanner scanner) throws IOException {
        System.out.print("Nom d'utilisateur à vérifier : ");
        String usernameToCheck = scanner.nextLine();

        Map<String, String> payload = new HashMap<>();
        payload.put("username", usernameToCheck);

        PeerRequest request = new PeerRequest(RequestType.CHECK_ONLINE, payload);
        sendRequest(output, request);
        PeerResponse response = receiveResponse(input);

        if (response.isSuccess()) {
            System.out.println(ANSI_GREEN + "✅ L'utilisateur " + usernameToCheck + " est en ligne." + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + "❌ L'utilisateur " + usernameToCheck + " n'est pas en ligne." + ANSI_RESET);
        }
    }

    private static Map<String, String> getCredentials(Scanner scanner) {
        Map<String, String> creds = new HashMap<>();
        System.out.print("Nom d'utilisateur : ");
        creds.put("username", scanner.nextLine());
        System.out.print("Mot de passe : ");
        creds.put("password", scanner.nextLine());
        return creds;
    }

    private static Map<String, String> getCredentialsRegister(Scanner scanner) {
        Map<String, String> creds = new HashMap<>();
        System.out.print("Nom d'utilisateur : ");
        creds.put("username", scanner.nextLine());
        System.out.print("Email : ");
        creds.put("email", scanner.nextLine());
        System.out.print("Mot de passe : ");
        creds.put("password", scanner.nextLine());
        return creds;
    }

    private static void sendRequest(BufferedWriter output, PeerRequest request) throws IOException {
        String json = gson.toJson(request);
        output.write(json);
        output.newLine();
        output.flush();
    }

    private static PeerResponse receiveResponse(BufferedReader input) throws IOException {
        String json = input.readLine();
        Type responseType = new TypeToken<PeerResponse>() {}.getType();
        return gson.fromJson(json, responseType);
    }

    private static void printPrompt() {
        System.out.print(ANSI_CYAN + "\n> " + ANSI_RESET);
    }
}