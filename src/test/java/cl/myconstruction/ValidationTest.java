package cl.myconstruction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ValidationTest {
 @Test void aceptaCorreoValido(){assertTrue(Validation.validEmail("usuario@empresa.cl"));}
 @Test void rechazaCorreoInvalido(){assertFalse(Validation.validEmail("sin-arroba"));assertFalse(Validation.validEmail(null));}
 @Test void evitaHtmlInyectado(){assertEquals("&lt;script&gt;&amp;&lt;/script&gt;",Validation.escape("<script>&</script>"));}
}
