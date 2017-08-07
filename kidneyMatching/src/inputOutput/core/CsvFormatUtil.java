package inputOutput.core;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import multiPeriod.TimeInstant;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatterBuilder;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import data.BloodType;
import data.ExchangeUnit.ExchangeUnitType;
import data.Gender;
import data.Hospital;
import data.Person;
import data.Race;

public class CsvFormatUtil {

  public static CsvFormat<Object> toStringFormat = new CsvFormat<Object>() {

    @Override
    public String apply(Object value) {
      return value.toString();
    }
  };

  public static CsvFormat<Object> toStringNullBlank = new CsvFormat<Object>() {

    @Override
    public String apply(Object value) {
      return value == null ? "" : value.toString();
    }
  };

  public static CsvFormat<DateTime> year = new CsvDateTimeFormat(
      new DateTimeFormatterBuilder().appendYear(4, 4).toFormatter());

  public static CsvFormat<Period> getYears = new CsvFormat<Period>() {

    @Override
    public String apply(Period value) {
      if (value == null) {
        return "";
      }
      return Integer.toString(value.getYears());
    }

  };

  public static CsvFormat<Boolean> yesNo = new CsvFormat<Boolean>() {

    @Override
    public String apply(Boolean value) {
      if (value == null) {
        return null;
      } else if (value.booleanValue()) {
        return "Yes";
      } else {
        return "No";
      }
    }

  };

  public static CsvFormat<ExchangeUnitType> exchangeUnitType = new CsvFormat<ExchangeUnitType>() {

    @Override
    public String apply(ExchangeUnitType value) {
      if (value == ExchangeUnitType.altruistic) {
        return "Altruistic Donor";
      } else if (value == ExchangeUnitType.paired) {
        return "Incompatible Pair";
      } else if (value == ExchangeUnitType.chip) {
        return "Chip";
      } else {

        throw new RuntimeException();
      }
    }

  };

  public static CsvFormat<ExchangeUnitType> donorType = new CsvFormat<ExchangeUnitType>() {

    @Override
    public String apply(ExchangeUnitType value) {
      if (value == ExchangeUnitType.altruistic) {
        return "Donor Non Directed";
      } else if (value == ExchangeUnitType.paired) {
        return "Donor Incompatible";
      } else {
        throw new RuntimeException();
      }
    }
  };

  public static CsvFormat<ExchangeUnitType> receiverType = new CsvFormat<ExchangeUnitType>() {

    @Override
    public String apply(ExchangeUnitType value) {
      if (value == ExchangeUnitType.paired) {
        return "Recipient";
      } else if (value == ExchangeUnitType.chip) {
        return "Recipient";
      } else {
        throw new RuntimeException();
      }
    }
  };

  public static CsvFormat<DateTime> mmddyyyyFormat = new CsvDateTimeFormat(
      new DateTimeFormatterBuilder().appendMonthOfYear(0).appendLiteral('/')
          .appendDayOfMonth(0).appendLiteral('/').appendYear(4, 4)
          .toFormatter());

  public static CsvFormat<TimeInstant<ReadableInstant>> mmddyyyyFormatTimeInstant = new CsvFormat<TimeInstant<ReadableInstant>>() {

    @Override
    public String apply(TimeInstant<ReadableInstant> value) {
      return value == null ? "" : mmddyyyyFormat.apply(new DateTime(value
          .getValue()));
    }

  };

  public static CsvFormat<Double> noDecimals = new CsvFormat<Double>() {
    private NumberFormat format = new DecimalFormat("#");

    @Override
    public String apply(Double value) {
      return format.format(value);
    }
  };

  public static CsvFormat<Double> oneDecimals = new CsvFormat<Double>() {
    private NumberFormat format = new DecimalFormat("#.0");

    @Override
    public String apply(Double value) {
      return format.format(value);
    }
  };

  public static CsvFormat<Double> twoDecimals = new CsvFormat<Double>() {
    private NumberFormat format = new DecimalFormat("#.00");

    @Override
    public String apply(Double value) {
      return format.format(value);
    }
  };

  public static CsvFormat<Double> threeDecimals = new CsvFormat<Double>() {
    private NumberFormat format = new DecimalFormat("#.000");

    @Override
    public String apply(Double value) {
      return format.format(value);
    }
  };

  public static CsvFormat<Interval> numDays = new CsvFormat<Interval>() {

    @Override
    public String apply(Interval value) {
      return value == null ? "" : Integer.toString(value.toPeriod(
          PeriodType.days()).getDays());
    }

  };

  public static CsvFormat<Gender> genderFormat = new CsvFormat<Gender>() {

    @Override
    public String apply(Gender value) {
      if (value == Gender.MALE) {
        return "M";
      } else if (value == Gender.FEMALE) {
        return "F";
      } else {
        return "";
      }
    }

  };

  public static CsvFormat<BloodType> bloodTypeFormat = new CsvFormat<BloodType>() {

    @Override
    public String apply(BloodType blood) {
      if (blood.equals(BloodType.A)) {
        return "A";
      } else if (blood.equals(BloodType.B)) {
        return "B";
      } else if (blood.equals(BloodType.AB)) {
        return "AB";
      } else if (blood.equals(BloodType.O)) {
        return "O";
      } else {
        throw new RuntimeException();
      }
    }

  };

  public static CsvFormat<Hospital> hospitalFormat = new CsvFormat<Hospital>() {

    @Override
    public String apply(Hospital value) {
      return value.getCodeName();
    }

  };

  public static CsvFormat<Race> raceFormat = new CsvFormat<Race>() {
    @Override
    public String apply(Race race) {
      if (race.equals(Race.WHITE)) {
        return "Caucasian";
      } else if (race.equals(Race.BLACK)) {
        return "Black";
      } else if (race.equals(Race.ASIAN)) {
        return "Asian";
      } else if (race.equals(Race.HISPANIC)) {
        return "Latino";
      } else if (race.equals(Race.NOT_DISCLOSED)) {
        return "Not Disclosed";
      } else if (race.equals(Race.OTHER)) {
        return "Other";
      } else if (race.equals(Race.UNKNOWN)) {
        return "";
      } else {
        throw new RuntimeException();
      }
    }
  };

  public static CsvFormat<Boolean> specialHlaFormat = new CsvFormat<Boolean>() {

    @Override
    public String apply(Boolean value) {
      if (value == null) {
        return "";
      } else if (value.booleanValue() == true) {
        return "1";
      } else {
        return "0";
      }
    }

  };

  public static CsvFormat<Boolean> excelTrueFalse = new CsvFormat<Boolean>() {
    @Override
    public String apply(Boolean value) {
      if (value == null) {
        return "";
      } else if (value.booleanValue() == true) {
        return "TRUE";
      } else {
        return "FALSE";
      }
    }
  };

  public static CsvFormat<Person> idFormat = new CsvFormat<Person>() {

    @Override
    public String apply(Person value) {
      return value.getId();
    }

  };

  public static <T> CsvFormat<List<? extends T>> makeListFormat(
      final CsvFormat<? super T> elementFormat, String delim) {
    final Joiner joiner = Joiner.on(delim);
    return new CsvFormat<List<? extends T>>() {
      @Override
      public String apply(List<? extends T> value) {
        List<String> strings = new ArrayList<String>();
        for (T t : value) {
          strings.add(elementFormat.apply(t));
        }
        return joiner.join(strings);
      }
    };
  }

  public static <S, T> CsvFormat<List<? extends S>> makeCompoundAttributeFormat(
      final Attribute<S, T> attribute,
      final CsvFormat<? super T> elementFormat, String delim) {

    final CsvFormat<List<? extends T>> finalFormat = makeListFormat(
        elementFormat, delim);

    return new CsvFormat<List<? extends S>>() {
      @Override
      public String apply(List<? extends S> value) {
        List<T> newList = new ArrayList<T>();
        for (S data : value) {
          newList.add(attribute.apply(data));
        }
        return finalFormat.apply(newList);
      }
    };
  }

  public static CsvFormat<List<? extends Person>> personListNameFormat = makeListFormat(
      idFormat, "|");

  public static CSVFormat pgfPlotFileFormat = CSVFormat.EXCEL.withQuote(
      ' ').withDelimiter(' ');
  public static CSVFormat pgfPlotTableFormat = CSVFormat.EXCEL
      .withQuote(' ');

  public static CsvFormat<StatisticalSummary> meanFormat = new CsvFormat<StatisticalSummary>() {
    @Override
    public String apply(StatisticalSummary value) {
      return Double.toString(value.getMean());
    }
  };

  public static CsvFormat<StatisticalSummary> sumFormat = new CsvFormat<StatisticalSummary>() {
    @Override
    public String apply(StatisticalSummary value) {
      return Double.toString(value.getSum());
    }
  };

  public static CsvFormat<StatisticalSummary> countFormat = new CsvFormat<StatisticalSummary>() {
    @Override
    public String apply(StatisticalSummary value) {
      // System.err.println("Count is working " + value.getN());
      return Double.toString(value.getN());
    }
  };

  public static CsvFormat<StatisticalSummary> stDevFormat = new CsvFormat<StatisticalSummary>() {
    @Override
    public String apply(StatisticalSummary value) {
      return Double.toString(value.getStandardDeviation());
    }
  };
  public static CsvFormat<StatisticalSummary> maxFormat = new CsvFormat<StatisticalSummary>() {
    @Override
    public String apply(StatisticalSummary value) {
      return Double.toString(value.getMax());
    }
  };
  public static CsvFormat<StatisticalSummary> minFormat = new CsvFormat<StatisticalSummary>() {
    @Override
    public String apply(StatisticalSummary value) {
      return Double.toString(value.getMin());
    }
  };

  public static final ImmutableMap<SumStatVal, CsvFormat<StatisticalSummary>> statisticalSummaryFormatters;
  static {
    statisticalSummaryFormatters = ImmutableMap
        .<SumStatVal, CsvFormat<StatisticalSummary>> builder()
        .put(SumStatVal.MEAN, meanFormat).put(SumStatVal.SUM, sumFormat)
        .put(SumStatVal.COUNT, countFormat).put(SumStatVal.ST_DEV, stDevFormat)
        .put(SumStatVal.MAX, maxFormat).put(SumStatVal.MIN, minFormat).build();
  }

}