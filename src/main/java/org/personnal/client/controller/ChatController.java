package org.personnal.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.personnal.client.MainClient;
import org.personnal.client.call.AudioCallManager;
import org.personnal.client.database.DAO.*;
import org.personnal.client.model.FileData;
import org.personnal.client.model.Message;
import org.personnal.client.model.User;
import org.personnal.client.network.ClientSocketManager;
import org.personnal.client.protocol.PeerRequest;
import org.personnal.client.protocol.PeerResponse;
import org.personnal.client.protocol.RequestType;
import org.personnal.client.UI.ChatView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class ChatController {
    private final MainClient app;
    private final String currentUsername;
    private final ClientSocketManager socketManager;
    private final ObservableList<String> contacts = FXCollections.observableArrayList();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private String currentChatPartner;

    // DAOs pour accéder à la base de données locale
    private final IUserDAO userDAO;
    private final IMessageDAO messageDAO;
    private final IFileDAO fileDAO;

    // Référence à la vue de chat
    private ChatView chatView;

    // Thread pool pour les opérations asynchrones
    private final ExecutorService fileTransferExecutor = Executors.newFixedThreadPool(2);

    // Cache des utilisateurs pour éviter des requêtes répétées à la base de données
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    // Cache des statuts en ligne
    private final Map<String, Boolean> onlineStatusCache = new ConcurrentHashMap<>();

    // Répertoire pour stocker les fichiers reçus
    private final String filesDirectory;

    // Drapeau pour éviter les mises à jour UI trop fréquentes
    private volatile boolean refreshingContactList = false;
    // Ajouter un attribut pour le gestionnaire d'appels
    private AudioCallManager audioCallManager;

    public ChatController(MainClient app, String currentUsername) throws IOException {
        this.app = app;
        this.currentUsername = currentUsername;
        this.socketManager = ClientSocketManager.getInstance();

        // Initialiser les DAOs
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
        this.fileDAO = new FileDAO();

        // Initialiser le gestionnaire d'appels
        this.audioCallManager = new AudioCallManager(this);

        // Créer le répertoire pour les fichiers reçus s'il n'existe pas
        this.filesDirectory = "files/" + currentUsername;
        createFilesDirectory();

        // Définir l'utilisateur courant comme propriété système pour les requêtes SQL
        System.setProperty("current.user", currentUsername);

        // Charger les contacts depuis la BD locale au démarrage
        loadContactsFromDatabase();
    }

    // Getter pour le gestionnaire d'appels
    public AudioCallManager getAudioCallManager() {
        return audioCallManager;
    }

    /**
     * Crée le répertoire pour les fichiers reçus
     */
    private void createFilesDirectory() {
        File directory = new File(filesDirectory);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Répertoire des fichiers créé: " + filesDirectory);
            } else {
                System.err.println("Impossible de créer le répertoire des fichiers: " + filesDirectory);
            }
        }
    }

    /**
     * Configure la vue de chat et démarre le listener de messages
     * @param chatView Vue de chat à configurer
     */
    public void setupChatView(ChatView chatView) {
        this.chatView = chatView;

        // Démarrer le listener de messages
        socketManager.startMessageListener(chatView, currentUsername);

        // Charger les statuts des contacts en arrière-plan
        preloadContactStatuses();

        // Démarrer la mise à jour périodique
        startPeriodicStatusUpdate();

        System.out.println("ChatView configurée pour l'utilisateur " + currentUsername);
    }

    /**
     * Vérifie si le listener de messages est actif et le redémarre si nécessaire
     */
    public void checkAndRestartMessageListener() {
        if (!socketManager.isMessageListenerRunning() && chatView != null) {
            System.out.println("MessageListener n'est plus en exécution, redémarrage...");

            // Redémarrer sur le thread JavaFX
            Platform.runLater(() -> {
                try {
                    socketManager.startMessageListener(chatView, currentUsername);
                } catch (Exception e) {
                    System.err.println("Erreur lors du redémarrage du MessageListener: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Affiche une notification à l'utilisateur
     */
    private void showNotification(String message) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Notification");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.show();

                // Fermer automatiquement après 3 secondes
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(alert::close);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } catch (Exception e) {
                System.err.println("Erreur lors de l'affichage de la notification: " + e.getMessage());
            }
        });
    }

    /**
     * Charge les contacts de l'utilisateur depuis la base de données locale
     */
    private void loadContactsFromDatabase() {
        // Vider les listes avant de les recharger
        contacts.clear();
        usersList.clear();
        userCache.clear();

        // Récupérer tous les utilisateurs depuis la base de données
        List<User> contactList = userDAO.findAll();

        for (User contact : contactList) {
            contacts.add(contact.getUsername());
            usersList.add(contact);
            userCache.put(contact.getUsername(), contact);
        }
    }

    /**
     * Envoie un message à l'utilisateur actuellement sélectionné et le sauvegarde en local
     */
    public boolean sendMessage(String receiver, String content) {
        System.out.println("🚀 [DEBUG] Début sendMessage - receiver: " + receiver + ", content: " + content);

        if (receiver == null || receiver.isEmpty() || content == null || content.isEmpty()) {
            System.out.println("❌ [DEBUG] Paramètres invalides");
            return false;
        }

        try {
            // Préparer le message pour l'envoi au serveur
            Map<String, String> payload = new HashMap<>();
            payload.put("sender", currentUsername);
            payload.put("receiver", receiver);
            payload.put("content", content);
            payload.put("read", "false");

            System.out.println("📦 [DEBUG] Payload créé: " + payload);

            // Créer un objet Message pour l'interface (optimiste)
            Message message = new Message();
            message.setSender(currentUsername);
            message.setReceiver(receiver);
            message.setContent(content);
            message.setTimestamp(LocalDateTime.now());
            message.setRead(true);

            System.out.println("💬 [DEBUG] Message object créé: " + message.getContent());

            // Vérifier l'état de la connexion
            if (socketManager == null) {
                System.out.println("❌ [DEBUG] SocketManager est null!");
                return false;
            }

            System.out.println("🔗 [DEBUG] SocketManager OK, envoi en cours...");

            // Envoyer le message au serveur en arrière-plan
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("📡 [DEBUG] Création de la requête...");

                    // Envoyer la requête
                    PeerRequest request = new PeerRequest(RequestType.SEND_MESSAGE, payload);
                    System.out.println("📤 [DEBUG] Envoi de la requête au serveur...");

                    socketManager.sendRequest(request);
                    System.out.println("⏳ [DEBUG] Requête envoyée, attente de la réponse...");

                    PeerResponse response = socketManager.readResponse();
                    System.out.println("📥 [DEBUG] Réponse reçue: success=" + response.isSuccess() +
                            ", message=" + response.getMessage());

                    if (response.isSuccess()) {
                        // Sauvegarder le message dans la BD locale
                        System.out.println("💾 [DEBUG] Sauvegarde en BD...");
                        messageDAO.saveMessage(message);
                        System.out.println("✅ [DEBUG] Message sauvegardé avec succès");

                        // Rafraîchir l'UI
                        Platform.runLater(() -> {
                            System.out.println("🔄 [DEBUG] Rafraîchissement de l'UI...");
                            if (chatView != null && receiver.equals(currentChatPartner)) {
                                chatView.refreshMessages();
                            }
                        });
                    } else {
                        System.out.println("❌ [DEBUG] Échec de l'envoi: " + response.getMessage());
                        showNotification("Erreur lors de l'envoi du message: " + response.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("💥 [DEBUG] Exception IOException: " + e.getMessage());
                    e.printStackTrace();
                    showNotification("Problème de connexion lors de l'envoi du message.");
                } catch (Exception e) {
                    System.err.println("💥 [DEBUG] Exception générale: " + e.getMessage());
                    e.printStackTrace();
                    showNotification("Erreur inattendue lors de l'envoi du message.");
                }
            });

            System.out.println("✅ [DEBUG] sendMessage retourne true");
            return true;
        } catch (Exception e) {
            System.err.println("💥 [DEBUG] Exception dans sendMessage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère un contact par son nom d'utilisateur (avec cache)
     */
    public User getUserByUsername(String username) {
        // Vérifier le cache d'abord
        User cachedUser = userCache.get(username);
        if (cachedUser != null) {
            return cachedUser;
        }

        // Sinon, chercher dans la BD et mettre en cache
        User user = userDAO.findByUsername(username);
        if (user != null) {
            userCache.put(username, user);
        }
        return user;
    }

    /**
     * Charge les messages pour un contact spécifique depuis la base de données locale
     */
    public ObservableList<Message> loadMessagesForContact(String contact) {
        ObservableList<Message> messages = FXCollections.observableArrayList();

        try {
            // Définir l'utilisateur courant comme propriété système pour la requête SQL
            System.setProperty("current.user", currentUsername);

            // Récupérer les messages échangés avec ce contact depuis la BD locale
            List<Message> messageList = messageDAO.getMessagesWith(contact);
            messages.addAll(messageList);

            // Récupérer également les fichiers échangés avec ce contact
            List<FileData> fileList = fileDAO.getFilesWith(contact);

            // Convertir les fichiers en messages pour les afficher dans la conversation
            for (FileData file : fileList) {
                Message fileMessage = new Message();
                fileMessage.setIdMessage(file.getId());
                fileMessage.setSender(file.getSender());
                fileMessage.setReceiver(file.getReceiver());
                fileMessage.setContent("Fichier: " + file.getFilename());
                fileMessage.setTimestamp(file.getTimestamp());
                fileMessage.setRead(file.isRead());

                messages.add(fileMessage);
            }

            // Trier les messages par date
            messages.sort((m1, m2) -> {
                if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                if (m1.getTimestamp() == null) return -1;
                if (m2.getTimestamp() == null) return 1;
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            });

            // Marquer les messages du contact comme lus
            messageDAO.markMessagesAsRead(contact, currentUsername);

            // Marquer également les fichiers comme lus
            if (fileDAO instanceof FileDAO) {
                ((FileDAO) fileDAO).markFilesAsRead(contact, currentUsername);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Gère l'envoi de fichier et le sauvegarde en local
     */
    public void handleAttachment() {
        if (currentChatPartner == null) {
            showNotification("Veuillez sélectionner un contact avant d'envoyer un fichier.");
            return;
        }

        File file = chooseFile();
        if (file != null) {
            // Vérifier la taille du fichier
            if (file.length() > 10 * 1024 * 1024) { // 10MB max
                showNotification("Le fichier est trop volumineux. Taille maximale: 10MB");
                return;
            }

            // Créer un FileData pour la base de données
            FileData fileData = new FileData();
            fileData.setSender(currentUsername);
            fileData.setReceiver(currentChatPartner);
            fileData.setFilename(file.getName());
            fileData.setFilepath(saveFileLocally(file));
            fileData.setTimestamp(LocalDateTime.now());
            fileData.setRead(true);

            // Afficher un indicateur de progression
            Platform.runLater(() -> {
                if (chatView != null) {
                    // Montrer que l'envoi est en cours
                    showNotification("Envoi du fichier en cours...");
                }
            });

            // Envoi en arrière-plan
            fileTransferExecutor.submit(() -> {
                try {
                    sendFileInBackground(currentChatPartner, file, fileData);

                    // Sauvegarder en BD seulement après envoi réussi
                    fileDAO.saveFile(fileData);

                    Platform.runLater(() -> {
                        if (chatView != null) {
                            chatView.refreshMessages();
                            showNotification("Fichier envoyé avec succès!");
                        }
                    });

                    System.out.println("Fichier envoyé avec succès au serveur : " + fileData.getFilename());
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi du fichier: " + e.getMessage());
                    Platform.runLater(() -> {
                        showNotification("Erreur lors de l'envoi du fichier: " + e.getMessage());
                    });
                }
            });
        }
    }

    /**
     * Ouvre un fichier
     */
    public void openFile(int fileId) {
        // Récupérer les informations du fichier
        FileData fileData = null;
        if (fileDAO instanceof FileDAO) {
            fileData = ((FileDAO) fileDAO).getFileById(fileId);
        }

        if (fileData == null || fileData.getFilepath() == null) {
            showNotification("Fichier introuvable.");
            return;
        }

        // Ouvrir le fichier avec l'application par défaut du système
        try {
            File file = new File(fileData.getFilepath());
            if (!file.exists()) {
                showNotification("Le fichier n'existe plus à cet emplacement.");
                return;
            }

            // Utiliser la méthode appropriée selon le système d'exploitation
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                new ProcessBuilder("open", file.getAbsolutePath()).start();
            } else {
                new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
            }
        } catch (IOException e) {
            showNotification("Erreur lors de l'ouverture du fichier: " + e.getMessage());
        }
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");
        return fileChooser.showOpenDialog(null);
    }

    private String saveFileLocally(File file) {
        try {
            // Créer un nom de fichier unique (timestamp + nom original)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFilename = timestamp + "_" + file.getName();

            // Chemin de destination
            Path destination = Paths.get(filesDirectory, uniqueFilename);

            // Créer le répertoire parent si nécessaire
            Files.createDirectories(destination.getParent());

            // Copier le fichier
            Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            return destination.toString();
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde locale du fichier: " + e.getMessage());
            return file.getAbsolutePath(); // Utiliser le chemin original en cas d'erreur
        }
    }

    private void sendFileInBackground(String receiver, File file, FileData fileData) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String base64Content = Base64.getEncoder().encodeToString(fileContent);

        Map<String, String> payload = new HashMap<>();
        payload.put("sender", currentUsername);
        payload.put("receiver", receiver);
        payload.put("filename", file.getName());
        payload.put("content", base64Content);

        PeerRequest request = new PeerRequest(RequestType.SEND_FILE, payload);
        socketManager.sendRequest(request);
        PeerResponse response = socketManager.readResponse();

        if (!response.isSuccess()) {
            throw new IOException("Échec de l'envoi: " + response.getMessage());
        }


        System.out.println("Fichier envoyé avec succès au serveur : " + fileData.getFilename());
    }

    /**
     * Vérifie si un utilisateur existe sur le serveur
     * @param username Le nom d'utilisateur à vérifier
     * @return true si l'utilisateur existe, false sinon
     */
    // Ajouter un cache pour les vérifications d'utilisateurs
    private final Map<String, Boolean> userExistsCache = new ConcurrentHashMap<>();
    private final long USER_CACHE_DURATION = 3600000; // 1 heure en millisecondes
    private final Map<String, Long> userCacheTimestamps = new ConcurrentHashMap<>();

    public boolean checkUserExists(String username) {
        if (username == null || username.isEmpty() || username.equals(currentUsername)) {
            return false;
        }

        // Déléguer au SocketManager qui gère le cache intelligent
        return socketManager.checkUserExists(username);
    }


    /**
     * Rafraîchit le statut en ligne de tous les contacts
     * @return Map des statuts (nom d'utilisateur -> en ligne)
     */
    public Map<String, Boolean> refreshContactStatuses() {
        // Copier la liste des contacts pour éviter les modifications concurrentes
        List<String> contactsToCheck = List.copyOf(contacts);

        if (contactsToCheck.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Utiliser la requête par lot pour tous les contacts
            CompletableFuture<Map<String, Boolean>> future = socketManager.batchCheckOnlineStatus(contactsToCheck);

            // Attendre avec un timeout raisonnable
            Map<String, Boolean> results = future.get(8, TimeUnit.SECONDS);

            // Mettre à jour le cache local
            onlineStatusCache.putAll(results);

            return results;

        } catch (TimeoutException e) {
            System.err.println("Timeout lors du rafraîchissement des statuts des contacts");
            return new HashMap<>();
        } catch (Exception e) {
            System.err.println("Erreur lors du rafraîchissement des statuts: " + e.getMessage());
            return new HashMap<>();
        }
    }




    // Méthode auxiliaire pour vérifier le statut en ligne d'un contact
    private Boolean checkOnlineStatus(String username) {
        // Vérifier le cache d'abord
        Boolean cachedStatus = onlineStatusCache.get(username);
        if (cachedStatus != null) {
            // Utiliser la version en cache pour l'UI immédiate, mais rafraîchir en arrière-plan
            CompletableFuture.runAsync(() -> {
                try {
                    boolean online = socketManager.isUserOnline(username);
                    // Mettre à jour le cache
                    onlineStatusCache.put(username, online);
                    // Rafraîchir l'UI si le statut a changé
                    if (online != cachedStatus && chatView != null) {
                        Platform.runLater(() -> chatView.updateContactStatus(username, online));
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs en arrière-plan
                }
            });
            return cachedStatus;
        }

        // Si pas de cache, faire la requête synchrone
        try {
            return socketManager.isUserOnline(username);
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du statut de " + username + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Ajoute un nouveau contact à la base de données locale
     * @param username Le nom d'utilisateur à ajouter comme contact
     * @param email L'email du contact
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addContact(String username, String email) {
        // Vérification de base
        if (username == null || username.isEmpty() || username.equals(currentUsername) || contacts.contains(username)) {
            return false;
        }

        // Vérifier si l'utilisateur existe sur le serveur
        if (!checkUserExists(username)) {
            return false;
        }

        // Ajouter à la base de données locale
        User newContact = new User(username, email);
        try {
            userDAO.insert(newContact);

            // Mettre à jour les listes observables pour l'interface
            Platform.runLater(() -> {
                contacts.add(username);
                usersList.add(newContact);
                userCache.put(username, newContact);
            });

            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout du contact : " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime un contact de la base de données locale
     */
    public boolean deleteContact(int userId) {
        try {
            User user = userDAO.findById(userId);
            if (user != null) {
                // Supprimer le contact de la base de données
                userDAO.delete(userId);

                // Mettre à jour les listes observables pour l'interface
                Platform.runLater(() -> {
                    contacts.remove(user.getUsername());
                    usersList.removeIf(u -> u.getIdUser() == userId);
                    userCache.remove(user.getUsername());
                    onlineStatusCache.remove(user.getUsername());
                });

                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du contact : " + e.getMessage());
            return false;
        }
    }

    /**
     * Filtre les contacts selon le texte de recherche
     */
    public void filterContacts(String searchText) {
        // Pour éviter plusieurs opérations simultanées
        if (refreshingContactList) return;

        refreshingContactList = true;

        try {
            // Recharger tous les contacts si le texte de recherche est vide
            if (searchText == null || searchText.isEmpty()) {
                loadContactsFromDatabase();
                return;
            }

            // Sinon, filtrer les contacts par nom d'utilisateur ou email
            String searchLower = searchText.toLowerCase();
            contacts.clear();
            usersList.clear();

            List<User> allContacts = userDAO.findAll();
            for (User user : allContacts) {
                if (user.getUsername().toLowerCase().contains(searchLower) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower))) {
                    contacts.add(user.getUsername());
                    usersList.add(user);
                }
            }
        } finally {
            refreshingContactList = false;
        }
    }

    /**
     * Vérifie si un utilisateur est en ligne
     * Utilise le cache local pour éviter des requêtes répétées
     */
    public boolean isUserOnline(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        // Déléguer au SocketManager qui gère le cache et les requêtes asynchrones
        return socketManager.isUserOnline(username);
    }

    /**
     * *** CHARGEMENT PRÉEMPTIF DES STATUTS ***
     * Charge les statuts de tous les contacts en arrière-plan au démarrage
     */
    public void preloadContactStatuses() {
        if (contacts.isEmpty()) {
            return;
        }

        // Charger les statuts en arrière-plan sans bloquer l'UI
        CompletableFuture.runAsync(() -> {
            try {
                List<String> contactsList = List.copyOf(contacts);
                socketManager.batchCheckOnlineStatus(contactsList)
                        .thenAccept(results -> {
                            // Mettre à jour le cache local
                            onlineStatusCache.putAll(results);

                            // Rafraîchir l'UI si nécessaire
                            if (chatView != null) {
                                Platform.runLater(() -> chatView.updateContactList());
                            }
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Erreur lors du chargement préemptif des statuts: " + throwable.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement préemptif: " + e.getMessage());
            }
        });
    }

    /**
     * *** MISE À JOUR PÉRIODIQUE DES STATUTS ***
     * Actualise périodiquement les statuts sans intervention de l'utilisateur
     */
    private final ScheduledExecutorService statusUpdateScheduler = Executors.newSingleThreadScheduledExecutor();

    public void startPeriodicStatusUpdate() {
        // Mettre à jour les statuts toutes les 2 minutes
        statusUpdateScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!contacts.isEmpty()) {
                    List<String> contactsList = List.copyOf(contacts);
                    socketManager.batchCheckOnlineStatus(contactsList)
                            .thenAccept(results -> {
                                // Mettre à jour seulement si il y a des changements
                                boolean hasChanges = results.entrySet().stream()
                                        .anyMatch(entry -> {
                                            Boolean cached = onlineStatusCache.get(entry.getKey());
                                            return cached == null || !cached.equals(entry.getValue());
                                        });

                                if (hasChanges) {
                                    onlineStatusCache.putAll(results);

                                    // Rafraîchir l'UI sur le thread JavaFX
                                    if (chatView != null) {
                                        Platform.runLater(() -> chatView.updateContactList());
                                    }
                                }
                            })
                            .exceptionally(throwable -> {
                                // Log silencieux pour éviter le spam
                                return null;
                            });
                }
            } catch (Exception e) {
                // Log silencieux pour la mise à jour périodique
            }
        }, 30, 120, TimeUnit.SECONDS); // Démarrer après 30 secondes, puis toutes les 2 minutes
    }


    /**
     * Enregistre un message reçu dans la base de données locale
     */
    public void saveReceivedMessage(Message message) {
        try {
            // Marquer le message comme non lu puisqu'il vient d'être reçu
            message.setRead(false);
            messageDAO.saveMessage(message);

            // Si la vue est disponible et que le message vient du partenaire de chat actuel,
            // rafraîchir la liste des messages
            if (chatView != null && currentChatPartner != null &&
                    message.getSender().equals(currentChatPartner)) {

                // Marquer le message comme lu immédiatement
                messageDAO.markMessagesAsRead(currentChatPartner, currentUsername);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du message reçu: " + e.getMessage());
        }
    }

    /**
     * Enregistre un fichier reçu dans la base de données locale
     */
    public void saveReceivedFile(FileData file) {
        try {
            // Assurer que le chemin de fichier est défini
            if (file.getFilepath() == null || file.getFilepath().isEmpty()) {
                // Créer un chemin dans le répertoire des fichiers
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String uniqueFilename = timestamp + "_" + file.getFilename();
                file.setFilepath(Paths.get(filesDirectory, uniqueFilename).toString());
            }

            // Marquer le fichier comme non lu puisqu'il vient d'être reçu
            file.setRead(false);
            fileDAO.saveFile(file);

            // Si la vue est disponible et que le fichier vient du partenaire de chat actuel,
            // le marquer comme lu
            if (chatView != null && currentChatPartner != null &&
                    file.getSender().equals(currentChatPartner) &&
                    fileDAO instanceof FileDAO) {

                ((FileDAO) fileDAO).markFilesAsRead(currentChatPartner, currentUsername);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du fichier reçu: " + e.getMessage());
        }
    }

    /**
     * Déconnecte l'utilisateur
     */
    public void disconnect() {
        try {
            // Arrêter les tâches périodiques
            statusUpdateScheduler.shutdown();
            try {
                if (!statusUpdateScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    statusUpdateScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                statusUpdateScheduler.shutdownNow();
            }

            fileTransferExecutor.shutdownNow();

            // Arrêter le listener de messages avant de fermer la connexion
            socketManager.stopMessageListener();

            // Envoyer la requête de déconnexion
            try {
                PeerRequest request = new PeerRequest(RequestType.DISCONNECT, null);
                socketManager.sendRequest(request);
                socketManager.readResponse();
            } catch (Exception e) {
                // Ignorer les erreurs lors de la déconnexion
            }

            // Fermer proprement les appels audio en cours
            if (audioCallManager != null) {
                if (audioCallManager.getCallStatus() != AudioCallManager.CallStatus.IDLE) {
                    audioCallManager.endCall();
                }
            }

            // Fermer la connexion
            socketManager.closeConnection();
        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    // Getters
    public String getCurrentUsername() {
        return currentUsername;
    }

    public ObservableList<String> getContacts() {
        return contacts;
    }

    public ObservableList<User> getUsersList() {
        return usersList;
    }

    public void setCurrentChatPartner(String partner) {
        this.currentChatPartner = partner;
    }

    public IUserDAO getUserDAO() {
        return userDAO;
    }

    public IMessageDAO getMessageDAO() {
        return messageDAO;
    }

    public IFileDAO getFileDAO() {
        return fileDAO;
    }
}