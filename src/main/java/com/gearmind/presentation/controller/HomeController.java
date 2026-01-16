package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.message.ListConversationsUseCase;
import com.gearmind.application.message.ListMessagesUseCase;
import com.gearmind.application.message.SendMessageUseCase;
import com.gearmind.domain.message.ConversationSummary;
import com.gearmind.domain.message.InternalMessage;
import com.gearmind.domain.message.MessageRepository;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.message.MySqlMessageRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class HomeController {

    @FXML
    private BorderPane root;
    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentPane;
    @FXML
    private Button btnToggleSidebar;
    @FXML
    private HBox userBox;
    @FXML
    private Label lblUsuarioActual;
    @FXML
    private Button btnUserMenu;
    @FXML
    private Button btnFichajes;
    @FXML
    private Button btnMensajes;
    @FXML
    private Button btnNavDashboard;
    @FXML
    private Button btnNavReportes;
    @FXML
    private Button btnNavCitas;
    @FXML
    private Button btnNavClientes;
    @FXML
    private Button btnNavVehiculos;
    @FXML
    private Button btnNavProductos;
    @FXML
    private Button btnNavUsuarios;
    @FXML
    private Button btnNavEmpresas;
    @FXML
    private Button btnNavReparaciones;
    @FXML
    private Button btnNavPresupuestos;
    @FXML
    private Button btnNavFacturas;
    @FXML
    private Button btnNavTareas;

    private javafx.scene.Node savedSidebar;
    private ContextMenu userMenu;
    private ContextMenu messagesMenu;
    private ListView<ConversationSummary> conversationList;
    private ListView<InternalMessage> messageList;
    private TextField messageInput;
    private Button btnSendMessage;
    private ConversationSummary activeConversation;
    private final MessageRepository messageRepository = new MySqlMessageRepository();
    private final ListConversationsUseCase listConversationsUseCase = new ListConversationsUseCase(messageRepository);
    private final ListMessagesUseCase listMessagesUseCase = new ListMessagesUseCase(messageRepository);
    private final SendMessageUseCase sendMessageUseCase = new SendMessageUseCase(messageRepository);

    @FXML
    public void initialize() {
        savedSidebar = sidebar;
        setupFromAuthContext();
        initUserMenu();
        initMessagesMenu();

        if (userBox != null) {
            userBox.setOnMouseClicked(e -> showUserMenu());
        }
        if (lblUsuarioActual != null) {
            lblUsuarioActual.setOnMouseClicked(e -> showUserMenu());
        }

        loadView("/view/DashboardView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    private void setupFromAuthContext() {
        if (!AuthContext.isLoggedIn()) {
            if (lblUsuarioActual != null) {
                lblUsuarioActual.setText("Invitado");
            }
            applyRoleToSidebar(null);
            return;
        }

        User user = AuthContext.getCurrentUser();
        UserRole role = AuthContext.getRole();

        if (lblUsuarioActual != null && user != null) {
            String rolTexto = switch (role) {
                case SUPER_ADMIN ->
                    "Super admin";
                case ADMIN ->
                    "Admin";
                case EMPLEADO ->
                    "Empleado";
            };
            lblUsuarioActual.setText(user.getNombre() + " (" + rolTexto + ")");
        }

        applyRoleToSidebar(role);
    }

    private void applyRoleToSidebar(UserRole role) {
        setAllSidebarButtonsVisible(true);

        if (role == null) {
            hideButton(btnNavEmpresas);
            hideButton(btnNavUsuarios);
            return;
        }

        if (role == UserRole.SUPER_ADMIN) {
            return;
        }

        if (role == UserRole.ADMIN) {
            hideButton(btnNavEmpresas);
            return;
        }

        if (role == UserRole.EMPLEADO) {
            hideButton(btnNavEmpresas);
            hideButton(btnNavUsuarios);
            hideButton(btnNavReportes);
        }
    }

    private void setAllSidebarButtonsVisible(boolean visible) {
        for (Button b : List.of(btnNavDashboard, btnNavReportes, btnNavCitas, btnNavReparaciones, btnNavPresupuestos, btnNavFacturas, btnNavTareas, btnNavClientes, btnNavVehiculos, btnNavProductos, btnNavUsuarios, btnNavEmpresas)) {
            if (b != null) {
                b.setVisible(visible);
                b.setManaged(visible);
            }
        }
    }

    private void hideButton(Button b) {
        if (b != null) {
            b.setVisible(false);
            b.setManaged(false);
        }
    }

    private void initUserMenu() {
        MenuItem miManage = new MenuItem("Gestionar cuenta");
        miManage.setOnAction(e -> onManageAccount());
        MenuItem miLogout = new MenuItem("Cerrar sesión");
        miLogout.setOnAction(e -> onLogout());
        userMenu = new ContextMenu(miManage, new SeparatorMenuItem(), miLogout);
        userMenu.setAutoHide(true);
    }

    private void initMessagesMenu() {
        if (btnMensajes == null) {
            return;
        }

        conversationList = new ListView<>();
        conversationList.getStyleClass().add("tfx-chat-list");
        conversationList.setPlaceholder(new Label("Sin conversaciones"));
        conversationList.setPrefWidth(220);

        conversationList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ConversationSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(item.getContactoNombre());
                name.getStyleClass().add("tfx-chat-contact-name");
                Label snippet = new Label(Optional.ofNullable(item.getUltimoMensaje()).orElse(""));
                snippet.getStyleClass().add("tfx-chat-contact-snippet");
                VBox box = new VBox(2, name, snippet);
                setGraphic(box);
                setText(null);
            }
        });

        messageList = new ListView<>();
        messageList.getStyleClass().add("tfx-chat-thread");
        messageList.setPlaceholder(new Label("Selecciona una conversación"));

        messageList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(InternalMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                boolean isOutgoing = AuthContext.isLoggedIn() && item.getSenderId() == AuthContext.getCurrentUser().getId();
                Label bubble = new Label(item.getContenido());
                bubble.getStyleClass().add("tfx-chat-bubble");
                bubble.getStyleClass().add(isOutgoing ? "tfx-chat-bubble-out" : "tfx-chat-bubble-in");
                bubble.setWrapText(true);
                bubble.setMaxWidth(220);

                HBox container = new HBox(bubble);
                container.setAlignment(isOutgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                setGraphic(container);
                setText(null);
            }
        });

        messageInput = new TextField();
        messageInput.setPromptText("Escribe un mensaje...");
        messageInput.getStyleClass().add("tfx-chat-input-field");
        messageInput.setOnAction(e -> sendMessage());

        btnSendMessage = new Button("Enviar");
        btnSendMessage.getStyleClass().addAll("button", "tfx-btn-primary", "tfx-chat-send");
        btnSendMessage.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(8, messageInput, btnSendMessage);
        inputBox.getStyleClass().add("tfx-chat-input");
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        VBox chatPane = new VBox(8, messageList, inputBox);
        VBox.setVgrow(messageList, Priority.ALWAYS);

        HBox content = new HBox(12, conversationList, chatPane);
        content.getStyleClass().add("tfx-chat-popup");
        HBox.setHgrow(chatPane, Priority.ALWAYS);

        CustomMenuItem item = new CustomMenuItem(content, false);
        item.setHideOnClick(false);
        messagesMenu = new ContextMenu(item);
        messagesMenu.setAutoHide(true);

        conversationList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            activeConversation = newValue;
            loadMessages(newValue);
        });

        setChatEnabled(false);
        Tooltip.install(btnMensajes, new Tooltip("Mensajería interna"));
        btnMensajes.setDisable(!AuthContext.isLoggedIn());
    }

    @FXML
    private void onUserMenu() {
        showUserMenu();
    }

    @FXML
    private void onMessagesMenu() {
        showMessagesMenu();
    }

    private void showUserMenu() {
        if (userMenu == null || btnUserMenu == null) {
            return;
        }

        if (userMenu.isShowing()) {
            userMenu.hide();
            return;
        }

        userMenu.show(btnUserMenu, Side.BOTTOM, 0, 6);
    }

    private void showMessagesMenu() {
        if (messagesMenu == null || btnMensajes == null) {
            return;
        }

        if (messagesMenu.isShowing()) {
            messagesMenu.hide();
            return;
        }

        refreshConversations();
        messagesMenu.show(btnMensajes, Side.BOTTOM, 0, 6);
    }

    private void refreshConversations() {
        if (!AuthContext.isLoggedIn()) {
            conversationList.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            long empresaId = AuthContext.getEmpresaId();
            long userId = AuthContext.getCurrentUser().getId();
            List<ConversationSummary> conversations = listConversationsUseCase.execute(empresaId, userId);
            conversationList.setItems(FXCollections.observableArrayList(conversations));

            if (activeConversation != null) {
                conversations.stream()
                        .filter(c -> c.getContactoId() == activeConversation.getContactoId())
                        .findFirst()
                        .ifPresent(conversationList.getSelectionModel()::select);
            }
        } catch (RuntimeException ex) {
            showChatError("No se pudieron cargar las conversaciones", ex);
        }
    }

    private void loadMessages(ConversationSummary conversation) {
        if (conversation == null || !AuthContext.isLoggedIn()) {
            messageList.setItems(FXCollections.observableArrayList());
            setChatEnabled(false);
            return;
        }

        try {
            long empresaId = AuthContext.getEmpresaId();
            long userId = AuthContext.getCurrentUser().getId();
            List<InternalMessage> messages = listMessagesUseCase.execute(empresaId, userId, conversation.getContactoId());
            messageList.setItems(FXCollections.observableArrayList(messages));
            setChatEnabled(true);
            if (!messages.isEmpty()) {
                messageList.scrollTo(messages.size() - 1);
            }
        } catch (RuntimeException ex) {
            showChatError("No se pudieron cargar los mensajes", ex);
        }
    }

    private void sendMessage() {
        if (activeConversation == null || !AuthContext.isLoggedIn()) {
            return;
        }

        String contenido = messageInput.getText();
        if (contenido == null || contenido.isBlank()) {
            return;
        }

        try {
            long empresaId = AuthContext.getEmpresaId();
            long senderId = AuthContext.getCurrentUser().getId();
            sendMessageUseCase.execute(empresaId, senderId, activeConversation.getContactoId(), contenido.trim());
            messageInput.clear();
            loadMessages(activeConversation);
            refreshConversations();
        } catch (RuntimeException ex) {
            showChatError("No se pudo enviar el mensaje", ex);
        }
    }

    private void setChatEnabled(boolean enabled) {
        messageInput.setDisable(!enabled);
        btnSendMessage.setDisable(!enabled);
    }

    private void showChatError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Mensajería interna");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void onManageAccount() {
        loadView("/view/UsuarioPanelView.fxml");
        setActiveNavButton(null);
    }

    @FXML
    private void onFichajes() {
        loadView("/view/FichajesView.fxml");
        setActiveNavButton(null);
    }

    private void onLogout() {
        try {
            SessionManager.getInstance().clearSession();

            URL fxml = getClass().getResource("/view/LoginView.fxml");
            if (fxml == null) {
                throw new IOException("No se encuentra /view/LoginView.fxml en el classpath");
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent loginRoot = loader.load();
            Stage stage = (Stage) root.getScene().getWindow();
            double width = stage.getScene() != null ? stage.getScene().getWidth() : 1024;
            double height = stage.getScene() != null ? stage.getScene().getHeight() : 720;
            Scene scene = new Scene(loginRoot, width, height);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setTitle("GearMind — Acceso");
            stage.setScene(scene);
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Cerrar sesión");
            a.setHeaderText("No se pudo volver al login");
            a.setContentText(ex.getMessage());
            a.showAndWait();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException("No se ha encontrado la vista: " + fxmlPath);
            }

            Parent view = FXMLLoader.load(url);
            contentPane.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveNavButton(Button activeButton) {
        List<Button> buttons = List.of(btnNavDashboard, btnNavReportes, btnNavCitas, btnNavReparaciones, btnNavPresupuestos, btnNavFacturas, btnNavTareas, btnNavClientes, btnNavVehiculos, btnNavProductos, btnNavUsuarios, btnNavEmpresas);

        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("tfx-nav-active");
            }
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("tfx-nav-active")) {
            activeButton.getStyleClass().add("tfx-nav-active");
        }
    }

    @FXML
    private void onToggleSidebar() {
        if (root.getLeft() == null) {
            root.setLeft(savedSidebar);
        } else {
            root.setLeft(null);
        }
    }

    @FXML
    private void onNavDashboard() {
        loadView("/view/DashboardView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    @FXML
    private void onNavReportes() {
        loadView("/view/ReportesView.fxml");
        setActiveNavButton(btnNavReportes);
    }

    @FXML
    private void onNavCitas() {
        loadView("/view/CitasView.fxml");
        setActiveNavButton(btnNavCitas);
    }

    @FXML
    private void onNavReparaciones() {
        loadView("/view/ReparacionesView.fxml");
        setActiveNavButton(btnNavReparaciones);
    }

    @FXML
    private void onNavPresupuestos() {
        loadView("/view/PresupuestosView.fxml");
        setActiveNavButton(btnNavPresupuestos);
    }

    @FXML
    private void onNavFacturas() {
        loadView("/view/FacturasView.fxml");
        setActiveNavButton(btnNavFacturas);
    }

    @FXML
    private void onNavTareas() {
        loadView("/view/TareasView.fxml");
        setActiveNavButton(btnNavTareas);
    }

    @FXML
    private void onNavClientes() {
        loadView("/view/ClientesView.fxml");
        setActiveNavButton(btnNavClientes);
    }

    @FXML
    private void onNavVehiculos() {
        loadView("/view/VehiculosView.fxml");
        setActiveNavButton(btnNavVehiculos);
    }

    @FXML
    private void onNavProductos() {
        loadView("/view/ProductosView.fxml");
        setActiveNavButton(btnNavProductos);
    }

    @FXML
    private void onNavUsuarios() {
        loadView("/view/UsuariosView.fxml");
        setActiveNavButton(btnNavUsuarios);
    }

    @FXML
    private void onNavEmpresas() {
        loadView("/view/EmpresasView.fxml");
        setActiveNavButton(btnNavEmpresas);
    }
}
