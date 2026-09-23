package com.futprediction.shared.util;

import java.util.Locale;
import java.util.Map;

public final class FlagUrlResolver {

    private static final Map<String, String> COUNTRY_CODES = Map.ofEntries(
            Map.entry("argentina", "ar"),
            Map.entry("brasil", "br"),
            Map.entry("brazil", "br"),
            Map.entry("francia", "fr"),
            Map.entry("france", "fr"),
            Map.entry("inglaterra", "gb-eng"),
            Map.entry("england", "gb-eng"),
            Map.entry("españa", "es"),
            Map.entry("espana", "es"),
            Map.entry("spain", "es"),
            Map.entry("portugal", "pt"),
            Map.entry("holanda", "nl"),
            Map.entry("netherlands", "nl"),
            Map.entry("belgica", "be"),
            Map.entry("bélgica", "be"),
            Map.entry("belgium", "be"),
            Map.entry("alemania", "de"),
            Map.entry("germany", "de"),
            Map.entry("italia", "it"),
            Map.entry("italy", "it"),
            Map.entry("colombia", "co"),
            Map.entry("mexico", "mx"),
            Map.entry("méxico", "mx"),
            Map.entry("estados unidos", "us"),
            Map.entry("usa", "us"),
            Map.entry("uruguay", "uy"),
            Map.entry("chile", "cl"),
            Map.entry("ecuador", "ec"),
            Map.entry("peru", "pe"),
            Map.entry("perú", "pe"),
            Map.entry("paraguay", "py"),
            Map.entry("venezuela", "ve"),
            Map.entry("croacia", "hr"),
            Map.entry("croatia", "hr"),
            Map.entry("marruecos", "ma"),
            Map.entry("morocco", "ma"),
            Map.entry("japon", "jp"),
            Map.entry("japón", "jp"),
            Map.entry("japan", "jp"),
            Map.entry("corea del sur", "kr"),
            Map.entry("south korea", "kr"),
            Map.entry("canada", "ca"),
            Map.entry("canadá", "ca"),
            Map.entry("suiza", "ch"),
            Map.entry("switzerland", "ch"),
            Map.entry("polonia", "pl"),
            Map.entry("poland", "pl"),
            Map.entry("dinamarca", "dk"),
            Map.entry("denmark", "dk"),
            Map.entry("suecia", "se"),
            Map.entry("sweden", "se"),
            Map.entry("austria", "at"),
            Map.entry("serbia", "rs"),
            Map.entry("ucrania", "ua"),
            Map.entry("ukraine", "ua"),
            Map.entry("turquia", "tr"),
            Map.entry("turkey", "tr"),
            Map.entry("tunez", "tn"),
            Map.entry("túnez", "tn"),
            Map.entry("senegal", "sn"),
            Map.entry("ghana", "gh"),
            Map.entry("nigeria", "ng"),
            Map.entry("camerun", "cm"),
            Map.entry("cameroon", "cm"),
            Map.entry("australia", "au"),
            Map.entry("qatar", "qa"),
            Map.entry("arabia saudita", "sa"),
            Map.entry("saudi arabia", "sa"),
            Map.entry("iran", "ir"),
            Map.entry("irán", "ir"),
            Map.entry("costa rica", "cr"),
            Map.entry("panama", "pa"),
            Map.entry("panamá", "pa"),
            Map.entry("wales", "gb-wls"),
            Map.entry("gales", "gb-wls"),
            Map.entry("escocia", "gb-sct"),
            Map.entry("scotland", "gb-sct"));

    private FlagUrlResolver() {}

    public static String resolve(String banderaUrl, String pais, String nombre) {
        if (banderaUrl != null && !banderaUrl.isBlank()) {
            return banderaUrl.trim();
        }
        String code = codeFor(pais);
        if (code == null) {
            code = codeFor(nombre);
        }
        return code != null ? "https://flagcdn.com/w80/" + code + ".png" : "";
    }

    private static String codeFor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return COUNTRY_CODES.get(normalize(value));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
