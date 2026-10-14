package altn72.railpulse.modele;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "programmeur")
public class Programmeur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_programmeur", nullable = false)
    private Integer id;

    @Column(name = "nom", nullable = false, length = 100)
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    private String prenom;

    @Column(name = "adresse")
    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères.")
    private String adresse;

    @Column(name = "langage_expertise", length = 100)
    @Size(max = 100, message = "Le langage ne doit pas dépasser 100 caractères.")
    private String langageExpertise;

    @Column(name = "livre_prefere", length = 150)
    @Size(max = 150, message = "Le livre ne doit pas dépasser 150 caractères.")
    private String livrePrefere;

    @Column(name = "salaire")
    @PositiveOrZero(message = "Le salaire doit être positif ou nul.")
    @DecimalMax(value = "1.7976931348623157E308", message = "Le salaire doit être un nombre fini.")
    private Double salaire;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getLangageExpertise() {
        return langageExpertise;
    }

    public void setLangageExpertise(String langageExpertise) {
        this.langageExpertise = langageExpertise;
    }

    public String getLivrePrefere() {
        return livrePrefere;
    }

    public void setLivrePrefere(String livrePrefere) {
        this.livrePrefere = livrePrefere;
    }

    public Double getSalaire() {
        return salaire;
    }

    public void setSalaire(Double salaire) {
        this.salaire = salaire;
    }

}
