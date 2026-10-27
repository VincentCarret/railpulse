package altn72.railpulse.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "prestataire")
public class Prestataire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du prestataire est obligatoire.")
    @Size(max = 100, message = "Le nom du prestataire ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "Le contact du prestataire est obligatoire.")
    @Size(max = 255, message = "Le contact du prestataire ne doit pas dépasser 255 caractères.")
    @Column(nullable = false)
    private String contact;

    protected Prestataire() {
    }

    public Prestataire(String nom, String contact) {
        this.nom = nom;
        this.contact = contact;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getContact() {
        return contact;
    }
}
