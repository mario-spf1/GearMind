package com.gearmind.infrastructure.database;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;

import java.io.Console;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;
import javax.sql.DataSource;

public final class FirstSetupMain {

    private FirstSetupMain() {
    }

    public static void main(String[] args) throws Exception {
        DataSource ds = DataSourceFactory.getDataSource();

        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM empresa")) {
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("La base de datos ya tiene datos. Saltando configuracion inicial.");
                return;
            }
        }

        Console console = System.console();
        Scanner scanner = (console == null) ? new Scanner(System.in) : null;

        System.out.println();
        System.out.println("=== Configuracion del primer administrador ===");
        System.out.println("Estos datos se podran cambiar desde la aplicacion.");
        System.out.println();

        String nombreEmpresa = leer(console, scanner, "Nombre del taller o empresa: ");
        String nombreAdmin   = leer(console, scanner, "Nombre del administrador:    ");
        String emailAdmin    = leer(console, scanner, "Email del administrador:     ");

        String password;
        while (true) {
            password = leerPassword(console, scanner, "Contrasena (minimo 8 caracteres): ");
            if (password.length() < 8) {
                System.out.println("  La contrasena debe tener al menos 8 caracteres.");
                continue;
            }
            String confirm = leerPassword(console, scanner, "Confirma la contrasena:           ");
            if (password.equals(confirm)) break;
            System.out.println("  Las contrasenas no coinciden. Intentalo de nuevo.");
            System.out.println();
        }

        MySqlEmpresaRepository empresaRepo = new MySqlEmpresaRepository();
        Empresa empresa = empresaRepo.save(
                new Empresa(0L, nombreEmpresa.trim(), "", "", "", "", "", "", "", true));

        MySqlUserRepository userRepo = new MySqlUserRepository();
        BCryptPasswordHasher hasher = new BCryptPasswordHasher();
        String hash = hasher.hash(password.trim());
        userRepo.create(empresa.getId(), nombreAdmin.trim(),
                emailAdmin.trim().toLowerCase(), hash, UserRole.ADMIN, true);

        System.out.println();
        System.out.println("Administrador creado correctamente.");
        System.out.println("Accede a GearMind con: " + emailAdmin.trim().toLowerCase());
        System.out.println();
    }

    private static String leer(Console c, Scanner s, String msg) {
        System.out.print(msg);
        System.out.flush();
        String value = c != null ? c.readLine() : s.nextLine();
        return value != null ? value : "";
    }

    private static String leerPassword(Console c, Scanner s, String msg) {
        System.out.print(msg);
        System.out.flush();
        if (c != null) {
            char[] pwd = c.readPassword();
            return pwd != null ? new String(pwd) : "";
        }
        return s.nextLine();
    }
}
