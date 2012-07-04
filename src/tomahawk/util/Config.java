package tomahawk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

	private final Properties prop;

	public Config(String filename) throws IOException {
		File propFile = new File(filename);
		prop = new Properties();

		if (!propFile.exists()) {
			propFile.createNewFile();

		} else {
			prop.load(new FileInputStream(propFile));
		}
	}

	public String get(ConfigKey key) {
		return prop.getProperty(key.toString(), key.defaultValue);
	}

	public int getInt(ConfigKey key) {
		String value = prop.getProperty(key.toString(), key.defaultValue);
		if (value.matches("\\d*")) {
			return Integer.parseInt(value);
		} else {
			return Integer.parseInt(key.defaultValue);
		}
	}

	public enum ConfigKey {
		uuid(java.util.UUID.randomUUID().toString()), readable_name("javaastorage"), port("50211");

		private final String defaultValue;

		private ConfigKey(String defaultValue) {
			this.defaultValue = defaultValue;
		}
	}
}
