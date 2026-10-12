package altn72.railpulse.modele;

import jakarta.persistence.*;

@Entity
@Table(name = "programmeur")
public class Programmeur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_programmeur", nullable = false)
    private Integer id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "langage_expertise", length = 100)
    private String langageExpertise;

    @Column(name = "livre_prefere", length = 150)
    private String livrePrefere;

    @Column(name = "salaire")
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