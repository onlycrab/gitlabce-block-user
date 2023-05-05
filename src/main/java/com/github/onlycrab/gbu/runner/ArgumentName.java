package com.github.onlycrab.gbu.runner;

/**
 * Names of all available arguments.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
class ArgumentName {
    /**
     * Short names of all available arguments.
     */
    public static class Short {
        public static final String GIT_ADDRESS = "g";
        public static final String GIT_CERT = "crt";
        public static final String GIT_TOKEN = "t";
        public static final String GIT_ONLY_IDENTITIES = "oi";
        public static final String GIT_EXCLUDE = "ex";
        public static final String GIT_USER_TEMPLATE = "ut";
        public static final String GIT_TIMEOUT = "to";
        public static final String AD_PROVIDER = "adp";
        public static final String AD_USER = "adu";
        public static final String AD_PASSWORD = "adc";
        public static final String AD_SEARCH = "ads";
        public static final String PROD_MODE = "pm";
        public static final String EXTERNAL_FILE = "ef";
        public static final String VERSION = "v";
    }

    /**
     * Long names of all available arguments.
     */
    @SuppressWarnings("unused")
    public static class Full {
        public static final String GIT_ADDRESS = "git";
        public static final String GIT_CERT = "certificate";
        public static final String GIT_TOKEN = "token";
        public static final String GIT_ONLY_IDENTITIES = "only-identities";
        public static final String GIT_EXCLUDE = "exclude";
        public static final String GIT_USER_TEMPLATE = "user-template";
        public static final String GIT_TIMEOUT = "timeout";
        public static final String AD_PROVIDER = "ad-provider";
        public static final String AD_USER = "ad-user";
        public static final String AD_PASSWORD = "ad-credentials";
        public static final String AD_SEARCH = "ad-search";
        public static final String PROD_MODE = "prod-mode";
        public static final String EXTERNAL_FILE = "external-file";
        public static final String VERSION = "version";
    }
}
