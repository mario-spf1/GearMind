
# ⚙️ GearMind

**Gestión integral de talleres mecánicos** desarrollada en **Java 21 + JavaFX**, siguiendo principios de **Clean Architecture** y con un diseño moderno, modular y mantenible.

---

## 🧱 Stack Tecnológico

| Capa | Tecnologías |
|------|--------------|
| **Frontend (UI)** | JavaFX 21 · CSS personalizado · FXML |
| **Aplicación / Dominio** | Java 21 · Clean Architecture (Domain → Application → Infrastructure → Presentation) |
| **Persistencia** | MySQL (pendiente de integración) · JDBC + HikariCP · Flyway |
| **Autenticación** | BCrypt (Spring Security Crypto) |
| **Build / Tooling** | Maven · NetBeans 23 · OpenJDK 21 (Temurin) |

---

## 🚀 Estructura del Proyecto

GearMind/
├─ pom.xml
├─ src/
│ ├─ main/
│ │ ├─ java/com/gearmind/
│ │ │ ├─ application/ → Casos de uso (lógica de aplicación)
│ │ │ ├─ domain/ → Modelos y contratos del dominio
│ │ │ ├─ infrastructure/ → Repositorios, seguridad y persistencia
│ │ │ ├─ presentation/ → Controladores y vistas JavaFX
│ │ │ └─ config/ → Wiring (AppConfig, inyección de dependencias)
│ │ └─ resources/
│ │ ├─ view/ → Archivos FXML (vistas)
│ │ └─ styles/ → Estilos CSS reutilizables
│ └─ test/ → Tests (JUnit 5)
└─ .gitignore

yaml
Copiar código

---

## 🖥️ Requisitos Previos

- **Java 21** (OpenJDK o Temurin)
- **Apache Maven 3.9+**
- **NetBeans 23** o cualquier IDE compatible con Maven
- **Git** (para control de versiones)

---

## ▶️ Ejecución del Proyecto

En NetBeans:

> **Run Project** → selecciona la clase principal  
> `com.gearmind.presentation.App`

O desde terminal:

```bash
mvn clean javafx:run
🎨 Estado actual
✅ Pantalla inicial (Home) con diseño responsive y tema oscuro
🧩 Sistema de estilos modular (theme.css + components.css)
🚧 Próximo paso: módulo de autenticación (Login + MainShell)

📘 Licencia
Este proyecto forma parte del Trabajo Fin de Grado (TFG) de Mario Rodríguez Gómez.
Uso académico y educativo permitido.

© 2025 Mario Rodríguez Gómez · Todos los derechos reservados.