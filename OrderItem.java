/**
 * Позиция заказа: товар и количество. Неизменяемый объект:
 * увеличение количества возвращает новую позицию, поэтому внешний код,
 * получивший позицию из заказа, не может нарушить инварианты.
 */
public class OrderItem {

    /** Минимальное количество позиции (включительно). */
    public static final int MIN_QUANTITY = 1;
    /** Максимальное количество позиции (включительно), в том числе после объединения строк. */
    public static final int MAX_QUANTITY = 100;

    private final Product product;
    private final int quantity;

    public OrderItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Товар не задан");
        }
        checkQuantity(quantity);
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    /**
     * Возвращает новую позицию с количеством quantity + amount.
     * Если сумма выходит за MAX_QUANTITY - ошибка, а не молчаливое усечение.
     */
    public OrderItem increase(int amount) {
        checkQuantity(amount); // защита и от переполнения int при сложении
        int sum = quantity + amount;
        if (sum > MAX_QUANTITY) {
            throw new IllegalArgumentException("Количество после объединения (" + sum
                    + ") превышает максимум " + MAX_QUANTITY);
        }
        return new OrderItem(product, sum);
    }

    public double getLineTotal() {
        return product.getPrice() * quantity;
    }

    private static void checkQuantity(int q) {
        if (q < MIN_QUANTITY || q > MAX_QUANTITY) {
            throw new IllegalArgumentException("Количество " + q + " вне диапазона "
                    + MIN_QUANTITY + ".." + MAX_QUANTITY);
        }
    }
}
