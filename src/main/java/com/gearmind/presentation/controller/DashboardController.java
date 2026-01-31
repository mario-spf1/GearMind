package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.appointment.ListAppointmentsUseCase;
import com.gearmind.application.customer.ListCustomersUseCase;
import com.gearmind.application.invoice.ListInvoicesUseCase;
import com.gearmind.application.repair.ListRepairsUseCase;
import com.gearmind.application.vehicle.ListVehiclesUseCase;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.infrastructure.appointment.MySqlAppointmentRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.invoice.MySqlInvoiceRepository;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
import com.gearmind.infrastructure.task.MySqlTaskRepository;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML
    private Label lblEmpresaTitulo;
    @FXML
    private Label lblTotalClientes;
    @FXML
    private Label lblTotalVehiculos;
    @FXML
    private Label lblReparacionesAbiertas;
    @FXML
    private Label lblTareasPendientes;

    @FXML
    private TableView<Customer> tblUltimosClientes;
    @FXML
    private TableColumn<Customer, String> colCliNombre;
    @FXML
    private TableColumn<Customer, String> colCliDni;
    @FXML
    private TableColumn<Customer, String> colCliTelefono;
    @FXML
    private TableColumn<Customer, String> colCliEmail;
    @FXML
    private Label lblUltimosClientesInfo;

    @FXML
    private Label lblUsuarioInfo;
    @FXML
    private Label lblRolInfo;
    @FXML
    private Label lblEmpresaInfo;
    @FXML
    private Label lblCitasHoy;
    @FXML
    private Label lblReparacionesActivas;
    @FXML
    private Label lblTareasCriticas;
    @FXML
    private Label lblAgendaHint;
    @FXML
    private Label lblReparacionesCola;
    @FXML
    private Label lblPendientesCobro;
    @FXML
    private Label lblCitasConfirmadas;
    @FXML
    private Label lblSatisfaccionEstimada;
    @FXML
    private PieChart chartTareasEstado;
    @FXML
    private BarChart<String, Number> chartCitasEstado;

    private final ListCustomersUseCase listCustomersUseCase;
    private final ListVehiclesUseCase listVehiclesUseCase;
    private final ListRepairsUseCase listRepairsUseCase;
    private final ListAppointmentsUseCase listAppointmentsUseCase;
    private final ListInvoicesUseCase listInvoicesUseCase;
    private final MySqlTaskRepository taskRepository;

    public DashboardController() {
        this.listCustomersUseCase = new ListCustomersUseCase(new MySqlCustomerRepository());
        this.listVehiclesUseCase = new ListVehiclesUseCase(new MySqlVehicleRepository());
        this.listRepairsUseCase = new ListRepairsUseCase(new MySqlRepairRepository());
        this.listAppointmentsUseCase = new ListAppointmentsUseCase(new MySqlAppointmentRepository());
        this.listInvoicesUseCase = new ListInvoicesUseCase(new MySqlInvoiceRepository());
        this.taskRepository = new MySqlTaskRepository();
    }

    @FXML
    public void initialize() {
        setupSessionSummary();
        setupClientesTable();
        loadClientesData();
        loadDashboardStats();
    }

    private void setupSessionSummary() {
        if (!AuthContext.isLoggedIn()) {
            if (lblEmpresaTitulo != null) {
                lblEmpresaTitulo.setText("Resumen del taller");
            }
            if (lblUsuarioInfo != null) {
                lblUsuarioInfo.setText("No hay usuario en sesión");
            }
            return;
        }

        User user = AuthContext.getCurrentUser();
        UserRole role = AuthContext.getRole();
        String empresaNombre = AuthContext.getEmpresaNombre();

        if (lblEmpresaTitulo != null) {
            if (empresaNombre != null && !empresaNombre.isBlank()) {
                lblEmpresaTitulo.setText("Resumen de " + empresaNombre);
            } else {
                lblEmpresaTitulo.setText("Resumen del taller");
            }
        }

        if (lblUsuarioInfo != null) {
            lblUsuarioInfo.setText("Usuario: " + user.getNombre() + " (" + user.getEmail() + ")");
        }

        if (lblRolInfo != null) {
            String rolTexto = switch (role) {
                case SUPER_ADMIN ->
                    "Super administrador";
                case ADMIN ->
                    "Administrador";
                case EMPLEADO ->
                    "Empleado";
            };
            lblRolInfo.setText("Rol: " + rolTexto);
        }

        if (lblEmpresaInfo != null) {
            if (empresaNombre != null && !empresaNombre.isBlank()) {
                lblEmpresaInfo.setText("Empresa: " + empresaNombre + " (ID " + AuthContext.getEmpresaId() + ")");
            } else {
                lblEmpresaInfo.setText("Empresa asociada no definida");
            }
        }
    }

    private void setupClientesTable() {
        if (tblUltimosClientes == null) {
            return;
        }

        colCliNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCliDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colCliTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCliEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        tblUltimosClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblUltimosClientes.setFixedCellSize(28);
    }

    private void loadClientesData() {
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            List<Customer> all = listCustomersUseCase.listByEmpresa(empresaId);
            if (lblTotalClientes != null) {
                lblTotalClientes.setText(String.valueOf(all.size()));
            }
            List<Customer> ultimos = all.stream().sorted(Comparator.comparing(Customer::getNombre, String.CASE_INSENSITIVE_ORDER)).limit(10).collect(Collectors.toList());

            if (tblUltimosClientes != null) {
                tblUltimosClientes.setItems(FXCollections.observableArrayList(ultimos));
                int rows = Math.max(1, ultimos.size());
                double headerHeight = 28;
                double tableHeight = headerHeight + rows * tblUltimosClientes.getFixedCellSize() + 2;
                tblUltimosClientes.setPrefHeight(tableHeight);
            }

            if (lblUltimosClientesInfo != null) {
                if (all.isEmpty()) {
                    lblUltimosClientesInfo.setText("Todavía no hay clientes registrados.");
                } else if (all.size() <= 10) {
                    lblUltimosClientesInfo.setText("Mostrando todos los clientes (" + all.size() + ").");
                } else {
                    lblUltimosClientesInfo.setText("Mostrando los últimos " + ultimos.size()
                            + " de " + all.size() + " clientes.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (lblTotalClientes != null) {
                lblTotalClientes.setText("—");
            }
            if (lblUltimosClientesInfo != null) {
                lblUltimosClientesInfo.setText("No se pudieron cargar los clientes.");
            }
        }
    }

    private void loadDashboardStats() {
        try {
            Long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            if (empresaId == null) {
                setDashboardFallback();
                return;
            }

            List<Vehicle> vehicles = listVehiclesUseCase.execute();
            if (lblTotalVehiculos != null) {
                lblTotalVehiculos.setText(String.valueOf(vehicles.size()));
            }

            List<Repair> repairs = listRepairsUseCase.execute();
            long reparacionesActivas = repairs.stream()
                    .map(Repair::getEstado)
                    .filter(Objects::nonNull)
                    .filter(status -> status == RepairStatus.ABIERTA || status == RepairStatus.EN_PROCESO)
                    .count();
            if (lblReparacionesAbiertas != null) {
                lblReparacionesAbiertas.setText(String.valueOf(reparacionesActivas));
            }

            List<Task> tasks = taskRepository.findByEmpresa(empresaId);
            long tareasPendientes = tasks.stream()
                    .map(Task::getEstado)
                    .filter(Objects::nonNull)
                    .filter(status -> status == TaskStatus.PENDIENTE || status == TaskStatus.EN_PROCESO)
                    .count();
            if (lblTareasPendientes != null) {
                lblTareasPendientes.setText(String.valueOf(tareasPendientes));
            }

            List<Appointment> appointments = listAppointmentsUseCase.execute(empresaId);
            long citasHoy = appointments.stream()
                    .filter(a -> a.getDateTime() != null)
                    .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                    .filter(a -> a.getDateTime().toLocalDate().equals(LocalDate.now()))
                    .count();

            long tareasCriticas = tasks.stream()
                    .filter(t -> t.getPrioridad() == TaskPriority.ALTA)
                    .filter(t -> t.getEstado() == TaskStatus.PENDIENTE || t.getEstado() == TaskStatus.EN_PROCESO)
                    .count();

            List<Invoice> invoices = listInvoicesUseCase.listByEmpresa(empresaId);
            long pendientesCobro = invoices.stream()
                    .map(Invoice::getEstado)
                    .filter(Objects::nonNull)
                    .filter(status -> status == InvoiceStatus.PENDIENTE)
                    .count();

            long citasConfirmadas = appointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                    .count();

            String satisfaccionEstimada = buildSatisfaccionEstimada(tasks);
            updateTaskStatusChart(tasks);
            updateAppointmentStatusChart(appointments);

            if (lblCitasHoy != null) {
                lblCitasHoy.setText("• " + citasHoy + " citas programadas hoy");
            }
            if (lblReparacionesActivas != null) {
                lblReparacionesActivas.setText("• " + reparacionesActivas + " reparaciones activas");
            }
            if (lblTareasCriticas != null) {
                lblTareasCriticas.setText("• " + tareasCriticas + " tareas críticas");
            }

            if (lblReparacionesCola != null) {
                lblReparacionesCola.setText(String.valueOf(reparacionesActivas));
            }
            if (lblPendientesCobro != null) {
                lblPendientesCobro.setText(String.valueOf(pendientesCobro));
            }
            if (lblCitasConfirmadas != null) {
                lblCitasConfirmadas.setText(String.valueOf(citasConfirmadas));
            }
            if (lblSatisfaccionEstimada != null) {
                lblSatisfaccionEstimada.setText(satisfaccionEstimada);
            }

            if (lblAgendaHint != null) {
                if (tareasCriticas > 0) {
                    lblAgendaHint.setText("Revisa las tareas críticas pendientes.");
                } else if (citasHoy > 0) {
                    lblAgendaHint.setText("Agenda con actividad hoy.");
                } else {
                    lblAgendaHint.setText("Sin alertas críticas pendientes.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            setDashboardFallback();
        }
    }

    private void setDashboardFallback() {
        if (lblTotalVehiculos != null) {
            lblTotalVehiculos.setText("—");
        }
        if (lblReparacionesAbiertas != null) {
            lblReparacionesAbiertas.setText("—");
        }
        if (lblTareasPendientes != null) {
            lblTareasPendientes.setText("—");
        }
        if (lblCitasHoy != null) {
            lblCitasHoy.setText("• — citas programadas hoy");
        }
        if (lblReparacionesActivas != null) {
            lblReparacionesActivas.setText("• — reparaciones activas");
        }
        if (lblTareasCriticas != null) {
            lblTareasCriticas.setText("• — tareas críticas");
        }
        if (lblAgendaHint != null) {
            lblAgendaHint.setText("No se pudo cargar la agenda.");
        }
        if (lblReparacionesCola != null) {
            lblReparacionesCola.setText("—");
        }
        if (lblPendientesCobro != null) {
            lblPendientesCobro.setText("—");
        }
        if (lblCitasConfirmadas != null) {
            lblCitasConfirmadas.setText("—");
        }
        if (lblSatisfaccionEstimada != null) {
            lblSatisfaccionEstimada.setText("—");
        }
    }

    private String buildSatisfaccionEstimada(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "—";
        }
        long total = tasks.size();
        long completadas = tasks.stream().filter(t -> t.getEstado() == TaskStatus.COMPLETADA).count();
        long porcentaje = Math.round((double) completadas * 100 / total);
        return porcentaje + "%";
    }

    private void updateTaskStatusChart(List<Task> tasks) {
        if (chartTareasEstado == null) {
            return;
        }
        long pendientes = tasks.stream().filter(t -> t.getEstado() == TaskStatus.PENDIENTE).count();
        long enProceso = tasks.stream().filter(t -> t.getEstado() == TaskStatus.EN_PROCESO).count();
        long completadas = tasks.stream().filter(t -> t.getEstado() == TaskStatus.COMPLETADA).count();
        long canceladas = tasks.stream().filter(t -> t.getEstado() == TaskStatus.CANCELADA).count();

        chartTareasEstado.setData(FXCollections.observableArrayList(
                new PieChart.Data("Pendiente", pendientes),
                new PieChart.Data("En proceso", enProceso),
                new PieChart.Data("Completada", completadas),
                new PieChart.Data("Cancelada", canceladas)
        ));
        chartTareasEstado.setLegendVisible(true);
    }

    private void updateAppointmentStatusChart(List<Appointment> appointments) {
        if (chartCitasEstado == null) {
            return;
        }
        long solicitadas = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.REQUESTED).count();
        long confirmadas = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long completadas = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long canceladas = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Estado de citas");
        series.getData().add(new XYChart.Data<>("Solicitada", solicitadas));
        series.getData().add(new XYChart.Data<>("Confirmada", confirmadas));
        series.getData().add(new XYChart.Data<>("Completada", completadas));
        series.getData().add(new XYChart.Data<>("Cancelada", canceladas));

        chartCitasEstado.getData().setAll(series);
        chartCitasEstado.setLegendVisible(false);
    }
}
