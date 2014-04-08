package au.org.ala.images

public class TilingAgent {

    private static final DEFAULT_SERVICE_BASE = "http://images.ala.org.au"
    private static final DEFAULT_WORK_DIRECTORY = "/data/images/tiling-agent/"
    private static final DEFAULT_WAIT_PERIOD = 10

    public static void main(String[] args) {

        def serviceBase = DEFAULT_SERVICE_BASE
        def workDirectory = DEFAULT_WORK_DIRECTORY
        def waitPeriod = DEFAULT_WAIT_PERIOD

        // process command line args

        for (int i = 0; i < args.length; ++i) {
            def arg = args[i]
            switch (arg) {
                case "-d":
                    if (i < args.length - 1) {
                        i += 1
                        workDirectory = args[i]
                    } else {
                        println "Missing argument for -d (working directory)!"
                    }
                    break;
                case "-s":
                    if (i < args.length - 1) {
                        i += 1
                        serviceBase = args[i]
                    } else {
                        println "Missing argument for -s (service base)"
                    }
                    break;
                case "-t":
                    if (i < args.length - 1) {
                        i += 1
                        waitPeriod =  Integer.parseInt(args[i])
                    } else {
                        println "Missing argument for -t (wait period)"
                    }

                    break;
                case "-?":
                    printHelp()
                    System.exit(0);
                    break
            }
        }

        Logger.log("Starting ALA Image Tiling Agent")
        Logger.log(" - Using serviceBase: ${serviceBase}")
        Logger.log(" - Using working directory: ${workDirectory}")
        Logger.log(" - Waiting period: ${waitPeriod} seconds")

        def agent = new ImageTilerThread(new File(workDirectory), serviceBase, waitPeriod)

        agent.run()

    }

    private static void printHelp() {
        println "TilerAgent [-d <workdirectory>] [-t <waitperiod>] [-s <serviceBase>]"
        println ""
        println "Where:"
        println "           <workdirectoy> is a working directory for tiling (Defaults to '${DEFAULT_WORK_DIRECTORY}')"
        println "           <waitperiod> is the number of seconds to wait before polling for jobs (Defaults to ${DEFAULT_WAIT_PERIOD})"
        println "           <servicebase> is the url base for the image service to tile for (Defaults to '${DEFAULT_SERVICE_BASE}')"

    }
}
