package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.message.ListConversationsUseCase;
import com.gearmind.application.message.ListMessagesUseCase;
import com.gearmind.application.message.SendMessageUseCase;
import com.gearmind.application.product.ListLowStockProductsUseCase;
import com.gearmind.domain.message.ConversationSummary;
import com.gearmind.domain.message.InternalMessage;
import com.gearmind.domain.message.MessageRepository;
import com.gearmind.domain.product.Product;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.domain.user.UserRepository;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.message.MySqlMessageRepository;
import com.gearmind.infrastructure.product.MySqlProductRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.Region;
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
    private Button btnAlertas;
    @FXML
    private Label lblAlertas;
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
    private Button btnNavPagos;
    @FXML
    private Button btnNavTareas;
    @FXML
    private Button btnNavAjustes;

    private javafx.scene.Node savedSidebar;
    private ContextMenu userMenu;
    private ContextMenu messagesMenu;
    private ContextMenu alertsMenu;
    private ListView<ConversationSummary> conversationList;
    private ListView<InternalMessage> messageList;
    private ListView<Product> alertsList;
    private TextField messageInput;
    private Button btnSendMessage;
    private ComboBox<UserOption> cmbNewConversation;
    private ConversationSummary activeConversation;
    private final MessageRepository messageRepository = new MySqlMessageRepository();
    private final ListConversationsUseCase listConversationsUseCase = new ListConversationsUseCase(messageRepository);
    private final ListMessagesUseCase listMessagesUseCase = new ListMessagesUseCase(messageRepository);
    private final SendMessageUseCase sendMessageUseCase = new SendMessageUseCase(messageRepository);
    private final UserRepository userRepository = new MySqlUserRepository();
    private final ListLowStockProductsUseCase listLowStockProductsUseCase = new ListLowStockProductsUseCase(new MySqlProductRepository());

    @FXML
    public void initialize() {
        savedSidebar = sidebar;
        setupFromAuthContext();
        initUserMenu();
        initMessagesMenu();
        initAlertasMenu();

        if (userBox != null) {
            userBox.setOnMouseClicked(e -> showUserMenu());
        }

        if (lblUsuarioActual != null) {
            lblUsuarioActual.setOnMouseClicked(e -> showUserMenu());
        }

        if (btnAlertas != null) {
            btnAlertas.setDisable(!AuthContext.isLoggedIn());
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
            hideButton(btnNavAjustes);
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
            hideButton(btnNavAjustes);
        }
    }

    private void setAllSidebarButtonsVisible(boolean visible) {
        for (Button b : List.of(btnNavDashboard, btnNavReportes, btnNavCitas, btnNavReparaciones, btnNavPresupuestos, btnNavFacturas, btnNavPagos, btnNavTareas, btnNavClientes, btnNavVehiculos, btnNavProductos, btnNavUsuarios, btnNavEmpresas, btnNavAjustes)) {
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
        userMenu.getStyleClass().add("tfx-user-menu");
        userMenu.setAutoHide(true);
    }

    private void initMessagesMenu() {
        if (btnMensajes == null) {
            return;
        }

        cmbNewConversation = new ComboBox<>();
        cmbNewConversation.getStyleClass().add("tfx-chat-combo");
        cmbNewConversation.setPromptText("Nuevo chat...");
        cmbNewConversation.setMaxWidth(Double.MAX_VALUE);
        cmbNewConversation.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(UserOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });
        cmbNewConversation.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(UserOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });
        cmbNewConversation.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            activeConversation = new ConversationSummary(newValue.id, newValue.nombre, null, null);
            conversationList.getSelectionModel().clearSelection();
            loadMessages(activeConversation);
        });

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
        VBox conversationPane = new VBox(8, cmbNewConversation, conversationList);
        VBox.setVgrow(conversationList, Priority.ALWAYS);
        HBox content = new HBox(12, conversationPane, chatPane);
        content.getStyleClass().add("tfx-chat-popup");
        content.setMinWidth(720);
        content.setPrefWidth(720);
        HBox.setHgrow(chatPane, Priority.ALWAYS);
        CustomMenuItem item = new CustomMenuItem(content, false);
        item.setHideOnClick(false);
        messagesMenu = new ContextMenu(item);
        messagesMenu.setAutoHide(false);

        conversationList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            activeConversation = newValue;
            loadMessages(newValue);
        });

        setChatEnabled(false);
        Tooltip.install(btnMensajes, new Tooltip("Mensajería interna"));
        btnMensajes.setDisable(!AuthContext.isLoggedIn());
    }

    private void initAlertasMenu() {
        if (btnAlertas == null) {
            return;
        }

        alertsList = new ListView<>();
        alertsList.setPlaceholder(new Label("Sin alertas de stock."));
        alertsList.setPrefWidth(380);
        alertsList.getStyleClass().add("tfx-chat-list");
        alertsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String nombre = safe(item.getNombre());
                Label stock = new Label(formatStock(item));
                Label name = new Label(nombre);
                stock.getStyleClass().add("tfx-warn");
                HBox row = new HBox(8, name, new Region(), stock);
                HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
                setGraphic(row);
                setText(null);
            }
        });

        VBox content = new VBox(8, new Label("Productos con stock bajo"), alertsList);
        content.getStyleClass().add("tfx-chat-popup");
        content.setMinWidth(420);
        content.setPrefWidth(420);
        CustomMenuItem item = new CustomMenuItem(content, false);
        item.setHideOnClick(false);
        alertsMenu = new ContextMenu(item);
        alertsMenu.setAutoHide(false);
        Tooltip.install(btnAlertas, new Tooltip("Alertas de stock"));
        btnAlertas.setDisable(!AuthContext.isLoggedIn());
        refreshAlertBadge();
    }

    @FXML
    private void onUserMenu() {
        showUserMenu();
    }

    @FXML
    private void onMessagesMenu() {
        showMessagesMenu();
    }

    @FXML
    private void onAlertasMenu() {
        showAlertasMenu();
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

    private void showAlertasMenu() {
        if (alertsMenu == null || btnAlertas == null) {
            return;
        }
        if (alertsMenu.isShowing()) {
            alertsMenu.hide();
            return;
        }
        refreshStockAlerts();
        alertsMenu.show(btnAlertas, Side.BOTTOM, 0, 6);
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
            cmbNewConversation.setItems(loadAvailableUsers(empresaId, userId));

            if (activeConversation != null) {
                conversations.stream().filter(c -> c.getContactoId() == activeConversation.getContactoId()).findFirst().ifPresentOrElse(conversationList.getSelectionModel()::select, () -> {
                    UserOption option = cmbNewConversation.getItems().stream().filter(user -> user.id == activeConversation.getContactoId()).findFirst().orElse(null);
                    cmbNewConversation.getSelectionModel().select(option);
                });
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

    private void refreshStockAlerts() {
        if (!AuthContext.isLoggedIn() || alertsList == null) {
            return;
        }
        List<Product> lowStock = AuthContext.isSuperAdmin() ? listLowStockProductsUseCase.listAllWithEmpresa() : listLowStockProductsUseCase.listByEmpresa(AuthContext.getEmpresaId());
        alertsList.setItems(FXCollections.observableArrayList(lowStock));
        refreshAlertBadge();
    }

    private void refreshAlertBadge() {
        if (lblAlertas == null) {
            return;
        }
        int count = 0;
        if (AuthContext.isLoggedIn()) {
            List<Product> lowStock = AuthContext.isSuperAdmin() ? listLowStockProductsUseCase.listAllWithEmpresa() : listLowStockProductsUseCase.listByEmpresa(AuthContext.getEmpresaId());
            count = lowStock.size();
        }
        lblAlertas.setText(String.valueOf(count));
        lblAlertas.setVisible(count > 0);
        lblAlertas.setManaged(count > 0);
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

    private ObservableList<UserOption> loadAvailableUsers(long empresaId, long userId) {
        List<UserOption> options = userRepository.findByEmpresaId(empresaId).stream().filter(User::isActivo).filter(user -> user.getId() != userId).map(user -> new UserOption(user.getId(), user.getNombre())).toList();
        return FXCollections.observableArrayList(options);
    }

    private String formatStock(Product product) {
        int stock = product.getStock() == null ? 0 : product.getStock();
        return String.valueOf(stock);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class UserOption {

        private final long id;
        private final String nombre;

        private UserOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
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
        List<Button> buttons = List.of(btnNavDashboard, btnNavReportes, btnNavCitas, btnNavReparaciones, btnNavPresupuestos, btnNavFacturas, btnNavPagos, btnNavTareas, btnNavClientes, btnNavVehiculos, btnNavProductos, btnNavUsuarios, btnNavEmpresas, btnNavAjustes);
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
    private void onNavPagos() {
        loadView("/view/PagosView.fxml");
        setActiveNavButton(btnNavPagos);
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

    @FXML
    private void onNavAjustes() {
        if (!AuthContext.isLoggedIn() || !AuthContext.isAdminOrSuperAdmin()) {
            return;
        }
        loadView("/view/AjustesView.fxml");
        setActiveNavButton(btnNavAjustes);
    }
}
