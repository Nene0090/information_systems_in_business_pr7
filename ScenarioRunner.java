import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Выполняет текстовый сценарий над новым заказом (для Main и SelfCheck).
 * Операции разделяются '|': "+P1x2" - добавить товар P1 в количестве 2, "-P1" - удалить товар P1,
 * "confirm" - подтвердить заказ (через OrderService). Каталог товаров фиксирован (см. конструктор).
 * Результат:
 *   OK|число позиций|итого|статус
 *   ERROR|тип исключения|сообщение     (сценарий останавливается на первой ошибке)
 */
public class ScenarioRunner {

    private final OrderService service = new OrderService();
    private final Map<String, Product> catalog = new LinkedHashMap<>();

    public ScenarioRunner() {
        addToCatalog(new Product("P1", "Ноутбук", 25000.00));
        addToCatalog(new Product("P2", "Мышь", 1500.00));
        addToCatalog(new Product("P3", "Монитор", 12000.00));
        addToCatalog(new Product("P4", "Кабель", 249.99));
        addToCatalog(new Product("P5", "Сервер", 45000.00));
    }

    public String run(String ops) {
        Order order = service.createOrder(new Employee("E1", "Иван Петров"));
        try {
            if (ops == null) {
                throw new IllegalArgumentException("Сценарий не задан");
            }
            if (!ops.trim().isEmpty()) {
                String[] tokens = ops.split("\\|", -1);
                for (int i = 0; i < tokens.length; i++) {
                    apply(order, tokens[i].trim(), i + 1);
                }
            }
            return String.format(Locale.ROOT, "OK|%d|%.2f|%s",
                    order.getItems().size(), order.total(), order.getStatus());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "ERROR|" + e.getClass().getSimpleName() + "|" + e.getMessage();
        }
    }

    private void apply(Order order, String op, int position) {
        if (op.isEmpty()) {
            throw new IllegalArgumentException("Операция " + position + ": пустая операция");
        }
        if (op.equals("confirm")) {
            service.confirmOrder(order);
        } else if (op.startsWith("+")) {
            String[] parts = op.substring(1).split("x", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Операция " + position + ": ожидается формат +<id>x<количество>");
            }
            order.addProduct(findProduct(parts[0], position), parseQuantity(parts[1], position));
        } else if (op.startsWith("-")) {
            order.removeProduct(op.substring(1));
        } else {
            throw new IllegalArgumentException("Операция " + position + ": неизвестная операция '" + op + "'");
        }
    }

    private Product findProduct(String id, int position) {
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Операция " + position + ": не указан товар");
        }
        Product p = catalog.get(id.trim());
        if (p == null) {
            throw new IllegalArgumentException("Операция " + position + ": неизвестный товар '" + id.trim() + "'");
        }
        return p;
    }

    private int parseQuantity(String s, int position) {
        String t = s.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Операция " + position + ": не указано количество");
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Операция " + position + ": количество '" + t
                    + "' не является целым числом");
        }
    }

    private void addToCatalog(Product p) {
        catalog.put(p.getId(), p);
    }
}
