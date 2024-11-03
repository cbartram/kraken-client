package com.kraken.panel;


import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFormattedTextField;
import lombok.RequiredArgsConstructor;

final class UnitFormatter extends JFormattedTextField.AbstractFormatter {
	private final String units;

	UnitFormatter(String units) {
		this.units = units;
	}

	@Override
	public Object stringToValue(final String text) throws ParseException {
		final String trimmedText;

		// Using the spinner controls causes the value to have the unit on the end, so remove it
		if (text.endsWith(units)) {
			trimmedText = text.substring(0, text.length() - units.length());
		} else {
			trimmedText = text;
		}

		try {
			return Integer.valueOf(trimmedText);
		}
		catch (NumberFormatException e) {
			throw new ParseException(trimmedText + " is not an integer.", 0); // NOPMD: PreserveStackTrace
		}
	}

	@Override
	public String valueToString(final Object value) {
		return value + units;
	}
}

@RequiredArgsConstructor
final class UnitFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {
	private final String units;
	private final Map<JFormattedTextField, JFormattedTextField.AbstractFormatter> formatters = new HashMap<>();

	@Override
	public JFormattedTextField.AbstractFormatter getFormatter(final JFormattedTextField tf) {
		return formatters.computeIfAbsent(tf, (key) -> new UnitFormatter(units));
	}
}
