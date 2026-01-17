package com.gearmind.application.email;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.email.EmailAttachment;
import com.gearmind.domain.email.EmailMessage;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceLine;
import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.invoice.InvoicePdfGenerator;
import com.gearmind.infrastructure.invoice.InvoicePdfStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SendInvoiceEmailUseCase {

    private final InvoiceRepository invoiceRepository;
    private final EmpresaRepository empresaRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase;

    public SendInvoiceEmailUseCase(InvoiceRepository invoiceRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, InvoicePdfGenerator pdfGenerator, EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase) {
        this.invoiceRepository = invoiceRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.enviarEmailEmpresaUseCase = enviarEmailEmpresaUseCase;
    }

    public void execute(SendInvoiceEmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de envío de factura no puede ser nula.");
        }
        Invoice invoice = invoiceRepository.findById(request.invoiceId()).orElseThrow(() -> new IllegalArgumentException("No se encontró la factura indicada."));
        Customer customer = customerRepository.findById(invoice.getClienteId()).orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente de la factura."));
        Empresa empresa = empresaRepository.findById(invoice.getEmpresaId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(invoice.getVehiculoId()).orElse(null);

        String recipient = safeTrim(request.recipientEmail());
        if (recipient.isBlank()) {
            recipient = safeTrim(customer.getEmail());
        }
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene email para enviar la factura.");
        }

        List<InvoiceLine> lines = invoiceRepository.findLinesByInvoiceId(invoice.getId());
        Path pdfPath = ensurePdfExists(invoice, lines, empresa, customer, vehicle);
        String subject = "Factura #" + invoice.getNumero() + " - " + (empresa != null ? empresa.getNombre() : "GearMind");
        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(customer.getNombre()).append(",\n\n");
        body.append("Adjuntamos la factura en formato PDF correspondiente a los servicios prestados.");
        if (vehicle != null) {
            body.append("\nVehículo: ").append(vehicle.getMarca()).append(" ").append(vehicle.getModelo());
            if (vehicle.getMatricula() != null && !vehicle.getMatricula().isBlank()) {
                body.append(" (").append(vehicle.getMatricula()).append(")");
            }
            body.append(".");
        }
        if (request.message() != null && !request.message().isBlank()) {
            body.append("\n\nMensaje adicional:\n").append(request.message().trim());
        }
        body.append("\n\nGracias por confiar en nosotros.\n");

        EmailAttachment attachment = new EmailAttachment(pdfPath.getFileName().toString(), pdfPath, "application/pdf");
        EmailMessage message = new EmailMessage(recipient, subject, body.toString(), false, List.of(attachment));
        enviarEmailEmpresaUseCase.execute(invoice.getEmpresaId(), message);
    }

    private Path ensurePdfExists(Invoice invoice, List<InvoiceLine> lines, Empresa empresa, Customer customer, Vehicle vehicle) {
        Path pdfPath = InvoicePdfStorage.resolvePath(invoice.getId());
        if (Files.exists(pdfPath)) {
            return pdfPath;
        }
        return pdfGenerator.generate(invoice, lines, empresa, customer, vehicle);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
