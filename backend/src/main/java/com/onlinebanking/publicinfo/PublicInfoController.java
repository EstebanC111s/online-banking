package com.onlinebanking.publicinfo;

import com.onlinebanking.fees.FeeSchedule;
import com.onlinebanking.publicinfo.LegalDocuments.LegalDoc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicInfoController {

    private final LegalDocuments legal;
    private final FeeSchedule fees;
    private final PromiseService promises;

    public PublicInfoController(LegalDocuments legal, FeeSchedule fees, PromiseService promises) {
        this.legal = legal;
        this.fees = fees;
        this.promises = promises;
    }

    public record CompanyInfo(String legalName, String taxId, String address, String email, String supportHours,
                              String supervisor, String disclaimer) {
    }

    public record LicenseItem(String asset, String author, String license, String url, String usedFor) {
    }

    public record Integration(String name, String purpose, String dataShared, String status, boolean requiresConsent,
                              String lastReviewed) {
    }

    @GetMapping("/legal")
    public List<LegalDoc> legalIndex() {
        return legal.all();
    }

    @GetMapping("/legal/{slug}")
    public LegalDoc legal(@PathVariable String slug) {
        return legal.get(slug);
    }

    @GetMapping("/fees")
    public List<FeeSchedule.FeeItem> fees() {
        return fees.publicTable();
    }

    @GetMapping("/company")
    public CompanyInfo company() {
        return new CompanyInfo(
                "Banco Gocomezbo S.A.S. (empresa ficticia)",
                "NIT 000.000.000-0 (ficticio)",
                "Cali, Valle del Cauca, Colombia",
                "contacto@gocomezbo.demo",
                "Lunes a viernes, 8:00 a. m. a 6:00 p. m. (hora de Colombia)",
                "Ninguno: no es una entidad vigilada por la Superintendencia Financiera",
                "Proyecto académico de práctica en Java. No capta dinero real ni presta servicios financieros.");
    }

    @GetMapping("/licenses")
    public List<LicenseItem> licenses() {
        return List.of(
                new LicenseItem("Logo e íconos (SVG)", "Creados para este proyecto", "Uso libre dentro del proyecto", null,
                        "Logo de la cabecera e íconos de la interfaz"),
                new LicenseItem("Fotografías", "-", "-", null,
                        "Esta app no usa fotografías ni imágenes de stock"),
                new LicenseItem("Tipografía", "Fuentes del sistema operativo", "Las del sistema del usuario", null,
                        "No descargamos fuentes externas (tampoco rastrean)"),
                new LicenseItem("Spring Boot", "VMware / Broadcom", "Apache-2.0", "https://spring.io/projects/spring-boot", "Backend"),
                new LicenseItem("H2 Database", "H2 Group", "MPL-2.0 / EPL-1.0", "https://h2database.com", "Base de datos local"),
                new LicenseItem("React", "Meta Platforms", "MIT", "https://react.dev", "Interfaz web"),
                new LicenseItem("React Router", "Remix Software", "MIT", "https://reactrouter.com", "Navegación de la interfaz"),
                new LicenseItem("Vite", "VoidZero", "MIT", "https://vite.dev", "Herramienta de compilación del frontend"));
    }

    @GetMapping("/integrations")
    public List<Integration> integrations() {
        String reviewed = "2026-11-11";
        return List.of(
                new Integration("Base de datos H2 (local)", "Guardar cuentas y movimientos",
                        "Todos los datos del servicio; no sale del servidor", "Activa", false, reviewed),
                new Integration("Correo (simulado)", "Avisos de movimientos, autorización de tutor, publicidad",
                        "Correo y nombre; la publicidad solo con consentimiento", "Simulada: no se envía nada fuera del servidor",
                        false, reviewed),
                new Integration("Analítica propia", "Saber qué páginas se usan",
                        "Solo la ruta visitada, sin identidad, IP ni dispositivo", "Activa solo si la aceptas", true, reviewed),
                new Integration("Proveedores de publicidad, redes sociales o analítica de terceros", "-", "Ninguno",
                        "No usamos", false, reviewed));
    }

    @GetMapping("/promises")
    public List<PromiseService.Promise> promises() {
        return promises.all();
    }
}
