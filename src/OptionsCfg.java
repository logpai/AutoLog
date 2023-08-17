import org.apache.commons.cli.*;

public class OptionsCfg {
    public static Options buildOptions() {
        Options options = new Options();
        options.addOption("j","jarName", true, "Jar file to contain the Log Statements");
        options.addOption("l", "LogMethods", true, "LogMethods to analyze");
        options.addOption("o", "Output", true, "Output files");
        options.addOption("h","help", false, "Print usage");
        options.addOption("m","matcher", false, "Matcher for Log API");

        return options;
    }

    public static Options LogEPOptions() {
        Options options = new Options();
        options.addOption("j","jarName", true, "Jar file to contain the Log Statements");
        options.addOption("l", "LogMethods", true, "LogMethods to analyze");
        options.addOption("o", "Output", true, "Output file");
        options.addOption("h","help", false, "Print usage");
        options.addOption("m","matcher", false, "Matcher for Log API");
        return options;
    }


    public static Options LogTempOptions() {
        Options options = new Options();
        options.addOption("j","jarName", true, "Jar file to contain the Log Statements");
        options.addOption("o", "Output", true, "Output file");
        options.addOption("h","help", false, "Print usage");
        options.addOption("m","matcher", false, "Matcher for Log API");

        return options;
    }

    public static void printUsage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("OptionsUsage", options);
    }
}
