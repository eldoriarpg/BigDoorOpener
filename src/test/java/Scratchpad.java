import de.eldoria.bigdoorsopener.util.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Scratchpad {

    @Test
    public void test() {
        Assertions.assertEquals("6:00", Parser.parseTicksToTime(0));
        Assertions.assertEquals("12:00", Parser.parseTicksToTime(6000));
        Assertions.assertEquals("18:00", Parser.parseTicksToTime(12000));
        Assertions.assertEquals("0:00", Parser.parseTicksToTime(18000));

        Assertions.assertEquals(0, Parser.parseTimeToTicks("6:00"));
        Assertions.assertEquals(6000, Parser.parseTimeToTicks("12:00"));
        Assertions.assertEquals(12000, Parser.parseTimeToTicks("18:00"));
        Assertions.assertEquals(18000, Parser.parseTimeToTicks("0:00"));

        Assertions.assertEquals(13000, Parser.parseTimeToTicks("18:60"));
        Assertions.assertEquals(18000, Parser.parseTimeToTicks("24:00"));
        Assertions.assertEquals(19000, Parser.parseTimeToTicks("25:00"));
        Assertions.assertEquals(17000, Parser.parseTimeToTicks("-1:00"));
    }
}
