package com.example.agenticai.agent.tool.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorToolTest {

    private final CalculatorTool tool = new CalculatorTool();

    @Test
    void addsTwoNumbers() {
        String result = tool.execute(Map.of("a", 2, "b", 3, "operation", "add"));
        assertEquals("5.0", result);
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(ArithmeticException.class, () ->
                tool.execute(Map.of("a", 1, "b", 0, "operation", "divide")));
    }

    @Test
    void multiplyTwoNumbers() {
        String result = tool.execute(Map.of("a", 4, "b", 5, "operation", "multiply"));
        assertEquals("20.0", result);
    }
}
