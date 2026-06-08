package com.booker.util;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    public static String get(String key) {
        String value = DOTENV.get(key);
        return value != null ? value : System.getenv(key);
    }
}
