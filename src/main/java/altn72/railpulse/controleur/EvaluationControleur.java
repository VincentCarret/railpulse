package altn72.railpulse.controleur;

import altn72.railpulse.modele.Evaluation;
import altn72.railpulse.modele.Incident;
import altn72.railpulse.service.EvaluationService;
import altn72.railpulse.service.NouvelleEvaluation;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/incidents/{incidentId}/evaluer")
public class EvaluationControleur {
    private final EvaluationService evaluationService;

    public EvaluationControleur(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @InitBinder("nouvelleEvaluation")
    public void limiterChamps(WebDataBinder binder) {
        binder.setAllowedFields("note", "commentaire");
    }

    @GetMapping
    public String preparer(@PathVariable Long incidentId, Model model) {
        model.addAttribute("nouvelleEvaluation", new NouvelleEvaluation());
        return formulaire(evaluationService.preparerEvaluation(incidentId), model);
    }

    @PostMapping
    public String enregistrer(@PathVariable Long incidentId,
                              @Valid @ModelAttribute("nouvelleEvaluation") NouvelleEvaluation saisie,
                              BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return formulaire(evaluationService.preparerEvaluation(incidentId), model);
        }
        evaluationService.evaluer(incidentId, saisie);
        redirection.addFlashAttribute("succes", "L'évaluation de l'intervention a été enregistrée.");
        return "redirect:/incidents/" + incidentId;
    }

    private String formulaire(Incident incident, Model model) {
        model.addAttribute("incident", incident);
        model.addAttribute("noteMin", Evaluation.NOTE_MIN);
        model.addAttribute("noteMax", Evaluation.NOTE_MAX);
        model.addAttribute("longueurCommentaireMax", Evaluation.LONGUEUR_COMMENTAIRE_MAX);
        return "evaluerIntervention";
    }
}
