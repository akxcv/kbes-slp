import java.io.File;
import java.util.Scanner;
import java.util.Locale;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;

public class Main {

    public static void main(String[] args) throws Exception {
		// Разбор CLI
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		options.addOption( OptionBuilder.withLongOpt("input")
										.hasArg()
										.withDescription("входной файл")
										.create("i") );
		options.addOption( OptionBuilder.withLongOpt("output")
										.hasArg()
										.withDescription("выходной файл")
										.create("o") );
        options.addOption( OptionBuilder.withLongOpt("verbose")
                                        .withDescription("вербальный режим")
                                        .create("v") );
        options.addOption( OptionBuilder.withLongOpt("integer")
                                        .withDescription("округление ответа до целых чисел")
                                        .create() );

		CommandLine line = parser.parse(options, args);

		String input, output;

		// Имя входного файла
		if (line.hasOption("input"))
            input = line.getOptionValue("input");
		else
			input = null;

        output = perform(input, line.hasOption("verbose"), line.hasOption("integer"));

	    // Вывод
		if (line.hasOption("output")) {
			try( PrintWriter out = new PrintWriter(line.getOptionValue("output")) ) {
				out.println(output);
			}
		} else {
			System.out.println(output);
		}
    }

	public static String perform(String inputFileName, boolean verbose, boolean integer) {
		try {
            SimplexTable simplexTable = new SimplexTable(createSimplexTable(inputFileName));

            String states = verbose ? highlight("Исходная таблица: ") + "\n" + simplexTable + "\n" : "";
            Answer answer = solve(simplexTable);

            if (verbose) {
                ArrayList<SimplexTable> stateList = simplexTable.getStateList();

                int stateNumber = 1;
                for (SimplexTable state : stateList) {
                    states += highlight("Шаг " + stateNumber) + "\n" + state + "\n";
                    stateNumber++;
                }
            }

	       	return highlight("Задача") + "\n" +
                    simplexTable.problemString() + "\n" +
                    states +
                    highlight("Решение") + "\n" +
                    answer.toString(integer);
		} catch (IOException e) {
			return "Файл не найден: " + inputFileName;
		}
	}

    public static String perform(String inputFileName) {
        return perform(inputFileName, false, false);
    }

    public static Answer solve(SimplexTable simplexTable) {
        int cols = simplexTable.cols();
        int rows = simplexTable.rows();
        int resCol, resRow;
        int [] rowNames = new int[rows];
        double [] solution = new double[rows];
        boolean solved = false;
        Answer answer = new Answer();

        for (int j = 0; j < rows; j++)
            rowNames[j] = cols + j + 1;

        while (!solved) {
            resCol = simplexTable.findResCol();
            if (resCol == -1) {
                return new NullAnswer();
            } else if (resCol == 0) {
                solved = true;
            } else {
                resRow = simplexTable.findResRow(resCol);
                rowNames[resRow] = resCol;
                simplexTable.step(resRow, resCol);
            }
        }

        for (int i = 1; i <= cols; i++) {
            int j = 0;
            for (; j < rows && rowNames[j] != i; j++);
            if (j == rows)
                answer.addItem("x" + i, 0);
            else
                answer.addItem("x" + i, simplexTable.getElement(j, 0));
        }

        answer.addItem("max{ F(x) }", simplexTable.getElement(rows, 0));

		return answer;
    }

    public static double[][] createSimplexTable(String fileName) throws IOException {

        Scanner scanner;

        if (fileName == null) {
            scanner = new Scanner(System.in).useLocale(new Locale("US"));
        } else {
            scanner = new Scanner(new File(fileName)).useLocale(new Locale("US"));
        }

        if (fileName == null) System.out.print("Количество переменных: ");
        int cols = scanner.nextInt();

        if (fileName == null) System.out.print("Количество ограничений: ");
        int rows = scanner.nextInt();

        double [][] simplexTable = new double[rows + 1][cols + 1];

        simplexTable[rows][0] = 0;
        if (fileName == null) System.out.println(highlight("\nЦелевая функция"));
        for (int i = 1; i <= cols; ++i) {
            if (fileName == null) System.out.print("  Коэффициент #" + i + ": ");
            simplexTable[rows][i] = -scanner.nextDouble();
        }
        if (fileName == null) System.out.println(highlight("\nКоэффициенты ограничений"));
        for (int i = 0; i < rows; ++i) {
            if (fileName == null) System.out.println("  Ограничение #" + (i + 1) + ":");
            for (int j = 1; j <= cols; ++j) {
                if (fileName == null) System.out.print("    Коэффициент при x" + j + ": ");
                simplexTable[i][j] = scanner.nextDouble();
            }
        }
        if (fileName == null) System.out.println(highlight("\nСвободные члены ограничений"));
        for (int i = 0; i < rows; ++i) {
            if (fileName == null) System.out.print("  Ограничение #" + (i+1) + ": ");
            simplexTable[i][0] = scanner.nextDouble();
        }

        if (fileName == null) System.out.println("");
        return simplexTable;

    }

    private static String highlight(String line) {
        return "\033[7m" + line + "\033[0m";
    }

}
