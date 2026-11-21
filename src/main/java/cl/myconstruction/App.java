
package cl.myconstruction;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Locale;

import org.mindrot.jbcrypt.BCrypt;

/**
 * My Construction
 * Aplicacion web Java, Jakarta Servlet, MySQL y Maven.
 * Despliegue mediante archivo WAR en Apache Tomcat.
 */
public final class App {

    private App() {
    }

    static String env(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    // CONEXION A MYSQL CORREGIDA
    static Connection connect() throws SQLException {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException e) {

            throw new SQLException(
                "No se encontro el controlador JDBC de MySQL", e);
        }

        String url = env(
            "DB_URL",
            "jdbc:mysql://localhost:3306/myconstruction"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC"
        );

        String user = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "");

        return DriverManager.getConnection(
            url,
            user,
            password
        );
    }

    @WebServlet("/*")
    public static final class SiteServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private String getPath(HttpServletRequest req) {

            String path = req.getPathInfo();

            return path == null ? "/" : path;
        }

        @Override
        protected void doGet(
                HttpServletRequest req,
                HttpServletResponse res) throws IOException {

            String path = getPath(req);
            String context = req.getContextPath();

            // CERRAR SESION
            if ("/logout".equals(path)) {

                HttpSession session = req.getSession(false);

                if (session != null) {
                    session.invalidate();
                }

                res.sendRedirect(context + "/");
                return;
            }

            // PANEL DE ADMINISTRACION
            if ("/dashboard".equals(path)) {

                HttpSession session = req.getSession(false);

                if (session == null
                        || session.getAttribute("usuario") == null) {

                    res.sendRedirect(context + "/login");
                    return;
                }

                page(req, res, "Panel de administración",
                    """
                    <div class='panel'>

                        <span class='pill'>Panel privado</span>

                        <h1>Panel de control</h1>

                        <p>Sesión iniciada como <strong>
                    """
                    + Validation.escape(
                        (String) session.getAttribute("usuario"))
                    + """
                        </strong></p>

                        <p>
                            Bienvenido al sistema de gestión
                            de My Construction.
                        </p>

                        <section class='features'>

                            <div>
                                <h3>Proyectos</h3>
                                <p>Gestión de obras.</p>
                            </div>

                            <div>
                                <h3>Clientes</h3>
                                <p>Control de clientes.</p>
                            </div>

                            <div>
                                <h3>Ventas</h3>
                                <p>Gestión comercial.</p>
                            </div>

                        </section>
                    """
                    + "<a class='btn' href='"
                    + context
                    + "/logout'>Cerrar sesión</a></div>"
                );

                return;
            }

            // PAGINA DE LOGIN
            if ("/login".equals(path)) {

                login(req, res, "");
                return;
            }

            // RUTA NO ENCONTRADA
            if (!"/".equals(path)) {

                res.sendError(404);
                return;
            }

            // PAGINA PRINCIPAL
            page(req, res, "Inicio",
                """
                <section class='hero'>

                    <span class='pill'>
                        Construimos nuevos horizontes
                    </span>

                    <h1>
                        Construimos espacios.<br>
                        Creamos futuro.
                    </h1>

                    <p>
                        Casas, departamentos, oficinas
                        y remodelaciones para cada etapa
                        de tu vida.
                    </p>
                """
                + "<a class='btn' href='"
                + context
                + "/login'>Acceso a clientes y personal</a>"
                + """
                </section>

                <section class='features'>

                    <div>
                        <h3>Viviendas</h3>
                        <p>Proyectos inmobiliarios a medida.</p>
                    </div>

                    <div>
                        <h3>Oficinas</h3>
                        <p>Espacios profesionales y funcionales.</p>
                    </div>

                    <div>
                        <h3>Remodelaciones</h3>
                        <p>Transformamos lo existente.</p>
                    </div>

                </section>
                """
            );
        }

        // PROCESAMIENTO DEL LOGIN
        @Override
        protected void doPost(
                HttpServletRequest req,
                HttpServletResponse res) throws IOException {

            if (!"/login".equals(getPath(req))) {

                res.sendError(404);
                return;
            }

            req.setCharacterEncoding(
                StandardCharsets.UTF_8.name());

            String email = req.getParameter("email");
            String password = req.getParameter("password");

            // VALIDACION DE DATOS
            if (email == null
                    || !Validation.validEmail(email)
                    || password == null
                    || password.isBlank()) {

                login(req, res, "Credenciales inválidas.");
                return;
            }

            String sql = """
                SELECT password_hash
                FROM usuarios
                WHERE email = ?
                """;

            try (
                Connection connection = connect();

                PreparedStatement statement =
                    connection.prepareStatement(sql)
            ) {

                statement.setString(
                    1,
                    email.trim().toLowerCase(Locale.ROOT)
                );

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()
                            && BCrypt.checkpw(
                                password,
                                result.getString("password_hash"))) {

                        HttpSession session = req.getSession(true);

                        req.changeSessionId();

                        session.setAttribute(
                            "usuario",
                            email.trim().toLowerCase(Locale.ROOT)
                        );

                        session.setMaxInactiveInterval(1800);

                        res.sendRedirect(
                            req.getContextPath() + "/dashboard"
                        );

                        return;
                    }
                }

                login(
                    req,
                    res,
                    "Correo o contraseña incorrectos."
                );

            } catch (SQLException e) {

                getServletContext().log(
                    "Error de conexion a MySQL", e
                );

                res.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
                );

                login(
                    req,
                    res,
                    "Servicio temporalmente no disponible."
                );

            } catch (IllegalArgumentException e) {

                getServletContext().log(
                    "Formato de contraseña no valido", e
                );

                login(
                    req,
                    res,
                    "Credenciales inválidas."
                );
            }
        }

        // FORMULARIO DE INICIO DE SESION
        private void login(
                HttpServletRequest req,
                HttpServletResponse res,
                String error) throws IOException {

            String context = req.getContextPath();

            page(req, res, "Ingresar",
                """
                <div class='login-wrap'>

                    <div class='login-side'>

                        <span class='pill'>Portal privado</span>

                        <h1>Bienvenido a My Construction.</h1>

                        <p>
                            Administra tus proyectos
                            en un solo lugar.
                        </p>

                    </div>
                """
                + "<form class='form' method='post' action='"
                + context
                + "/login'>"
                + """
                    <h2>Iniciar sesión</h2>

                    <p>Ingresa tus credenciales corporativas.</p>
                """
                + (error.isEmpty()
                    ? ""
                    : "<div class='error' role='alert'>"
                    + Validation.escape(error)
                    + "</div>")
                + """
                    <label>
                        Correo electrónico

                        <input type='email'
                            name='email'
                            required
                            autocomplete='username'
                            maxlength='190'
                            placeholder='nombre@empresa.cl'>
                    </label>

                    <label>
                        Contraseña

                        <input type='password'
                            name='password'
                            required
                            autocomplete='current-password'
                            placeholder='********'>
                    </label>

                    <button type='submit'>
                        Ingresar al sistema
                    </button>

                    <small>
                        Acceso exclusivo para
                        usuarios autorizados.
                    </small>

                    </form>
                </div>
                """
            );
        }

        // DISEÑO GENERAL DE LA APLICACION
        private void page(
                HttpServletRequest req,
                HttpServletResponse res,
                String title,
                String body) throws IOException {

            String context = req.getContextPath();

            res.setContentType("text/html; charset=UTF-8");
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("X-Frame-Options", "DENY");

            String css = """
                * {
                    box-sizing: border-box;
                }

                body {
                    margin: 0;
                    font-family: Arial, sans-serif;
                    background: #f5f6f4;
                    color: #20312b;
                }

                header {
                    background: #102a24;
                    color: white;
                    padding: 20px max(6%, 20px);
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                header a {
                    color: white;
                    text-decoration: none;
                    margin-left: 24px;
                }

                .logo {
                    font-weight: 800;
                    letter-spacing: 2px;
                }

                .accent {
                    color: #e1b85c;
                }

                .hero {
                    padding: 110px max(8%, 24px);
                    background: linear-gradient(
                        110deg, #102a24, #295446);
                    color: white;
                }

                .hero h1 {
                    font-size: clamp(36px, 6vw, 68px);
                    line-height: 1.1;
                }

                .hero p {
                    max-width: 530px;
                    line-height: 1.7;
                }

                .pill {
                    display: inline-block;
                    background: #e1b85c;
                    color: #102a24;
                    padding: 9px 15px;
                    border-radius: 25px;
                    font-size: 13px;
                    font-weight: 700;
                }

                .btn, button {
                    display: inline-block;
                    margin-top: 25px;
                    background: #e1b85c;
                    color: #102a24;
                    border: 0;
                    padding: 15px 25px;
                    border-radius: 7px;
                    font-weight: 700;
                    text-decoration: none;
                    cursor: pointer;
                }

                .btn:hover, button:hover {
                    background: #d1a747;
                }

                .features {
                    display: grid;
                    grid-template-columns: repeat(3, 1fr);
                    gap: 25px;
                    padding: 45px max(8%, 24px);
                }

                .features > div, .panel, .form {
                    padding: 32px;
                    background: white;
                    border-radius: 13px;
                    box-shadow: 0 8px 30px #102a2410;
                }

                .login-wrap {
                    max-width: 1040px;
                    margin: 80px auto;
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    box-shadow: 0 15px 50px #102a241a;
                    border-radius: 15px;
                    overflow: hidden;
                }

                .login-side {
                    background: #173b31;
                    padding: 65px 45px;
                    color: white;
                }

                .login-side h1 {
                    font-size: 43px;
                }

                .form {
                    border-radius: 0;
                    box-shadow: none;
                }

                .form h2 {
                    margin-top: 4px;
                }

                .form p, .form small {
                    color: #68786e;
                }

                .form label {
                    display: block;
                    font-size: 14px;
                    font-weight: bold;
                    margin: 20px 0;
                }

                .form input {
                    display: block;
                    width: 100%;
                    margin-top: 9px;
                    padding: 15px;
                    border: 1px solid #d6dfd9;
                    border-radius: 7px;
                    font: inherit;
                }

                .form button {
                    width: 100%;
                }

                .error {
                    background: #ffe8e8;
                    color: #8d2525;
                    padding: 12px;
                    border-radius: 6px;
                }

                .panel {
                    max-width: 1000px;
                    margin: 70px auto;
                }

                footer {
                    text-align: center;
                    padding: 22px;
                    color: #677e71;
                }

                @media(max-width: 700px) {

                    .login-wrap, .features {
                        grid-template-columns: 1fr;
                    }

                    .login-wrap {
                        margin: 20px;
                    }

                    .login-side {
                        padding: 32px;
                    }

                    .hero {
                        padding: 60px 24px;
                    }
                }
                """;

            res.getWriter().write(
                """
                <!doctype html>
                <html lang='es'>

                <head>

                    <meta charset='utf-8'>

                    <meta name='viewport'
                        content='width=device-width,initial-scale=1'>
                """
                + "<title>"
                + Validation.escape(title)
                + " | My Construction</title>"
                + "<style>" + css + "</style>"
                + """
                </head>

                <body>

                    <header>

                        <div class='logo'>
                            MY <span class='accent'>CONSTRUCTION</span>
                        </div>

                        <nav>
                """
                + "<a href='" + context + "/'>Inicio</a>"
                + "<a href='" + context + "/login'>Ingresar</a>"
                + """
                        </nav>

                    </header>
                """
                + "<main>" + body + "</main>"
                + """
                    <footer>
                        © 2026 My Construction
                        - Plataforma de gestión
                    </footer>

                </body>
                </html>
                """
            );
        }
    }
}


