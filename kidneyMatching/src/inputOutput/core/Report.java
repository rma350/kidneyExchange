package inputOutput.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.common.base.Predicate;

public class Report<V> {

  private boolean useTitles;
  private List<String> titles;
  private List<AttributeConverter<V>> converters;

  public Report(List<AttributeConverter<V>> converters) {
    this(null, converters);
  }

  public Report(List<String> titles, List<AttributeConverter<V>> converters) {
    this.titles = titles;
    this.converters = converters;
    this.useTitles = titles != null;
    if (useTitles && titles.size() != converters.size()) {
      throw new RuntimeException();
    }
  }

  public void writeReport(Iterable<V> entries, Predicate<V> predicate,
      String fileName) {
    writeReport(entries, predicate, fileName, CSVFormat.EXCEL);
  }

  public void writeReport(Iterable<V> entries, Predicate<V> predicate,
      String fileName, CSVFormat format) {

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      CSVPrinter printer = new CSVPrinter(writer, format);
      if (this.useTitles) {
        printer.printRecord(this.titles.toArray(new String[titles.size()]));
      }
      for (V entry : entries) {
        if (predicate.apply(entry)) {
          List<String> nextLine = new ArrayList<String>();
          for (AttributeConverter<V> converter : converters) {
            nextLine.add(converter.apply(entry));
          }
          printer.printRecord(nextLine.toArray(new String[this.converters.size()]));
        }
      }
      printer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
