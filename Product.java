/**
 * Товар. Инварианты (проверяются в конструкторе, объект неизменяем):
 * id и наименование не пустые, цена положительная и конечная.
 * Уникальность id в рамках заказа обеспечивает Order (см. addProduct).
 */
public class Product {

    private final String id;
    private final String name;
    private final double price;

    public Product(String id, String name, double price) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор товара не задан");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Наименование товара не задано");
        }
        if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
            throw new IllegalArgumentException("Цена товара должна быть положительной: " + price);
        }
        this.id = id.trim();
        this.name = name.trim();
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    /** Товары равны, если совпадают все поля; нужно для обнаружения конфликта данных в заказе. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        Product p = (Product) o;
        return id.equals(p.id) && name.equals(p.name) && Double.compare(price, p.price) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, price);
    }

    @Override
    public String toString() {
        return id + " " + name + " (" + price + ")";
    }
}
