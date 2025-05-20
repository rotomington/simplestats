package network.roto.simplestats.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ConfigUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

    public static Map<String, Object> readJsonConfig(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> config = gson.fromJson(reader, type);
            if (config == null) {
                LOGGER.warn("Config file {} is empty or invalid, using default empty config", filePath);
                return new HashMap<>();
            }
            return config;
        } catch (IOException e) {
            LOGGER.error("Failed to read config file: {}", filePath, e);
            LOGGER.warn("Using default empty config for {}", filePath);
            return new HashMap<>();
        }
    }
} 