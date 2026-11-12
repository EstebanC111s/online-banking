package com.onlinebanking.publicinfo;

import com.onlinebanking.common.NotFoundException;
import com.onlinebanking.fees.FeeSchedule;
import com.onlinebanking.common.Money;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LegalDocuments {

    public static final String TERMS_VERSION = "2026-11-11";
    public static final String PRIVACY_VERSION = "2026-11-11";

    public record Section(String heading, String body) {
    }

    public record LegalDoc(String slug, String title, String version, List<Section> sections) {
    }

    private final Map<String, LegalDoc> docs = new LinkedHashMap<>();

    public LegalDocuments(FeeSchedule fees) {
        docs.put("terminos", new LegalDoc("terminos", "Términos de uso", TERMS_VERSION, List.of(
                new Section("Quiénes somos", "Banco Ceiba Demo es un proyecto académico de práctica. No es una entidad financiera, "
                        + "no está vigilado por la Superintendencia Financiera de Colombia y no maneja dinero real. Los datos de empresa están en /empresa."),
                new Section("Tu cuenta", "Para usar la banca en línea necesitas registrarte con tu nombre, correo, contraseña y fecha de nacimiento. "
                        + "Si eres menor de 18 años, tu padre, madre o tutor debe autorizar la cuenta antes de que puedas operar. La edad mínima es 14 años."),
                new Section("Costos", "Todos los costos están publicados en la tabla de tarifas y se muestran antes de confirmar cada operación. "
                        + "No cobramos nada que no aparezca en esa tabla."),
                new Section("Transferencias", "Cada transferencia tiene dos pasos: primero ves una cotización con el destinatario, el monto, la comisión y el total; "
                        + "luego decides si la confirmas. La cotización vence a los pocos minutos y no se ejecuta si no la confirmas."),
                new Section("Derecho de retracto", "Puedes retractarte de la apertura de una cuenta dentro de los 5 días hábiles siguientes, sin penalidad, "
                        + "siempre que su saldo esté en cero (puedes moverlo gratis a otra cuenta tuya). Inspirado en el artículo 47 de la Ley 1480 de 2011."),
                new Section("Reclamos y devoluciones", "Puedes abrir una disputa sobre cualquier cargo. Las reglas están en la Política de devoluciones."),
                new Section("Cierre", "Puedes cerrar tus cuentas y pedir el borrado de tus datos en cualquier momento desde Privacidad.")
        )));

        docs.put("privacidad", new LegalDoc("privacidad", "Política de privacidad", PRIVACY_VERSION, List.of(
                new Section("Responsable", "Banco Ceiba Demo (proyecto académico). Contacto: privacidad@ceiba.demo. "
                        + "Esta política sigue los principios de la Ley 1581 de 2012 de protección de datos personales en Colombia."),
                new Section("Qué datos recogemos y por qué", "Nombre (identificarte), correo (iniciar sesión y avisos), contraseña cifrada con BCrypt (seguridad), "
                        + "fecha de nacimiento (verificar mayoría de edad) y, solo si eres menor, el correo de tu tutor. Nada más: no pedimos teléfono, dirección, documento ni género."),
                new Section("Movimientos", "Guardamos tus cuentas y movimientos para prestarte el servicio. Si pides borrar tus datos, tu perfil se anonimiza; "
                        + "los movimientos contables se conservan sin datos que te identifiquen porque la ley exige conservar registros contables."),
                new Section("Consentimientos opcionales", "Publicidad por correo y medición anónima de uso. Ambos empiezan desactivados y los cambias cuando quieras en Privacidad."),
                new Section("Con quién compartimos", "Con nadie. La lista completa de integraciones y qué dato recibe cada una está en /integraciones."),
                new Section("Tus derechos", "Conocer, actualizar, rectificar y suprimir tus datos, y revocar consentimientos. Desde Privacidad puedes descargar todos tus datos en JSON "
                        + "y borrar tu cuenta."),
                new Section("Inteligencia artificial", "No usamos IA para tomar decisiones sobre ti. Detalle en el Aviso de IA.")
        )));

        docs.put("cookies", new LegalDoc("cookies", "Aviso de cookies y almacenamiento", "2026-11-11", List.of(
                new Section("Esenciales (siempre activas)", "Guardamos tu sesión en el almacenamiento de la pestaña (sessionStorage) para que no tengas que iniciar sesión "
                        + "en cada página. Se borra al cerrar la pestaña. También guardamos tu elección sobre este aviso."),
                new Section("Analítica (opcional)", "Si la aceptas, contamos qué páginas se visitan, sin tu identidad, IP ni dispositivo. Si la rechazas no se envía nada."),
                new Section("Publicidad de terceros", "No usamos cookies de publicidad ni de terceros."),
                new Section("Cambiar tu elección", "Puedes cambiarla en cualquier momento con el enlace \"Preferencias de cookies\" del pie de página.")
        )));

        docs.put("devoluciones", new LegalDoc("devoluciones", "Política de devoluciones y disputas", "2026-11-11", List.of(
                new Section("Comisiones", "Si disputas una comisión, la devolvemos de inmediato y de forma automática, sin preguntas."),
                new Section("Transferencias", "Si disputas una transferencia, la revisamos y respondemos en máximo 15 días hábiles. "
                        + "Si procede, el dinero vuelve a tu cuenta como un movimiento de \"Devolución\"."),
                new Section("Rechazos", "Si rechazamos una disputa, siempre te decimos por qué por escrito."),
                new Section("Plazo para reclamar", "Puedes disputar movimientos de los últimos 90 días.")
        )));

        docs.put("ia", new LegalDoc("ia", "Aviso de inteligencia artificial", "2026-11-11", List.of(
                new Section("¿Usamos IA?", "No. Ninguna decisión sobre tu cuenta (aprobaciones, bloqueos, disputas) la toma un sistema de inteligencia artificial."),
                new Section("Automatizaciones", "Lo único automático son reglas fijas y públicas: la tarifa de transferencias ("
                        + Money.format(fees.thirdPartyTransferFee()) + " a terceros, $0 entre tus cuentas) y la devolución inmediata de comisiones disputadas."),
                new Section("Si algún día usamos IA", "Lo marcaremos claramente en la pantalla donde se use, actualizaremos este aviso y "
                        + "siempre podrás pedir que una persona revise la decisión.")
        )));
    }

    public LegalDoc get(String slug) {
        LegalDoc doc = docs.get(slug);
        if (doc == null) {
            throw new NotFoundException("Documento");
        }
        return doc;
    }

    public List<LegalDoc> all() {
        return List.copyOf(docs.values());
    }
}
