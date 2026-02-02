# ⚙️ GearMind
> **Proyecto de Fin de Grado (TFG)**  
> *Una solución robusta para la gestión inteligente de talleres mecánicos.*

---

## 📌 ¿Qué es GearMind?

**GearMind** es una aplicación de escritorio de alto rendimiento diseñada para la **gestión integral de talleres**, optimizando tanto la operativa interna como la experiencia del cliente. A diferencia de las soluciones SaaS, GearMind apuesta por un **entorno local**, garantizando la soberanía de los datos y la privacidad del negocio.

### Capacidades principales:
* 📦 **Gestión de Activos:** Control total de clientes y vehículos.
* 🛠️ **Ciclo de Taller:** Citas, reparaciones y seguimiento de tareas.
* 💰 **Administración:** Presupuestos, facturación y pagos (contado/plazos).
* 📈 **Análisis:** Reportes detallados y documentación técnica en PDF.
* 🤖 **Omnicanalidad:** Comunicación automatizada vía **Bot de Telegram**.

---

## 🎯 Objetivos del Proyecto

* **Ingeniería de Calidad:** Implementar una solución realista basada en buenas prácticas de software.
* **Arquitectura Limpia:** Separación estricta de responsabilidades (Clean Architecture).
* **Escalabilidad:** Código mantenible y desacoplado, preparado para futuras expansiones.
* **Innovación:** Integración de canales modernos para mejorar el engagement con el cliente.

---

## 🧱 Stack Tecnológico

| Capa | Tecnología |
| :--- | :--- |
| **Lenguaje** | **Java 21** (LTS) |
| **Interfaz (GUI)** | **JavaFX 21** (FXML + CSS Personalizado) |
| **Arquitectura** | **Clean Architecture** (Domain-driven) |
| **Persistencia** | **MySQL 8** + JDBC + HikariCP |
| **Migraciones** | **Flyway** |
| **Seguridad** | **BCrypt** (Spring Security Crypto) |
| **Build Tool** | **Maven** |
| **Testing** | **JUnit 5** |

---

## 🏗️ Arquitectura y Estructura

GearMind se rige por los principios de **Clean Architecture**, asegurando que el dominio sea el núcleo del sistema, libre de dependencias de frameworks o agentes externos.

### Estructura de directorios:
```bash
GearMind/
├─ src/main/java/com/gearmind/
│  ├─ domain/          # 🧩 Entidades y Contratos (Core)
│  ├─ application/     # ⚙️ Casos de Uso (Lógica de Negocio)
│  ├─ infrastructure/  # 🛠️ Persistencia, API Telegram, Seguridad
│  ├─ presentation/    # 🖥️ JavaFX (Controladores y Vistas)
│  └─ config/          # ⚙️ Inyección de Dependencias
├─ src/main/resources/
│  ├─ view/            # Archivos FXML
│  ├─ styles/          # Temas CSS (Dark Mode)
│  └─ db/migration/    # Scripts SQL de Flyway
└─ test/               # 🧪 Tests Unitarios (JUnit 5)
```

---

## ✨ Funcionalidades Destacadas

### 💻 Interfaz de Usuario
* **Dashboard:** Panel visual con el resumen del estado actual del taller.
* **UX Moderna:** Interfaz con tema oscuro y componentes reutilizables.
* **Gestión Documental:** Generación de facturas y reportes profesionales exportables.

### 🤖 Bot de Telegram (Portal del Cliente)
Un canal directo donde los clientes pueden:
* 📅 Solicitar citas de forma automática.
* 🔍 Consultar el estado de su reparación.
* 📄 Ver facturas recientes y próximas citas programadas.

---

## 🚀 Instalación y Ejecución

### Requisitos Previos
* Java 21 (OpenJDK / Temurin recomendado).
* MySQL Server 8.0 o superior.
* Apache Maven 3.9+.

### Pasos para ejecutar

1. **Clonar el repositorio:**
```bash
git clone https://github.com/tu-usuario/gearmind.git
```

2. **Configurar la Base de Datos:** Crea una base de datos llamada `gearmind`. Las tablas se crearán automáticamente al iniciar gracias a Flyway.

3. **Ejecución vía Terminal:**
```bash
mvn clean javafx:run
```

---

## 🛡️ Seguridad y Calidad

* **Protección de Datos:** Contraseñas cifradas mediante algoritmos de hash BCrypt.
* **Roles de Acceso:** Diferenciación clara entre privilegios de Administrador y Empleado.
* **Robustez:** Validación estricta de datos en la capa de aplicación antes de la persistencia.

---

## 📘 Licencia y Autoría

Este proyecto ha sido desarrollado como Trabajo Fin de Grado (TFG).

**Autor:** Mario Rodríguez Gómez  
**Uso:** Académico y educativo.