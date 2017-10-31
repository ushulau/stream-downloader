package com.gplex;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

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
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }


    // Put your logic here.
    @Override
    public void run(String... args) throws Exception {
        StopWatch sw = new StopWatch();
        sw.start();
        CommandLineArgs cla = new CommandLineArgs();
        cla.parceArgs(args);
        Set<String> filesToDelete = new HashSet<>();

        logger.debug("{}", cla.url);

        URL url = new URL(cla.url);
        String playlistFileName = FilenameUtils.getName(url.getPath());
        logger.info("{}", playlistFileName);

        FileUtils.copyURLToFile(url, new File(playlistFileName), 10000, 10000);
        filesToDelete.add(playlistFileName);

        FileReader fr = new FileReader(playlistFileName);
        BufferedReader br = new BufferedReader(fr);

        InputStream inputStream = new FileInputStream(new File(playlistFileName));
        PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
        Playlist pl = parser.parse();
        MediaPlaylist mpl = null;
        String baseUrl = getUrlWithoutParameters(url.toString()).replace(playlistFileName, "");
        if (pl.hasMasterPlaylist()) {
            MasterPlaylist masterPlayList = pl.getMasterPlaylist();
            List<PlaylistData> playLists = masterPlayList.getPlaylists();
            PlaylistData playlistData = null;
            logger.debug("Available bitrate");
            for (PlaylistData playList : playLists) {
                logger.debug("bitrate {}\t-\tresolution {}\t-\t{}", playList.getStreamInfo().getBandwidth(), playList.getStreamInfo().getResolution() != null ? playList.getStreamInfo().getResolution().toString() : "Not available", playList.getUri());
            }
            if (cla.bitrate < playLists.size() && cla.bitrate >= 0) {
                playlistData = playLists.get(cla.bitrate);
            } else {
                playlistData = playLists.get(0);
            }


            URL playlistURL = new URL(playlistData.getUri());
            playlistFileName = FilenameUtils.getName(playlistURL.getPath());
            filesToDelete.add(playlistFileName);
            logger.info("{}", playlistFileName);

            FileUtils.copyURLToFile(playlistURL, new File(playlistFileName), 10000, 10000);
            baseUrl = getUrlWithoutParameters(playlistURL.toString()).replace(playlistFileName, "");
            inputStream = new FileInputStream(new File(playlistFileName));
            parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
            pl = parser.parse();

            logger.debug(" -> {}");


        }

        if (!pl.hasMediaPlaylist()) {
            logger.error("No playlist detected");
            return;
        }

        mpl = pl.getMediaPlaylist();

        int duration = mpl.getTargetDuration();




        logger.info("total fragments = {} with video duration = {}", mpl.getTracks().size(), Utils.formatter.print(new Duration(mpl.getTracks().size() * duration * 1000).toPeriod()));

        final AtomicInteger i = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(mpl.getTracks().size());
        final String plfn = playlistFileName;
        ForkJoinPool customThreadPool = new ForkJoinPool(50);
        final MediaPlaylist mp = mpl;
        final String fragmentBaseUrl = baseUrl;
        final Map<String, String> indexMap = new HashMap<>();
        int a = 0;
        for(TrackData tr: mp.getTracks()){
            indexMap.put(tr.getUri(), String.format("%05d", a));
            a++;
        }

        customThreadPool.submit(
                () -> mp.getTracks().parallelStream().forEach(f -> {
                    int num = i.getAndIncrement();
                    try {
                        //Pattern pattern = Pattern.compile("(.*mp4Frag)(?<frag>\\d+)(Num)(?<num>\\d+)(.*)");
                        //Matcher matcher = pattern.matcher(f.getUri());
                        //matcher.find();//Integer.valueOf(matcher.group("num")
                        String fName = plfn + "_" + indexMap.get(f.getUri()) + ".ts";

                        URL u = new URL(fragmentBaseUrl + f.getUri());
                        FileUtils.copyURLToFile(u, new File(fName), 10000, 10000);

                    } catch (Exception e) {
                        logger.error("", e);
                    } finally {
                        latch.countDown();
                    }
                    logger.info("fragment {} downloaded", f);
                }));

        latch.await();

        logger.info("--- STARTING FRAGMENT MERGING ---");
        exec("ls");


        String s = null;


        //in mac oxs
        // String command = "echo Hello world!\nffmpeg -y -f concat -safe 0 -i ts.list -c copy " + argMap.get(OUT);
        String command = "cat $(ls " + playlistFileName.replace(".m3u8", "") + "*.ts) > " + cla.out;

        exec(command);
        //delete temp files
        if (cla.deleteTemp) {
            exec("rm -f " + playlistFileName + "*");
            filesToDelete.forEach(fileName -> exec("rm -f "+ fileName));

        }

        File out = new File(cla.out);
        logger.info("\n\n[{}] -> size of {} was downloaded in {}\n\n", out.getName(), FileUtils.byteCountToDisplaySize(out.length()), Utils.formatter.print(new Duration(sw.getTime()).toPeriod()));
    }

    public static void exec(String command) {
        try {

            logger.info("Executing shell command: {}", command);


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
                logger.info("> " + line);
            }
            while (null != (line = ebr.readLine())) {
                logger.error("> " + line);
            }

            br.close();
            ebr.close();

            // Make sure the shell has terminated, print out exit value
            System.out.println("Exit code: " + bash.waitFor());
        }catch (Exception e){
            logger.error("",e);
        }
    }


    private void deleteFile(String name) {
        try {

            File file = new File(name);

            if (file.delete()) {
                logger.info(file.getName() + " is deleted!");
            } else {
                logger.error("Delete operation is failed for {}.", file.getName());
            }

        } catch (Exception e) {

            logger.error("", e);

        }
    }

}
