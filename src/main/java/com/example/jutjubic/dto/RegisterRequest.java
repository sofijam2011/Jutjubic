package com.example.jutjubic.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email mora biti validan")
    private String email;

    @NotBlank(message = "Korisničko ime je obavezno")
    @Size(min = 3, max = 50, message = "Korisničko ime mora imati između 3 i 50 karaktera")
    private String username;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 8, message = "Lozinka mora imati najmanje 8 karaktera")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!/]).*$",
            message = "Lozinka mora sadržati bar jedno malo slovo, veliko slovo, broj i specijalni karakter")
    private String password;

    @NotBlank(message = "Potvrda lozinke je obavezna")
    private String confirmPassword;

    @NotBlank(message = "Ime je obavezno")
    private String firstName;

    @NotBlank(message = "Prezime je obavezno")
    private String lastName;

    @NotBlank(message = "Adresa je obavezna")
    private String address;

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}