package altn72.railpulse.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "equipement", indexes = @Index(name = "idx_equipement_station", columnList = "station_id"))
public class Equipement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La référence de l'équipement est obligatoire.")
    @Size(max = 50, message = "La référence ne doit pas dépasser 50 caractères.")
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @NotBlank(message = "Le libellé de l'équipement est obligatoire.")
    @Size(max = 150, message = "Le libellé ne doit pas dépasser 150 caractères.")
    @Column(nullable = false, length = 150)
    private String libelle;

    @NotNull(message = "Le type de l'équipement est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeEquipement type;

    @NotNull(message = "La station de l'équipement est obligatoire.")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @NotNull(message = "L'état de fonctionnement est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(name = "etat_fonctionnement", nullable = false, length = 30)
    private EtatFonctionnement etatFonctionnement;

    protected Equipement() {
    }

    public Equipement(String reference, String libelle, TypeEquipement type,
                      Station station, EtatFonctionnement etatFonctionnement) {
        this.reference = reference;
        this.libelle = libelle;
        this.type = type;
        this.station = station;
        this.etatFonctionnement = etatFonctionnement;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getLibelle() {
        return libelle;
    }

    public TypeEquipement getType() {
        return type;
    }

    public Station getStation() {
        return station;
    }

    public EtatFonctionnement getEtatFonctionnement() {
        return etatFonctionnement;
    }
}
