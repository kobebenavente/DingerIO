package com.kobe.dinger;
import com.kobe.dinger.service.LiveGameService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveGameServiceUnitTest {
    private final LiveGameService service = new LiveGameService(null, null);

    @Test
    void getInningOrdinal_returnsFirstForOne(){
        String result = service.getInningOrdinal(1);
        assertEquals("1st", result);
    }

    @Test
    void getInningOrdinal_returnsSecondForTwo(){
        String result = service.getInningOrdinal(2);
        assertEquals("2nd", result);
    }
    @Test
    void getInningOrdinal_returnsThirdForThree(){
        String result = service.getInningOrdinal(3);
        assertEquals("3rd", result);
    }

    @Test
    void getInningOrdinal_returnsTenthForTen(){
        String result = service.getInningOrdinal(10);
        assertEquals("10th", result);
    }


}
