import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Запуск:
 *   java Main                      - обработать data/sample.csv (id;ops;expected)
 *   java Main путь/к/файлу.csv     - обработать указанный файл
 *   java Main --line "+P1x1|+P2x2|confirm"   - выполнить один сценарий
 * Каталог товаров: P1 Ноутбук 25000, P2 Мышь 1500, P3 Монитор 12000, P4 Кабель 249.99, P5 Сервер 45000.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        ScenarioRunner runner = new ScenarioRunner();
        if (args.length == 2 && args[0].equals("--line")) {
            System.out.println(runner.run(args[1]));
            return;
        }
        String path = args.length == 1 ? args[0] : "data/sample.csv";
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) { // строка 0 - заголовок
            String[] c = lines.get(i).split(";", -1);
            if (c.length < 3) {
                continue;
            }
            System.out.println(c[0] + ": " + runner.run(c[1]));
        }
    }
}
