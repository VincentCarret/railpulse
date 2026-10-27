package altn72.railpulse.controleur;

import altn72.railpulse.service.ExportCatalogueService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class ExportCatalogueControleur {
    private final ExportCatalogueService export;

    public ExportCatalogueControleur(ExportCatalogueService export) {
        this.export = export;
    }

    @GetMapping("/stations/export.csv")
    public void exporter(HttpServletResponse reponse) throws IOException {
        String csv = export.exporter();
        reponse.setContentType("text/csv;charset=UTF-8");
        reponse.setHeader("Content-Disposition", "attachment; filename=\"railpulse-equipements.csv\"");
        reponse.setHeader("Cache-Control", "no-store");
        reponse.getWriter().write(csv);
    }
}
