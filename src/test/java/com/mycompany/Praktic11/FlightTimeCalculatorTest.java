package com.mycompany.Praktic11;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;

class FlightTimeCalculatorTest {
    
    private List<Flight> testFlights;
    private Map<String, CrewMember> crewMembers;

    @BeforeEach
    void setUp() {
        testFlights = new ArrayList<>();
        testFlights.add(new Flight("Boeing737", "1234", 
                LocalDateTime.of(2025, 3, 1, 10, 0), 
                LocalDateTime.of(2025, 3, 1, 12, 30),
                Arrays.asList("Ivanov", "Petrov")));

        testFlights.add(new Flight("AirbusA320", "5678", 
                LocalDateTime.of(2025, 3, 2, 14, 15), 
                LocalDateTime.of(2025, 3, 2, 17, 45),
                Arrays.asList("Sidorov", "Smirnov")));

        crewMembers = FlightTimeCalculytor.calculateFlightHours(testFlights);
    }

    @Test
    void testCalculateFlightHours() {
        assertEquals(2, crewMembers.size(), "Должно быть 2 уникальных члена экипажа");

        CrewMember ivanov = crewMembers.get("Ivanov");
        assertNotNull(ivanov, "Экипаж Иванова должен существовать");
        assertEquals(1, ivanov.monthlyHours.size(), "Должен быть 1 месяц с налётом");
        
        CrewMember petrov = crewMembers.get("Petrov");
        assertNotNull(petrov, "Экипаж Петрова должен существовать");
        assertEquals(1, petrov.monthlyHours.size(), "Должен быть 1 месяц с налётом");
    }

    @Test
    void testSaveReport() throws IOException {
        String testFile = "test_report.json";
        FlightTimeCalculytor.saveReport(crewMembers, testFile);

        assertTrue(Files.exists(Paths.get(testFile)), "Файл отчёта должен существовать");

        // Проверяем, что файл не пустой
        String content = Files.readString(Paths.get(testFile));
        assertFalse(content.isEmpty(), "Файл отчёта не должен быть пустым");

        // Удаляем тестовый файл после проверки
        Files.delete(Paths.get(testFile));
    }
}
