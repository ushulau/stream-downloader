package com.gplex;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * bl.rutube.ru/route
 */
@SpringBootApplication
public class StreamDownloaderApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(StreamDownloaderApplication.class);
    private static final String URL = "url";
    private static final String OUT = "out";

    public static void main(String[] args) throws Exception {

        //disabled banner, don't want to see the spring logo
        SpringApplication app = new SpringApplication(StreamDownloaderApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

    }

    private static String getUrlWithoutParameters(String url) {
        try {
            URI uri = new URI(url);
            return new URI(uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    null, // Ignore the query part of the input url
                    uri.getFragment()).toString();
        }catch (Exception e){
            logger.error("",e);
        }
        return null;
    }


    static final PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendDays()
            .appendSuffix("d ")
            .appendHours()
            .appendSuffix("h ")
            .appendMinutes()
            .appendSuffix("m ")
            .appendSeconds()
            .appendSuffix("s")
            .toFormatter();


    // Put your logic here.
    @Override
    public void run(String... args) throws Exception {
        Map<String, String> argMap = new HashMap<>();
        for (String a : args) {
            String[] spl = a.split("=", 2);
            argMap.put(spl[0].toLowerCase(), spl.length > 1 ? spl[1] : null);
        }
        if (!argMap.containsKey(URL)) {
            logger.error("url is required parameter");
            return;//exit(0);
        }

        if( StringUtils.isBlank(argMap.get(OUT))){
            argMap.put(OUT, "out.mp4");
        }
        URL url = new URL(argMap.get(URL));
        String m3u8FileName = FilenameUtils.getName(url.getPath());
        logger.info("{}", m3u8FileName);

        FileUtils.copyURLToFile(url, new File(m3u8FileName), 10000, 10000);


        FileReader fr = new FileReader(m3u8FileName);
        BufferedReader br = new BufferedReader(fr);
        String sCurrentLine;
        List<String> fragments = new ArrayList<>();
        String baseUrl = getUrlWithoutParameters(url.toString()).replace(m3u8FileName, "");
        while ((sCurrentLine = br.readLine()) != null) {
            if (sCurrentLine.endsWith(".ts")) {
                fragments.add(baseUrl + sCurrentLine);
            }
        }

        logger.info("total fragments = {} with video duration = {}", fragments.size(), formatter.print(new Duration(fragments.size() * 8000).toPeriod()));
        if(true) {
            final AtomicInteger i = new AtomicInteger(0);
            final CountDownLatch latch = new CountDownLatch(fragments.size());
            ForkJoinPool customThreadPool = new ForkJoinPool(30);
            customThreadPool.submit(
                    () -> fragments.parallelStream().forEach(f -> {

                        try {
                            Pattern pattern = Pattern.compile("(.*mp4Frag)(?<frag>\\d+)(Num)(?<num>\\d+)(.*)");
                        Matcher matcher = pattern.matcher(f);
                        matcher.find();
                        String nm  = String.format("%04d", Integer.valueOf(matcher.group("num")));
                        String fName = m3u8FileName + "_"+ nm+".ts";

                            URL u = new URL(f);
                            FileUtils.copyURLToFile(u, new File(fName), 10000, 10000);

                        } catch (Exception e) {
                            logger.error("", e);
                        } finally {
                            latch.countDown();
                        }
                        logger.info("fragment {} downloaded", f);
                    }));

            latch.await();

            FileWriter writer = new FileWriter("ts.list");
            for (String f : fragments) {
                writer.write("file '" + FilenameUtils.getName(new URL(f).getPath()) + "'\n");
            }
            writer.close();
        }
        logger.info("--- STARTING FRAGMENT MERGING ---");
        exec("ls");



        String s = null;


        //in mac oxs
       // String command = "echo Hello world!\nffmpeg -y -f concat -safe 0 -i ts.list -c copy " + argMap.get(OUT);
        String command = "cat $(ls "+m3u8FileName.replace(".m3u8","")+"*.ts) > "+ argMap.get(OUT);

        exec(command);
        //delete temp files
        if(argMap.get("d") == null || argMap.get("d").equalsIgnoreCase("true")){
            /*for (String f : fragments) {
                deleteFile(FilenameUtils.getName(new URL(f).getPath()));

            }

            deleteFile(m3u8FileName);*/
            exec("rm -f "+ m3u8FileName+"*");
            exec("rm -f ts.list");
        }



    }

    public static void exec(String command) throws Exception {
        logger.info("Executing shell command: {}",command );

        // Start the shell
        ProcessBuilder pb = new ProcessBuilder("/bin/bash");
        Process bash = pb.start();

        // Pass commands to the shell
        PrintStream ps = new PrintStream(bash.getOutputStream());
        ps.println(command);
        ps.close();

        // Get an InputStream for the stdout of the shell
        BufferedReader br = new BufferedReader(
                new InputStreamReader(bash.getInputStream()));

        BufferedReader ebr = new BufferedReader(
                new InputStreamReader(bash.getErrorStream()));

        // Retrieve and print output
        String line;
        while (null != (line = br.readLine())) {
           logger.info("> "+line);
        }
        while (null != (line = ebr.readLine())) {
            logger.error("> "+line);
        }

        br.close();
        ebr.close();

        // Make sure the shell has terminated, print out exit value
        System.out.println("Exit code: " + bash.waitFor());

    }


    private void deleteFile(String name){
        try{

            File file = new File(name);

            if(file.delete()){
                logger.info(file.getName() + " is deleted!");
            }else{
                logger.error("Delete operation is failed for {}.", file.getName());
            }

        }catch(Exception e){

            logger.error("", e);

        }
    }

}
