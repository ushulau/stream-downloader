package com.gplex;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Created by Vlad S. on 10/29/17.
 */
public class Utils {

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

}
