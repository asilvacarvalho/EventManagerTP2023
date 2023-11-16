package pt.isec.eventmanager.users;

import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String password;
    private String studentNumber;
    private boolean admin = false;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, String password, String name, String studentNumber, boolean admin) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.studentNumber = studentNumber;
        this.admin = admin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
