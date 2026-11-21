package cl.myconstruction;
import java.util.regex.Pattern;
public final class Validation {
    private static final Pattern EMAIL=Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private Validation() {}
    public static boolean validEmail(String email) {return email!=null && email.length()<=190 && EMAIL.matcher(email.trim()).matches();}
    public static String escape(String s) {return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
