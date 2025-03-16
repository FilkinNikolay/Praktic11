package com.mycompany.Praktic11;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

class Flight {
    String aircraftType;
    String aircraftNumber;
    LocalDateTime departure;
    LocalDateTime arrival;
    List<String> crew;

    Flight(String aircraftType, String aircraftNumber, LocalDateTime departure, LocalDateTime arrival, List<String> crew) {
        this.aircraftType = aircraftType;
        this.aircraftNumber = aircraftNumber;
        this.departure = departure;
        this.arrival = arrival;
        this.crew = crew;
    }
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class CrewMember {
    String id;
    String name;
    Map<YearMonth, Duration> monthlyHours = new HashMap<>();
    Map<YearMonth, Map<Integer, Duration>> weeklyHours = new HashMap<>();
    Map<LocalDate, Duration> dailyHours = new HashMap<>();
    List<String> warnings = new ArrayList<>();

    CrewMember(String id, String name) {
        this.id = id;
        this.name = name;
    }
}


public class FlightTimeCalculytor {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        String inputFile = "flights.csv";
        String outputJson = "report.json";

        try {
            if (!Files.exists(Paths.get(inputFile))) {
                System.err.println("Файл не найден: " + inputFile);
                return;
            }

            List<Flight> flights = loadFlights(inputFile);
            Map<String, CrewMember> crewMembers = calculateFlightHours(flights);
               System.out.println("Количество членов экипажа: " + crewMembers.size());

                for (CrewMember member : crewMembers.values()) {
                System.out.println("Экипаж: " + member.name + ", налет: " + member.monthlyHours);
            }           Map<String, CrewMember> testCrew = new HashMap<>();
            CrewMember testMember = new CrewMember("Test", "Тестовый экипаж");
            testMember.warnings.add("Тестовое предупреждение");
            testCrew.put("Test", testMember);

            saveReport(crewMembers, "report.json");


        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    private static List<Flight> loadFlights(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath))
                .skip(1)
                .map(line -> line.split(","))
                .filter(parts -> parts.length >= 5)
                .map(parts -> {
                    try {
                        LocalDateTime departure = LocalDateTime.parse(parts[2], formatter);
                        LocalDateTime arrival = LocalDateTime.parse(parts[3], formatter);
                        List<String> crew = Arrays.asList(parts[4].split(";"));
                        return new Flight(parts[0], parts[1], departure, arrival, crew);
                    } catch (Exception e) {
                        System.err.println("Ошибка обработки строки: " + Arrays.toString(parts));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Map<String, CrewMember> calculateFlightHours(List<Flight> flights) {
        Map<String, CrewMember> crewMap = new HashMap<>();

        for (Flight flight : flights) {
            Duration flightDuration = Duration.between(flight.departure, flight.arrival);
            YearMonth month = YearMonth.from(flight.departure);
            int weekNumber = flight.departure.get(WeekFields.of(Locale.getDefault()).weekOfYear());
            LocalDate day = flight.departure.toLocalDate();

            for (String crewId : flight.crew) {
                crewMap.putIfAbsent(crewId, new CrewMember(crewId, "Crew " + crewId));
                CrewMember member = crewMap.get(crewId);

                member.monthlyHours.merge(month, flightDuration, Duration::plus);
                member.weeklyHours.computeIfAbsent(month, k -> new HashMap<>()).merge(weekNumber, flightDuration, Duration::plus);
                member.dailyHours.merge(day, flightDuration, Duration::plus);
            }
        }

        for (CrewMember member : crewMap.values()) {
            member.monthlyHours.forEach((month, duration) -> {
                if (duration.toHours() > 80) {
                    member.warnings.add("Превышение 80 часов в " + month);
                }
            });

            member.dailyHours.forEach((date, duration) -> {
                if (duration.toHours() > 8) {
                    member.warnings.add("Превышение 8 часов в день: " + date);
                }
            });

            member.weeklyHours.forEach((month, weeks) -> 
                weeks.forEach((week, duration) -> {
                    if (duration.toHours() > 36) {
                        member.warnings.add("Превышение 36 часов в неделю (неделя " + week + "): " + month);
                    }
                })
            );
        }

        return crewMap;
    }

static void saveReport(Map<String, CrewMember> crewMembers, String filePath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule()); // Поддержка Java 8 time API
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    List<CrewMember> crewMembersList = new ArrayList<>(crewMembers.values());

    String output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(crewMembersList);

    Files.write(Paths.get(filePath), output.getBytes());
    System.out.println("Отчёт сохранён: " + Paths.get(filePath).toAbsolutePath());
}
}