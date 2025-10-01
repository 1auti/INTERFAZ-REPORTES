package login;

import org.zkoss.bind.annotation.Command;
import org.zkoss.zk.ui.Executions;

public class login {
    private String username;
    private String password;

    // Getters y Setters para username y password
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Método para manejar el login
    @Command
    public void doLogin() {
        if ("admin".equals(username) && "password123".equals(password)) {
            Executions.sendRedirect("/home.zul");  // Cambia "home.zul" por la página que desees redirigir
        } else {
            System.out.println("Usuario o contraseña incorrectos");
        }
    }
}
