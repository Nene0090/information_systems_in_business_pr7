import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Самопроверка без внешних библиотек. Код возврата 1, если хотя бы одна проверка не прошла.
 * Запуск: java SelfCheck [путь к sample.csv]
 */
public class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;
    private static final Product P1 = new Product("P1", "Ноутбук", 25000.00);
    private static final Product P2 = new Product("P2", "Мышь", 1500.00);
    private static final Employee E = new Employee("E1", "Иван Петров");

    public static void main(String[] args) throws IOException {
        try {
            runAll(args);
        } catch (RuntimeException e) {
            failed++;
            System.out.println("FAIL  неожиданное исключение в проверке: " + e);
        }    }

    private static void runAll(String[] args) throws IOException {
        System.out.println("--- Product и Employee: инварианты конструктора ---");
        expectIae("Product: id null", () -> new Product(null, "x", 1));
        expectIae("Product: пустой id", () -> new Product("  ", "x", 1));
        expectIae("Product: пустое наименование", () -> new Product("P", "", 1));
        expectIae("Product: цена 0", () -> new Product("P", "x", 0));
        expectIae("Product: отрицательная цена", () -> new Product("P", "x", -5));
        expectIae("Product: цена NaN", () -> new Product("P", "x", Double.NaN));
        expectIae("Product: цена Infinity", () -> new Product("P", "x", Double.POSITIVE_INFINITY));
        check("Product: минимальная положительная цена допустима", new Product("P", "x", 0.01).getPrice() == 0.01);
        expectIae("Employee: пустой id", () -> new Employee("", "Иван"));
        expectIae("Employee: null ФИО", () -> new Employee("E1", null));
        expectIae("Order: null сотрудник", () -> new Order(null));

        System.out.println("--- Количество позиции: границы 1..100 ---");
        expectIae("OrderItem: 0", () -> new OrderItem(P1, 0));
        expectIae("OrderItem: -1", () -> new OrderItem(P1, -1));
        check("OrderItem: 1 допустимо", new OrderItem(P1, 1).getQuantity() == 1);
        check("OrderItem: 100 допустимо", new OrderItem(P1, 100).getQuantity() == 100);
        expectIae("OrderItem: 101", () -> new OrderItem(P1, 101));
        expectIae("OrderItem: товар null", () -> new OrderItem(null, 1));
        expectIae("increase: переполнение int", () -> new OrderItem(P1, 100).increase(Integer.MAX_VALUE));

        System.out.println("--- Добавление и объединение ---");
        Order o = new Order(E);
        o.addProduct(P1, 1);
        o.addProduct(P2, 2);
        check("две разные позиции", o.getItems().size() == 2);
        o.addProduct(P2, 3);
        check("повторный товар объединён в одну строку", o.getItems().size() == 2);
        check("количество объединено: 2 + 3 = 5", o.getItems().get(1).getQuantity() == 5);
        check("порядок строк сохранён", o.getItems().get(0).getProduct().getId().equals("P1"));
        Order o2 = new Order(E);
        o2.addProduct(P2, 99);
        expectIae("объединение 99 + 2 > 100", () -> o2.addProduct(P2, 2));
        check("после отказа количество не изменилось", o2.getItems().get(0).getQuantity() == 99);
        o2.addProduct(P2, 1);
        check("объединение до ровно 100 допустимо", o2.getItems().get(0).getQuantity() == 100);
        expectIae("addProduct: null товар", () -> new Order(E).addProduct(null, 1));
        expectIae("addProduct: количество 0", () -> new Order(E).addProduct(P1, 0));
        Order o3 = new Order(E);
        o3.addProduct(P1, 1);
        expectIae("конфликт: тот же id, другая цена", () -> o3.addProduct(new Product("P1", "Ноутбук", 1.0), 1));
        expectIae("конфликт: тот же id, другое наименование", () -> o3.addProduct(new Product("P1", "Планшет", 25000.00), 1));
        check("после конфликта заказ не изменился", o3.getItems().size() == 1 && o3.getItems().get(0).getQuantity() == 1);

        System.out.println("--- Удаление по id ---");
        Order o4 = new Order(E);
        o4.addProduct(P1, 1);
        o4.addProduct(P2, 1);
        o4.removeProduct("P1");
        check("позиция удалена", o4.getItems().size() == 1 && o4.getItems().get(0).getProduct().getId().equals("P2"));
        expectIae("удаление отсутствующей позиции", () -> o4.removeProduct("P1"));
        expectIae("удаление: null id", () -> o4.removeProduct(null));
        expectIae("удаление: пустой id", () -> o4.removeProduct(" "));

        System.out.println("--- total вычисляется, а не хранится ---");
        Order o5 = new Order(E);
        check("total пустого заказа = 0", o5.total() == 0.0);
        o5.addProduct(P1, 1);
        o5.addProduct(P2, 2);
        check("total = 25000 + 2 * 1500 = 28000", Math.abs(o5.total() - 28000.0) < 1e-9);
        o5.removeProduct("P2");
        check("total пересчитан после удаления: 25000", Math.abs(o5.total() - 25000.0) < 1e-9);
        boolean hasTotalField = false;
        for (Field f : Order.class.getDeclaredFields()) {
            if (f.getName().toLowerCase().contains("total") || f.getType() == double.class) {
                hasTotalField = true;
            }
        }
        check("в Order нет поля для итоговой суммы (рефлексия)", !hasTotalField);

        System.out.println("--- Запрет изменения после confirm ---");
        Order o6 = new Order(E);
        o6.addProduct(P1, 1);
        o6.confirm();
        check("статус CONFIRMED", o6.getStatus() == OrderStatus.CONFIRMED);
        expectIse("addProduct после confirm", () -> o6.addProduct(P2, 1));
        expectIse("removeProduct после confirm", () -> o6.removeProduct("P1"));
        expectIse("повторный confirm", o6::confirm);
        check("состав не изменился после отказов", o6.getItems().size() == 1);
        expectIse("confirm пустого заказа", () -> new Order(E).confirm());

        System.out.println("--- Инкапсуляция коллекции ---");
        Order o7 = new Order(E);
        o7.addProduct(P1, 1);
        expectUnsupported("getItems().add недопустим", () -> o7.getItems().add(new OrderItem(P2, 1)));
        expectUnsupported("getItems().clear недопустим", () -> o7.getItems().clear());
        expectUnsupported("getItems().remove недопустим", () -> o7.getItems().remove(0));
        boolean mutableItemApi = false;
        for (java.lang.reflect.Method m : OrderItem.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("set")) {
                mutableItemApi = true;
            }
        }
        check("у OrderItem нет public setter (позиция неизменяема)", !mutableItemApi);
        OrderItem before = o7.getItems().get(0);
        before.increase(5);
        check("increase возвращает новую позицию, исходная не изменилась", before.getQuantity() == 1);

        System.out.println("--- Лимит общей суммы (вариант 5: 30 000,00) ---");
        OrderService s = new OrderService();
        check("createOrder создаёт пустой заказ в DRAFT", s.createOrder(E).getStatus() == OrderStatus.DRAFT);
        expectIae("createOrder: null сотрудник", () -> s.createOrder(null));
        expectIae("validateForConfirm: null заказ", () -> s.validateForConfirm(null));
        Order l1 = limitOrder(30000.00);
        s.validateForConfirm(l1);
        check("сумма ровно 30000.00 допустима", true);
        s.confirmOrder(l1);
        check("заказ с суммой 30000.00 подтверждён", l1.getStatus() == OrderStatus.CONFIRMED);
        Order l2 = limitOrder(30000.01);
        expectIse("сумма 30000.01 отклонена", () -> s.confirmOrder(l2));
        check("после отказа заказ остался DRAFT", l2.getStatus() == OrderStatus.DRAFT);
        Order l3 = limitOrder(29999.99);
        s.confirmOrder(l3);
        check("сумма 29999.99 допустима", l3.getStatus() == OrderStatus.CONFIRMED);
        l2.removeProduct("L");
        l2.addProduct(new Product("L", "Лимит", 29999.99), 1);
        s.confirmOrder(l2);
        check("после уменьшения суммы заказ можно подтвердить", l2.getStatus() == OrderStatus.CONFIRMED);
        expectIse("validateForConfirm: пустой заказ", () -> s.validateForConfirm(new Order(E)));
        expectIse("validateForConfirm: уже подтверждён", () -> s.validateForConfirm(l1));
        Order l4 = new Order(E);
        l4.addProduct(new Product("C", "Копейки", 0.1), 100);
        s.confirmOrder(l4);
        check("погрешность double не влияет (100 x 0.10 = 10.00)", l4.getStatus() == OrderStatus.CONFIRMED);

        System.out.println("--- Сценарии (ScenarioRunner) ---");
        ScenarioRunner r = new ScenarioRunner();
        scenario(r, "типовой сценарий", "+P1x1|+P2x2", "OK|2|28000.00|DRAFT");
        scenario(r, "пустой сценарий", "", "OK|0|0.00|DRAFT");
        scenario(r, "null вместо сценария", null, "ERROR|IllegalArgumentException");
        scenario(r, "пустая операция", "+P1x1||+P2x1", "ERROR|IllegalArgumentException");
        scenario(r, "неизвестная операция", "ship", "ERROR|IllegalArgumentException");
        scenario(r, "пустой id товара", "+x2", "ERROR|IllegalArgumentException");
        scenario(r, "пустое количество", "+P1x", "ERROR|IllegalArgumentException");
        scenario(r, "количество не число", "+P1xabc", "ERROR|IllegalArgumentException");
        scenario(r, "лимит превышен", "+P1x1|+P3x1|confirm", "ERROR|IllegalStateException");
        scenario(r, "лимит ровно", "+P2x20|confirm", "OK|1|30000.00|CONFIRMED");

        System.out.println("--- Файл sample.csv ---");
        checkSampleCsv(r, args.length == 1 ? args[0] : "data/sample.csv");

        System.out.println();
        System.out.println("Итого: пройдено " + passed + ", не пройдено " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Заказ из одной позиции, итог которого равен amount. */
    private static Order limitOrder(double amount) {
        Order o = new Order(E);
        o.addProduct(new Product("L", "Лимит", amount), 1);
        return o;
    }

    private static void scenario(ScenarioRunner r, String name, String ops, String expected) {
        String actual = r.run(ops);
        check("сценарий: " + name + " -> " + expected, actual.startsWith(expected));
    }

    private static void checkSampleCsv(ScenarioRunner r, String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) {
            String[] c = lines.get(i).split(";", -1);
            String actual = r.run(c[1]);
            check("CSV " + c[0] + " (ожидалось " + c[2] + ")", actual.startsWith(c[2]));
        }
    }

    private static void expectIae(String name, Runnable action) {
        expect(name, action, IllegalArgumentException.class);
    }

    private static void expectIse(String name, Runnable action) {
        expect(name, action, IllegalStateException.class);
    }

    private static void expectUnsupported(String name, Runnable action) {
        expect(name, action, UnsupportedOperationException.class);
    }

    private static void expect(String name, Runnable action, Class<? extends RuntimeException> type) {
        try {
            action.run();
            check(name + " (исключение не возникло)", false);
        } catch (RuntimeException e) {
            check(name + (e.getClass() == type ? "" : " (получено " + e.getClass().getSimpleName() + ")"),
                    e.getClass() == type);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
