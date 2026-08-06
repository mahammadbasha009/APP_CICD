package com.example;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class AppTest {
  @Test void messageIsCorrect() { assertEquals("Simple CI/CD App is running!", App.message()); }
}
