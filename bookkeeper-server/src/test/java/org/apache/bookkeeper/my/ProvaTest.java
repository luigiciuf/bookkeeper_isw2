package org.apache.bookkeeper.my;

import org.apache.bookkeeper.my.Prova;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;


//Junit test class for Simple calculator
public class ProvaTest {
    private static Prova calculator;
    @Before
    public void setUp(){
        calculator = new Prova();
    }
    @Test
    public void addSimple(){
        double result = calculator.calculate(1,1,'+');
        Assertions.assertEquals(2,result);
    }
    @Test
    public void multiplySimple(){
        double result = calculator.calculate(1,1,'*');
        Assertions.assertEquals(1,result);
    }
    @Test
    public void subtractSimple(){
        double result = calculator.calculate(1,1,'-');
        Assertions.assertEquals(0,result);
    }
    @Test
    public void divideSimple(){
        double result = calculator.calculate(1,1,':');
        Assertions.assertEquals(1,result);
    }
}