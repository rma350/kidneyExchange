package inputOutput.core;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

public class CsvDateTimeFormat extends CsvFormat<DateTime> {
	
	private DateTimeFormatter formatter;
	
	public CsvDateTimeFormat(DateTimeFormatter formatter){
		this.formatter = formatter;
	}

	@Override
	public String apply(DateTime value) {
		
		return value == null?  "" : formatter.print(value);
	}

}
