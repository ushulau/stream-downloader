package com.gplex;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

/**
 * Created by Vlad S. on 10/29/17.
 */
public class CommandLineArgs {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineArgs.class);

    @Option(name = "-str")
    String str = "(default value)";

    @Option(name = "-u", usage = "url with manifest/playlist location", metaVar = "URL")
    String url = null;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    @Option(name = "-o", usage = "file name for output", metaVar = "OUTPUT")
    String out = "out_" + dateFormat.format(new Date()) + ".mp4";


    @Option(name = "-d", handler = BooleanOptionHandler.class, usage = "delete temporary files and downloaded fragments", metaVar = "DELETE_TEMP")
    boolean deleteTemp = true;

    @Option(name = "-b", usage = "selects playlist out of master playlist in order provided", metaVar = "BITRATE")
    int bitrate = 0;

    // receives other command line parameters than options
    @Argument
    List<String> arguments = new ArrayList<String>();


    public void parceArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            logger.error(e.getMessage());
            logger.error("\n");
            parser.printUsage(System.err);
            logger.error("\n");
            logger.error("Example: " + parser.printExample(ALL));
        }
        Assert.notNull(url, "URL should be defined");

    }

}
