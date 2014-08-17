package inputOutput.core;

public class CompositeAttributeConverter<T, V> implements AttributeConverter<T> {

  private Attribute<T, ? extends V> attribute;
  private CsvFormat<? super V> format;

  // private Function<T,String> composed;

  public CompositeAttributeConverter(Attribute<T, ? extends V> attribute,
      CsvFormat<? super V> format) {
    this.attribute = attribute;
    this.format = format;
    // composed = Functions.compose(format, attribute);
  }

  @Override
  public String apply(T input) {
    // return composed.apply(input);
    V value = this.attribute.apply(input);
    if (value == null) {
      return "";
    } else {
      return format.apply(value);
    }
  }

  public Attribute<T, ? extends V> getAttribute() {
    return this.attribute;
  }

}
