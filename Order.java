import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Заказ сотрудника. Хранит состав и состояние, итог (total) не хранит, а вычисляет.
 * Инварианты: один товар - одна строка; количество 1..100; после confirm состав неизменяем;
 * наружу не отдаётся изменяемая коллекция.
 * Бизнес-ограничение на сумму (лимит) здесь не хранится - оно в OrderService.
 */
public class Order {

    private final Employee employee;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;

    public Order(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Сотрудник не задан");
        }
        this.employee = employee;
    }

    /**
     * Добавляет товар. Если товар уже есть в заказе, количество объединяется в существующей строке.
     *
     * @throws IllegalStateException    заказ уже подтверждён
     * @throws IllegalArgumentException товар не задан, количество вне 1..100, объединённое количество
     *                                  больше 100 или товар с тем же id имеет другие данные (конфликт)
     */
    public void addProduct(Product product, int quantity) {
        requireDraft();
        OrderItem incoming = new OrderItem(product, quantity); // проверка товара и количества
        int index = indexOf(product.getId());
        if (index < 0) {
            items.add(incoming);
            return;
        }
        OrderItem existing = items.get(index);
        if (!existing.getProduct().equals(product)) {
            throw new IllegalArgumentException("Конфликт данных: товар с id '" + product.getId()
                    + "' уже есть в заказе с другим наименованием или ценой");
        }
        items.set(index, existing.increase(quantity)); // при ошибке заказ не меняется
    }

    /**
     * Удаляет позицию по id товара.
     *
     * @throws IllegalStateException    заказ уже подтверждён
     * @throws IllegalArgumentException id пустой или такой позиции нет
     */
    public void removeProduct(String productId) {
        requireDraft();
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор товара не задан");
        }
        int index = indexOf(productId.trim());
        if (index < 0) {
            throw new IllegalArgumentException("В заказе нет товара с id '" + productId.trim() + "'");
        }
        items.remove(index);
    }

    /** Итоговая стоимость: вычисляется из текущего состава при каждом вызове. */
    public double total() {
        double sum = 0;
        for (OrderItem item : items) {
            sum += item.getLineTotal();
        }
        return sum;
    }

    /**
     * Подтверждает заказ; дальше состав изменять нельзя.
     *
     * @throws IllegalStateException заказ уже подтверждён или пуст
     */
    public void confirm() {
        requireDraft();
        if (items.isEmpty()) {
            throw new IllegalStateException("Нельзя подтвердить пустой заказ");
        }
        status = OrderStatus.CONFIRMED;
    }

    /** Состав заказа только для чтения (позиции тоже неизменяемы). */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Employee getEmployee() {
        return employee;
    }

    private void requireDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Заказ подтверждён и не может быть изменён");
        }
    }

    private int indexOf(String productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProduct().getId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }
}
