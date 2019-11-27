package com.jsq.gav.gles;

import com.jsq.gav.gles.filter.GlFilter;

/**
 * Created by jiang on 2019/5/10
 */

public class GlFilterConfig {

    public static final long DEFAULT_START_VALUE = -1L;
    public static final long DEFAULT_END_VALUE = 0x7fffffffffffffffL;

    public GlFilter glFilter;

    FrameBufferObject frameBufferObject;

    public long startTimeNs = DEFAULT_START_VALUE;

    public long endTimeNs = DEFAULT_END_VALUE;

    public GlFilterConfig(GlFilter glFilter) {
        this.glFilter = glFilter;
    }


    public GlFilterConfig(GlFilter glFilter, long startTimeNs) {
        this.glFilter = glFilter;
        this.startTimeNs = startTimeNs;
    }

    public GlFilterConfig(GlFilter glFilter, long startTimeNs, long endTimeNs) {
        this.glFilter = glFilter;
        this.startTimeNs = startTimeNs ;
        this.endTimeNs = endTimeNs ;
    }
}
