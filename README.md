# ⚙️ GearMind

Proyecto realizado como **Trabajo Fin de Grado (TFG)**.

---

## 📌 ¿Qué es GearMind?

**GearMind** es una aplicación de escritorio diseñada para la **gestión integral de talleres mecánicos**, cubriendo tanto la operativa interna del taller como la interacción con los clientes.

Permite centralizar y organizar:

- Clientes y vehículos  
- Citas y reparaciones  
- Presupuestos, facturación y pagos  
- Reportes y documentación  
- Comunicación con clientes mediante **bot de Telegram**

La aplicación funciona en **entorno local**, sin depender de servicios SaaS externos, garantizando el control total de los datos por parte del taller.

---

## 🎯 Objetivos del proyecto

- Desarrollar una solución integral y realista para la gestión de talleres mecánicos.
- Aplicar buenas prácticas de ingeniería del software.
- Implementar una arquitectura limpia, escalable y mantenible.
- Separar claramente dominio, lógica de negocio, infraestructura y presentación.
- Integrar canales modernos de comunicación con el cliente.

---

## 🧱 Stack Tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Lenguaje** | Java 21 |
| **Interfaz de usuario** | JavaFX 21 · FXML · CSS personalizado |
| **Arquitectura** | Clean Architecture (Domain → Application → Infrastructure → Presentation) |
| **Persistencia** | MySQL 8 · JDBC · HikariCP · Flyway |
| **Seguridad** | BCrypt (Spring Security Crypto) |
| **Comunicación** | Bot de Telegram |
| **Build & Tooling** | Maven · NetBeans 23 · OpenJDK 21 (Temurin) |
| **Testing** | JUnit 5 |

---

## 🏗️ Arquitectura

GearMind sigue una **Clean Architecture estricta**, lo que garantiza:

- Dominio independiente de frameworks.
- Casos de uso aislados y testeables.
- Infraestructura desacoplada.
- Interfaz JavaFX como capa de presentación.

Domain → Application → Infrastructure → Presentation

---

## 🚀 Estructura del Proyecto

```bash
GearMind/
├─ pom.xml
├─ README.md
├─ src/
│  ├─ main/
│  │  ├─ java/com/gearmind/
│  │  │  ├─ domain/          # Entidades y contratos del dominio
│  │  │  ├─ application/     # Casos de uso y lógica de aplicación
│  │  │  ├─ infrastructure/  # Persistencia, seguridad y servicios externos
│  │  │  ├─ presentation/    # JavaFX (controladores y vistas)
│  │  │  └─ config/          # Configuración e inyección de dependencias
│  │  └─ resources/
│  │     ├─ view/            # Archivos FXML
│  │     ├─ styles/          # CSS (tema y componentes)
│  │     └─ db/migration/    # Migraciones Flyway
│  └─ test/
│     └─ java/               # Tests unitarios (JUnit 5)
└─ .gitignore

```


✨ Funcionalidades
Funcionalidades implementadas

🏠 Panel de control con resumen del estado del taller.

👤 Gestión de clientes.

🚗 Gestión de vehículos.

📅 Gestión de citas.

🔧 Gestión de reparaciones y tareas.

💶 Presupuestos, facturas y pagos (contado y a plazos).

📊 Reportes exportables a PDF.

🤖 Bot de Telegram para clientes:

Solicitud de citas.

Consulta de próximas citas.

Estado de reparaciones.

Facturas recientes.

🎨 Interfaz moderna con tema oscuro y estilos reutilizables.

🖥️ Requisitos Previos

Java 21 (OpenJDK / Temurin recomendado)

Apache Maven 3.9 o superior

MySQL Server 8.0 o superior

NetBeans 23 (o cualquier IDE compatible con Maven)

Git

▶️ Ejecución del Proyecto
Desde NetBeans

Abrir el proyecto como Maven Project.

Ejecutar la clase principal:

com.gearmind.presentation.App

Desde terminal
mvn clean javafx:run

⚙️ Configuración de Base de Datos

Crear una base de datos MySQL (por ejemplo gearmind).

Configurar las credenciales en el archivo de configuración correspondiente.

Las migraciones se ejecutan automáticamente mediante Flyway al iniciar la aplicación.

🧪 Testing

Ejecución de tests unitarios:

mvn test


Los tests se centran en la lógica de negocio y los casos de uso, independientes de la interfaz gráfica.

🔒 Seguridad

Contraseñas cifradas con BCrypt.

Separación de roles (administrador y empleado).

Validación de datos en la capa de aplicación.

Control de acceso a funcionalidades según permisos.

📘 Licencia

Proyecto desarrollado como Trabajo Fin de Grado (TFG).

Autor:
Mario Rodríguez Gómez

Uso académico y educativo.
