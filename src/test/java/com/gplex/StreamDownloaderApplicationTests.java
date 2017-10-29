package com.gplex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StreamDownloaderApplicationTests {
	private static final Logger logger = LoggerFactory.getLogger(StreamDownloaderApplicationTests.class);
	@Test
	public void contextLoads() {
	}

	@Test
	public void execute() throws  Exception {
		StreamDownloaderApplication.exec("ls");
		StreamDownloaderApplication.exec("ffmpeg -f concat -safe 0 -i ts.list -c copy result.mp4");

	}

	@Test
	public void matcher() throws  Exception {
		Pattern pattern = Pattern.compile("(.*mp4Frag)(?<frag>\\d+)(Num.*)");
		Matcher matcher = pattern.matcher("https://video-2-201.rutube.ru/hls-vod/TLorh1Nnvzwx8B649IuWvA/1509168751/128/0x5000cca255cc6fb0/7c50e95170f84cc8a10d2b60179f7cb6.mp4Frag14Num14.ts");
		while (matcher.find()){
			logger.info("----> {}", matcher.group("frag"));
		}



	}

}
