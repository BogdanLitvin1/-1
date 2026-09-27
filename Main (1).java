import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));

        System.out.println("=== Создание рейсов ===");

        Route r1 = new Route("Москва", "Сочи");
        Route r2 = new Route("Санкт-Петербург", "Казань");
        Route r3 = new Route("Новосибирск", "Владивосток");

        Flight f1 = new Flight("SU-100", r1, 150);
        Flight f2 = new Flight("SU-200", r2);
        Flight f3 = new Flight("SU-300", r3, 50);

        Schedule schedule = new Schedule();
        schedule.addFlight(f1);
        schedule.addFlight(f2);
        schedule.addFlight(f3);

        System.out.println("Рейсов создано: " + Flight.getFlightsCreated());
        System.out.println("Рейсов в расписании: " + schedule.size());

        System.out.println("\n=== Корректные операции ===");
        f1.sellTicket();
        f1.sellTicket(50);
        f2.sellTicket(100);
        f3.sellTicket(10);

        System.out.println(f1);
        System.out.println(f2);
        System.out.println(f3);

        System.out.println("\nСредняя загрузка: " + String.format("%.2f%%", schedule.averageLoad()));

        Flight[] full = schedule.getFullFlights();
        System.out.println("Заполненных рейсов: " + full.length);
        for (Flight f : full) System.out.println("  " + f);

        System.out.println("\n=== Демонстрация защиты состояния ===");

        System.out.println("\n1) Продать больше билетов, чем есть:");
        System.out.println("   До: " + f1);
        try {
            f1.sellTicket(1000);
        } catch (IllegalStateException e) {
            System.out.println("   ОТБИТО: " + e.getMessage());
        }
        System.out.println("   После: " + f1);

        System.out.println("\n2) Создать рейс, где вылет = назначение:");
        try {
            Route bad = new Route("Москва", "Москва");
            Flight badFlight = new Flight("XX-1", bad, 100);
        } catch (IllegalArgumentException e) {
            System.out.println("   ОТБИТО: " + e.getMessage());
        }

        System.out.println("\n3) Продать -3 билета:");
        System.out.println("   До: " + f3);
        try {
            f3.sellTicket(-3);
        } catch (IllegalArgumentException e) {
            System.out.println("   ОТБИТО: " + e.getMessage());
        }
        System.out.println("   После: " + f3);

        System.out.println("\n4) Рейс с пустым номером:");
        try {
            new Flight("", r1, 100);
        } catch (IllegalArgumentException e) {
            System.out.println("   ОТБИТО: " + e.getMessage());
        }

        System.out.println("\n5) Попытка изменить маршрут через геттер:");
        Route gotRoute = f1.getRoute();
        System.out.println("   Получили: " + gotRoute);
        System.out.println("   Поля final, сеттеров нет — изменить невозможно.");
        System.out.println("   Маршрут f1 остался: " + f1.getRoute());

        System.out.println("\n6) Продать на несуществующем рейсе:");
        boolean ok = schedule.sellTickets("НЕТ-ТАКОГО", 5);
        if (!ok) System.out.println("   Рейс не найден (вернулся false, программа жива)");

        System.out.println("\n=== Программа завершилась нормально ===");
    }
}

final class Route {
    private final String from;
    private final String to;

    public Route(String from, String to) {
        if (from == null || from.isEmpty())
            throw new IllegalArgumentException("Пункт вылета пуст");
        if (to == null || to.isEmpty())
            throw new IllegalArgumentException("Пункт назначения пуст");
        if (from.equals(to))
            throw new IllegalArgumentException("Пункт вылета и назначения совпадают");
        this.from = from;
        this.to = to;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}

class Flight {
    private final String number;
    private final Route route;
    private final int totalSeats;
    private int soldSeats;

    private static int flightsCreated = 0;
    public static final int MAX_SEATS = 500;

    public Flight(String number, Route route, int totalSeats) {
        if (number == null || number.isEmpty())
            throw new IllegalArgumentException("Номер рейса пуст");
        if (route == null)
            throw new IllegalArgumentException("Маршрут не задан");
        if (totalSeats <= 0 || totalSeats > MAX_SEATS)
            throw new IllegalArgumentException("Всего мест должно быть от 1 до " + MAX_SEATS);

        this.number = number;
        this.route = route;
        this.totalSeats = totalSeats;
        this.soldSeats = 0;
        flightsCreated++;
    }

    public Flight(String number, Route route) {
        this(number, route, 100);
    }

    public void sellTicket() {
        sellTicket(1);
    }

    public void sellTicket(int count) {
        if (count <= 0)
            throw new IllegalArgumentException("Количество билетов должно быть > 0");
        if (soldSeats + count > totalSeats)
            throw new IllegalStateException("Свободных мест только: " + (totalSeats - soldSeats));
        soldSeats += count;
    }

    public String getNumber() { return number; }
    public Route getRoute() { return route; }
    public int getTotalSeats() { return totalSeats; }
    public int getSoldSeats() { return soldSeats; }
    public int getFreeSeats() { return totalSeats - soldSeats; }
    public boolean isFull() { return soldSeats == totalSeats; }

    public static int getFlightsCreated() { return flightsCreated; }

    public double getLoadPercent() {
        return (double) soldSeats / totalSeats * 100.0;
    }

    @Override
    public String toString() {
        return String.format("Рейс %s [%s] | мест: %d/%d (%.1f%%)%s",
                number, route, soldSeats, totalSeats, getLoadPercent(),
                isFull() ? " [ЗАПОЛНЕН]" : "");
    }
}

class Schedule {
    private Flight[] flights = new Flight[100];
    private int count = 0;

    public void addFlight(Flight f) {
        if (count >= flights.length)
            throw new IllegalStateException("Расписание переполнено");
        flights[count++] = f;
    }

    public Flight findByNumber(String number) {
        for (int i = 0; i < count; i++) {
            if (flights[i].getNumber().equals(number)) return flights[i];
        }
        return null;
    }

    public boolean sellTickets(String number, int cnt) {
        Flight f = findByNumber(number);
        if (f == null) return false;
        f.sellTicket(cnt);
        return true;
    }

    public Flight[] getFullFlights() {
        Flight[] result = new Flight[count];
        int k = 0;
        for (int i = 0; i < count; i++) {
            if (flights[i].isFull()) result[k++] = flights[i];
        }
        return Arrays.copyOf(result, k);
    }

    public double averageLoad() {
        if (count == 0) return 0;
        double sum = 0;
        for (int i = 0; i < count; i++) sum += flights[i].getLoadPercent();
        return sum / count;
    }

    public Flight[] getAll() {
        return Arrays.copyOf(flights, count);
    }

    public int size() { return count; }
}