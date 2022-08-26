package com.moesif.servlet.wrappers;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LoggingHttpServletResponseWrapper extends HttpServletResponseWrapper {
  private ServletOutputStream outputStream;
  private LoggingServletOutputStream logStream;
  private PrintWriter writer;
  private static final Logger logger = Logger.getLogger(LoggingHttpServletRequestWrapper.class.toString());

  public LoggingHttpServletResponseWrapper(HttpServletResponse response) {
    super(response);
    logger.info("LoggingHttpServletResponseWrapper construct");
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    logger.info("getOutputStream");
    if (writer != null) {
      logger.info("getOutputStream writer == null, throwing exception");
      throw new IllegalStateException("getWriter() has already been called on this response.");
    }

    if (outputStream == null) {
      logger.info("getOutputStream outputStream == null, setting value");
      outputStream = getResponse().getOutputStream();
      logStream = new LoggingServletOutputStream(outputStream);
    }

    return logStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    logger.info("getWriter");
    if (outputStream != null) {
      logger.info("getWriter outputStream != null, throwing exception");
      throw new IllegalStateException("getOutputStream() has already been called on this response.");
    }

    if (writer == null) {
      logger.info("getWriter writer == null, setting logStream and writer value");
      logStream = new LoggingServletOutputStream(getResponse().getOutputStream());
      writer = new PrintWriter(new OutputStreamWriter(logStream, getResponse().getCharacterEncoding()), true);
    }

    return writer;
  }

  @Override
  public void flushBuffer() throws IOException {
    logger.info("flushBuffer");
    if (writer != null) {
      logger.info("flushBuffer writer != null, flushing writer");
      writer.flush();
    } else if (outputStream != null) {
      logger.info("flushBuffer outputStream != null, flushing outputStream");
      logStream.flush();
    } else {
      logger.info("flushBuffer both writer and outputStream are null");
    }
  }

  public Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<String, String>(0);
    Collection<String> headerNames = getHeaderNames();

    for (String headerName: headerNames) {
      if (headerName != null) {
        if (headerName.equals("set-cookie")) {
          headers.put(headerName, getHeader(headerName));
        } else {
          headers.put(headerName, StringUtils.join(getHeaders(headerName), ","));
        }
      }
    }
    return headers;
  }

  public String getContent() {
    try {
      flushBuffer();
      if (logStream == null) {
        logger.info("getContent logStream == null, returning empty string");
        return "";
      }
      logger.info("getContent logStream != null, returning logStream value");
      String responseEncoding = getResponse().getCharacterEncoding();
      return logStream.baos.toString(responseEncoding != null ? responseEncoding : UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      logger.info("getContent UnsupportedEncodingException, returning signal string");
      return "[UNSUPPORTED ENCODING]";
    } catch (IOException e) {
      logger.info("getContent IOException, returning signal string");
      return "[IO EXCEPTION]";
    }
  }

  private class LoggingServletOutputStream extends ServletOutputStream {
    public LoggingServletOutputStream(ServletOutputStream outputStream) {

      this.outputStream = outputStream;
      this.baos = new ByteArrayOutputStream(1024);
    }

    private ServletOutputStream outputStream;
    private ByteArrayOutputStream baos;

    @Override
    public boolean isReady() {
      boolean ready = outputStream.isReady();
      logger.info("LoggingServletOutputStream isReady " + ready);
      return ready;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      logger.info("LoggingServletOutputStream setWriteListener");
      outputStream.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException {
        logger.info("LoggingServletOutputStream write " + b);
      outputStream.write(b);
      baos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      logger.info("LoggingServletOutputStream write " + Arrays.toString(b));
      outputStream.write(b);
      baos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      logger.info("LoggingServletOutputStream write with offset=" + StandardCharsets.UTF_8.decode(ByteBuffer.wrap(b, off, len)));
      outputStream.write(b, off, len);
      baos.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        logger.info("LoggingServletOutputStream flush");
      outputStream.flush();
      baos.flush();
    }

    public void close() throws IOException {
        logger.info("LoggingServletOutputStream close");
      outputStream.close();
      baos.close();
    }
  }
}
