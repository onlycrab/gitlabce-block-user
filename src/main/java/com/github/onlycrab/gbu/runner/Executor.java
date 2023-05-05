package com.github.onlycrab.gbu.runner;

import com.github.onlycrab.argParser.arguments.Argument;
import com.github.onlycrab.argParser.arguments.ArgumentParser;
import com.github.onlycrab.argParser.arguments.ArgumentStorage;
import com.github.onlycrab.argParser.arguments.exceptions.ArgumentException;
import com.github.onlycrab.gbu.worker.Worker;
import com.github.onlycrab.common.SimpleIniOper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
/**
 * The class that executes console commands.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
public class Executor {
    private static final Logger LOGGER = LogManager.getLogger(Executor.class);

    /**
     * Project title.
     */
    private static final String TITLE = "GitlabCE block by AD user state";

    /**
     * Arguments data resource name.
     */
    private static final String ARG_DATA = "Arguments.xml";

    /**
     * Storage for arguments data.
     */
    private ArgumentStorage storage = null;

    /**
     * Returns error message as JSON.
     *
     * @param msg message for printing
     * @return error message as JSON
     */
    static String printError(String msg) {
        if (msg != null) {
            return String.format("{\"error\" : \"%s\"}", msg);
        } else {
            return "{\"error\" : \"null\"}";
        }
    }

    /**
     * Returns info about project and version.
     *
     * @return info about project and version
     */
    private String getProjectVersion(){
        try {
            return String.format("%s (%s) version \"%s\"",
                    TITLE,
                    this.getClass().getPackage().getImplementationTitle(),
                    this.getClass().getPackage().getImplementationVersion()
            );
        } catch (Exception e) {
            return String.format("%s version \"unknown\" (error:%s)", TITLE, e.getMessage());
        }
    }

    /**
     * Initialize executor: create argument data storage and parse console args.
     *
     * @param args console arguments
     * @return help text about project or system error message, or {@code null} if no help argument found
     */
    private String init(String[] args) {
        String errText;

        //Create argument storage
        try {
            storage = new ArgumentStorage();
            storage.read(storage.getClass().getClassLoader().getResourceAsStream(ARG_DATA), null);
        } catch (ArgumentException | XMLStreamException | IOException e) {
            errText = "System error : cant read internal config <Arguments.xml>.";
            LOGGER.fatal(errText, e);
            return printError(errText);
        }

        //Print help if no input arguments
        if (args == null) {
            return storage.getHelp();
        } else if (args.length == 0) {
            return storage.getHelp();
        } else if (storage.isDeclared(ArgumentName.Short.VERSION)){
            return getProjectVersion();
        }

        //Parse arguments values to storage
        ArgumentParser.parse(storage, args);

        //Read additional argument values from file if needed
        Argument externalFile = storage.getArgument(ArgumentName.Short.EXTERNAL_FILE);
        if (externalFile.isFilled()) {
            try {
                ArgumentParser.parse(storage, SimpleIniOper.read(externalFile.getValue(), true, null));
            } catch (ArgumentException | IOException e) {
                errText = String.format("Cant read arguments from external file <%s>.", externalFile.getValue());
                LOGGER.fatal(errText, e);
                return printError(errText);
            }
        }

        //Print help if needed
        String help = storage.getSystemHelp(ArgumentStorage.HELP_SHORT_NAME);
        if (help != null) {
            return help;
        } else {
            help = storage.getSystemHelp(ArgumentStorage.HELP_LONG_NAME);
            if (help != null) {
                return help;
            } else if (storage.isDeclared(ArgumentName.Short.VERSION)){
                return getProjectVersion();
            }
        }

        //Check conditions
        if (!storage.isRequireFilled()) {
            errText = storage.getMessage();
            LOGGER.fatal(errText);
            return printError(errText);
        }
        if (storage.isConflict()) {
            errText = storage.getMessage();
            LOGGER.fatal(errText);
            return printError(errText);
        }

        return null;
    }

    /**
     * Execute console command.
     *
     * @param args console arguments
     * @return JSON result of command execution
     */
    public String execute(String[] args) {
        String help = init(args);
        if (help != null){
            return help;
        }

        try {
            Worker worker = new Worker(
                    storage.getValue(ArgumentName.Short.GIT_ADDRESS),
                    storage.getValue(ArgumentName.Short.GIT_TOKEN),
                    storage.isFilled(ArgumentName.Short.GIT_CERT) ? storage.getValue(ArgumentName.Short.GIT_CERT) : null,
                    storage.getValue(ArgumentName.Short.GIT_EXCLUDE),
                    storage.getValue(ArgumentName.Short.GIT_USER_TEMPLATE),
                    Boolean.valueOf(storage.getValue(ArgumentName.Short.GIT_ONLY_IDENTITIES)),
                    storage.getValue(ArgumentName.Short.GIT_TIMEOUT),
                    storage.getValue(ArgumentName.Short.AD_PROVIDER),
                    storage.getValue(ArgumentName.Short.AD_USER),
                    storage.getValue(ArgumentName.Short.AD_PASSWORD),
                    storage.getValue(ArgumentName.Short.AD_SEARCH),
                    Boolean.valueOf(storage.getValue(ArgumentName.Short.PROD_MODE))
            );
            String result = worker.processGitUsers();
            LOGGER.info(result);
            return result;
        } catch (Exception e) {
            String errText = String.format("Main processing error : %s.", e.getMessage());
            LOGGER.fatal(errText, e);
            return printError(errText);
        }
    }
}
