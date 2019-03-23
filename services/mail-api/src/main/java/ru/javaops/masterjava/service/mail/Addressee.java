package ru.javaops.masterjava.service.mail;

import lombok.*;

/**
 * gkislin
 * 15.11.2016
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "email")
public class Addressee {
    @NonNull
    private String email;
    private String name;

    public Addressee(String email) {
        this(email, null);
        email = email.trim();
        int idx = email.indexOf('<');
        if (idx == -1) {
            this.email = email;
        } else {
            this.name = email.substring(0, idx).trim();
            this.email = email.substring(idx + 1, email.length() - 1).trim();
        }
    }

    public String toString() {
        return name == null ? email : String.format("%s <%s>", name, email);
    }
}
