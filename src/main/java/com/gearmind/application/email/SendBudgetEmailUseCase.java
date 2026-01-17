package com.gearmind.application.email;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.budget.BudgetRepository;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.email.EmailAttachment;
import com.gearmind.domain.email.EmailMessage;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.budget.BudgetPdfGenerator;
import com.gearmind.infrastructure.budget.BudgetPdfStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SendBudgetEmailUseCase {

    private final BudgetRepository budgetRepository;
    private final EmpresaRepository empresaRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BudgetPdfGenerator pdfGenerator;
    private final EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase;

    public SendBudgetEmailUseCase(BudgetRepository budgetRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, BudgetPdfGenerator pdfGenerator, EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase) {
        this.budgetRepository = budgetRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.enviarEmailEmpresaUseCase = enviarEmailEmpresaUseCase;
    }

    public void execute(SendBudgetEmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de envío de presupuesto no puede ser nula.");
        }
        Budget budget = budgetRepository.findById(request.budgetId()).orElseThrow(() -> new IllegalArgumentException("No se encontró el presupuesto indicado."));
        Customer customer = customerRepository.findById(budget.getClienteId()).orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente del presupuesto."));
        Empresa empresa = empresaRepository.findById(budget.getEmpresaId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(budget.getVehiculoId()).orElse(null);

        String recipient = safeTrim(request.recipientEmail());
        if (recipient.isBlank()) {
            recipient = safeTrim(customer.getEmail());
        }
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene email para enviar el presupuesto.");
        }

        List<BudgetLine> lines = budgetRepository.findLinesByBudgetId(budget.getId());
        Path pdfPath = ensurePdfExists(budget, lines, empresa, customer, vehicle);
        String subject = "Presupuesto " + budget.getId() + " - " + (empresa != null ? empresa.getNombre() : "GearMind");
        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(customer.getNombre()).append(",\n\n");
        body.append("Adjuntamos el presupuesto solicitado para su aprobación.");
        if (request.message() != null && !request.message().isBlank()) {
            body.append("\n\nMensaje adicional:\n").append(request.message().trim());
        }
        body.append("\n\nGracias por confiar en nosotros.\n");

        EmailAttachment attachment = new EmailAttachment(pdfPath.getFileName().toString(), pdfPath, "application/pdf");
        EmailMessage message = new EmailMessage(recipient, subject, body.toString(), false, List.of(attachment));
        enviarEmailEmpresaUseCase.execute(budget.getEmpresaId(), message);
    }

    private Path ensurePdfExists(Budget budget, List<BudgetLine> lines, Empresa empresa, Customer customer, Vehicle vehicle) {
        Path pdfPath = BudgetPdfStorage.resolvePath(budget.getId());
        if (Files.exists(pdfPath)) {
            return pdfPath;
        }
        return pdfGenerator.generate(budget, lines, empresa, customer, vehicle);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
