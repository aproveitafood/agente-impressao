package qz.common;

import com.github.zafarkhaja.semver.Version;
import qz.utils.JavaVersion;

import java.awt.*;

import static qz.ws.SingleInstanceChecker.STEAL_WEBSOCKET_PROPERTY;

/**
 * Created by robert on 7/9/2014.
 */
public class Constants {
    public static final String HEXES = "0123456789ABCDEF";
    public static final char[] HEXES_ARRAY = HEXES.toCharArray();
    public static final int BYTE_BUFFER_SIZE = 8192;
    public static final Version VERSION = Version.valueOf("2.3.0");
    public static final Version JAVA_VERSION = JavaVersion.current();
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");

    /* QZ-Tray Constants */
    public static final String BLOCK_FILE = "blocked";
    public static final String ALLOW_FILE = "allowed";
    public static final String TEMP_FILE = "temp";
    public static final String LOG_FILE = "debug";
    public static final String PROPS_FILE = "agente-impressao"; // .properties extension is assumed
    public static final String PREFS_FILE = "prefs"; // .properties extension is assumed
    public static final String[] PERSIST_PROPS = {"file.whitelist", "file.allow", "networking.hostname", "networking.port", STEAL_WEBSOCKET_PROPERTY };
    public static final String AUTOSTART_FILE = ".autostart";
    public static final String DATA_DIR = "agente-impressao";

    public static final int BORDER_PADDING = 10;

    public static final String ABOUT_TITLE = "Agente de Impressao";
    public static final String APP_DISPLAY_NAME = "Aproveita Food";
    public static final String ABOUT_EMAIL = "contato@aproveitafood.com.br";
    public static final String ABOUT_URL = "https://github.com/aproveitafood/agente-impressao";
    public static final String ABOUT_COMPANY = "Aproveita Food";
    public static final String ABOUT_CITY = "";
    public static final String ABOUT_STATE = "";
    public static final String ABOUT_COUNTRY = "BR";

    public static final String ABOUT_LICENSING_URL = Constants.ABOUT_URL + "/blob/master/LICENSE.txt";
    public static final String ABOUT_SUPPORT_URL = Constants.ABOUT_URL + "/issues";
    public static final String ABOUT_PRIVACY_URL = Constants.ABOUT_URL;
    public static final String ABOUT_DOWNLOAD_URL = Constants.ABOUT_URL + "/releases/latest";

    public static final String VERSION_CHECK_URL = "https://api.github.com/repos/aproveitafood/agente-impressao/releases";
    public static final String VERSION_DOWNLOAD_URL = "https://github.com/aproveitafood/agente-impressao/releases";
    public static final boolean ENABLE_DIAGNOSTICS = true; // Diagnostics menu (logs, etc)

    public static final String BRAND_COLOR_HEX = "#BC361C";
    public static final Color BRAND_PRIMARY_COLOR = Color.decode(BRAND_COLOR_HEX);
    public static final Color BRAND_SUCCESS_COLOR = Color.decode("#6E8B4A");
    public static final Color BRAND_DANGER_COLOR = Color.decode("#D84A2B");

    // Brand surface, mirrored from ../frontend/src/styles/tokens.css.  The tray menu always uses
    // these, because it is a branded surface and must not follow the desktop's prefer-dark.
    public static final Color BRAND_CREAM_COLOR = Color.decode("#FFF3E7");
    public static final Color BRAND_CREAM_LIGHT_COLOR = Color.decode("#FFFAF4");
    public static final Color BRAND_CARD_COLOR = Color.decode("#FFFFFF");
    public static final Color BRAND_COCOA_COLOR = Color.decode("#4A2B1F");
    public static final Color BRAND_MUTED_COLOR = Color.decode("#765F55");
    public static final Color BRAND_BORDER_COLOR = Color.decode("#E8D9CC");
    @SuppressWarnings("ConstantValue")
    public static final boolean IS_REBRANDED = !ABOUT_EMAIL.equals("support@qz.io");

    public static final String TRUSTED_CERT = String.format("Verified by %s", Constants.ABOUT_COMPANY);
    public static final String SPONSORED_CERT = String.format("Sponsored by %s", Constants.ABOUT_COMPANY);
    public static final String SPONSORED_TOOLTIP = "Sponsored organization";
    public static final String STRICT_MODE_CERT = "Strictly permitted";
    public static final String THIRD_PARTY_CERT = "Third-party issued";
    public static final String UNTRUSTED_CERT = "Untrusted website";
    public static final String NO_TRUST = "Cannot verify trust";

    public static final String PROBE_REQUEST = "getProgramName";
    public static final String PROBE_RESPONSE = ABOUT_TITLE;

    public static final String ALLOW_SITES_TEXT = "Acesso permanente a recursos locais autorizado para \"%s\"";
    public static final String BLOCK_SITES_TEXT = "Acesso permanente a recursos locais bloqueado para \"%s\"";

    public static final String REMEMBER_THIS_DECISION = "Lembrar esta decisão";
    public static final String STRICT_MODE_LABEL = "Usar modo estrito de certificados";
    public static final String STRICT_MODE_TOOLTIP = String.format("Impede selecionar \"%s\" para a maioria dos sites", REMEMBER_THIS_DECISION);
    public static final String STRICT_MODE_CONFIRM = String.format("Ativar o modo estrito de certificados? A maioria dos sites deixará de funcionar com %s.", ABOUT_TITLE);
    public static final String ALLOW_SITES_LABEL = "Sites com acesso permanente autorizado";
    public static final String BLOCK_SITES_LABEL = "Sites com acesso permanente bloqueado";


    public static final String ALLOWED = "Autorizados";
    public static final String BLOCKED = "Bloqueados";

    public static final String OVERRIDE_CERT = "override.crt";
    public static final String WHITELIST_CERT_DIR = "whitelist";
    public static final String PROVISION_DIR = "provision";
    public static final String PROVISION_FILE = "provision.json";

    public static final String SIGNING_PRIVATE_KEY = "private-key.pem";
    public static final String SIGNING_CERTIFICATE = "digital-certificate.txt";

    public static final long VALID_SIGNING_PERIOD = 15 * 60 * 1000; //millis
    public static final int EXPIRY_WARN = 30;   // days
    public static final Color WARNING_COLOR_LITE = Color.RED;
    public static final Color TRUSTED_COLOR_LITE = Color.BLUE;
    public static final Color WARNING_COLOR_DARK = Color.decode("#EB6261");
    public static final Color TRUSTED_COLOR_DARK = Color.decode("#589DF6");
    public static Color WARNING_COLOR = WARNING_COLOR_LITE;
    public static Color TRUSTED_COLOR = TRUSTED_COLOR_LITE;

    public static boolean MASK_TRAY_SUPPORTED = true;

    public static final long MEMORY_PER_PRINT = 512; //MB

    public static final String RAW_PRINT = ABOUT_TITLE + " Raw Print";
    public static final String IMAGE_PRINT = ABOUT_TITLE + " Pixel Print";
    public static final String PDF_PRINT = ABOUT_TITLE + " PDF Print";
    public static final String HTML_PRINT = ABOUT_TITLE + " HTML Print";

    public static final Integer[] DEFAULT_WSS_PORTS = {8181, 8282, 8383, 8484};
    public static final Integer[] DEFAULT_WS_PORTS = {8182, 8283, 8384, 8485};
    public static final Integer[] CUPS_RSS_PORTS = {8586, 8687, 8788, 8889};
}
