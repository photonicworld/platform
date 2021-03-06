/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.http.server;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.encoder.EncoderBase;
import com.proofpoint.json.JsonCodec;
import com.proofpoint.log.Logging;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.LifeCycle;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

import static com.proofpoint.http.server.HttpRequestEvent.createHttpRequestEvent;

class JsonRequestLog
        implements RequestLog, LifeCycle
{
    private final CurrentTimeMillisProvider currentTimeMillisProvider;
    private final Appender<HttpRequestEvent> appender;

    JsonRequestLog(HttpServerConfig config, CurrentTimeMillisProvider currentTimeMillisProvider)
            throws IOException
    {
        this.currentTimeMillisProvider = currentTimeMillisProvider;

        appender = Logging.createFileAppender(
                config.getLogPath(),
                config.getLogMaxHistory(),
                config.getLogMaxSegmentSize(),
                config.getLogMaxTotalSize(),
                new EventEncoder(),
                new ContextBase());
    }

    @Override
    public void log(Request request, Response response)
    {
        long currentTime = currentTimeMillisProvider.getCurrentTimeMillis();
        HttpRequestEvent event = createHttpRequestEvent(request, response, currentTime);

        synchronized (appender) {
            appender.doAppend(event);
        }
    }

    @Override
    public void start()
            throws Exception
    {
    }

    @Override
    public void stop()
            throws Exception
    {
        appender.stop();
    }

    @Override
    public boolean isRunning()
    {
        return true;
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public boolean isStarting()
    {
        return false;
    }

    @Override
    public boolean isStopping()
    {
        return false;
    }

    @Override
    public boolean isStopped()
    {
        return false;
    }

    @Override
    public boolean isFailed()
    {
        return false;
    }

    @Override
    public void addLifeCycleListener(Listener listener)
    {
    }

    @Override
    public void removeLifeCycleListener(Listener listener)
    {
    }

    private static class EventEncoder extends EncoderBase<HttpRequestEvent>
    {
        private final JsonCodec<HttpRequestEvent> codec = JsonCodec.jsonCodec(HttpRequestEvent.class).withoutPretty();

        @Override
        public byte[] headerBytes()
        {
            return null;
        }

        @Override
        public byte[] encode(HttpRequestEvent event)
        {
            byte[] jsonBytes = codec.toJsonBytes(event);
            byte[] line = new byte[jsonBytes.length + 1];
            System.arraycopy(jsonBytes, 0, line, 0, jsonBytes.length);
            line[jsonBytes.length] = '\n';
            return line;
        }

        @Override
        public byte[] footerBytes()
        {
            return null;
        }
    }
}
