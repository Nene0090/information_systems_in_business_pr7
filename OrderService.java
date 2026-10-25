/**
 * Сервисный слой: сценарии работы с заказами и бизнес-ограничения, которые не являются
 * инвариантами самого заказа. Здесь же сосредоточен лимит общей суммы (вариант 5).
 */
public class OrderService {

    /**
     * Вариант 5 ("лимит общей суммы", значение 30): итоговая сумма заказа не может превышать
     * 30 000,00. Сумма ровно 30 000,00 допустима. Это единственное место с порогом.
     */
    private static final double MAX_ORDER_TOTAL = 30_000.00;

    public Order createOrder(Employee employee) {
        return new Order(employee);
    }

    /**
     * Проверка ограничений перед подтверждением: заказ не подтверждён, не пуст, сумма не больше лимита.
     * Заказ при проверке не изменяется.
     *
     * @throws IllegalStateException если подтверждать нельзя
     */
    public void validateForConfirm(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Заказ не задан");
        }
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Заказ уже подтверждён");
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Нельзя подтвердить пустой заказ");
        }
        // сравнение по копейкам, чтобы погрешность double не влияла на границу лимита
        if (Math.round(order.total() * 100) > Math.round(MAX_ORDER_TOTAL * 100)) {
            throw new IllegalStateException(String.format(java.util.Locale.ROOT,
                    "Сумма заказа %.2f превышает лимит %.2f", order.total(), MAX_ORDER_TOTAL));
        }
    }

    /** Сценарий подтверждения: сначала проверка ограничений, затем смена состояния. */
    public void confirmOrder(Order order) {
        validateForConfirm(order);
        order.confirm();
    }
}
