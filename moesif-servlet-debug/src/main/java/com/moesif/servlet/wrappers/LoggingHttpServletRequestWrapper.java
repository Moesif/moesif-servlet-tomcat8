package com.moesif.servlet.wrappers;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class LoggingHttpServletRequestWrapper extends HttpServletRequestWrapper {

  private static final Logger logger = Logger.getLogger(LoggingHttpServletRequestWrapper.class.toString());

  private static final List<String> FORM_CONTENT_TYPE = Arrays.asList("application/x-www-form-urlencoded", "multipart/form-data");

  private static final String METHOD_POST = "POST";

  private byte[] content;

  private final Map<String, String[]> parameterMap;

  private final HttpServletRequest delegate;

  public LoggingHttpServletRequestWrapper(HttpServletRequest request) {
    super(request);
    this.content = new byte[0];
    logger.info("LoggingHttpServletRequestWrapper construct");
    this.delegate = request;
    if (isFormPost()) {
      logger.info("LoggingHttpServletRequestWrapper is form post");
      this.parameterMap = request.getParameterMap();
    } else {
      logger.info("LoggingHttpServletRequestWrapper is not form post");
      this.parameterMap = Collections.emptyMap();
    }
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    if (ArrayUtils.isEmpty(content)) {
      logger.info("getInputStream content is empty");
      return delegate.getInputStream();
    }
    logger.info("getInputStream content is not empty:");
    return new LoggingServletInputStream(content);
  }

  @Override
  public BufferedReader getReader() throws IOException {
    if (ArrayUtils.isEmpty(content)) {
        logger.info("getReader content is empty");
      return delegate.getReader();
    }
    logger.info("getReader content is not empty");
    return new BufferedReader(new InputStreamReader(getInputStream()));
  }

  @Override
  public String getParameter(String name) {
    if (ArrayUtils.isEmpty(content) || this.parameterMap.isEmpty()) {
      return super.getParameter(name);
    }
    String[] values = this.parameterMap.get(name);
    if (values != null && values.length > 0) {
      return values[0];
    }
    return Arrays.toString(values);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    if (ArrayUtils.isEmpty(content) || this.parameterMap.isEmpty()) {
      return super.getParameterMap();
    }
    return this.parameterMap;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    if (ArrayUtils.isEmpty(content) || this.parameterMap.isEmpty()) {
      return super.getParameterNames();
    }
    return new ParamNameEnumeration(this.parameterMap.keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    if (ArrayUtils.isEmpty(content) || this.parameterMap.isEmpty()) {
      return super.getParameterValues(name);
    }
    return this.parameterMap.get(name);
  }

  public String getContent() {
    try {
      if (this.parameterMap.isEmpty()) {
        content = IOUtils.toByteArray(delegate.getInputStream());
        logger.info("getContent parameterMap is empty, got delegate.getInputStream. content:" + new String(content));
      } else {
        content = getContentFromParameterMap(this.parameterMap);
        logger.info("getContent parameterMap is not empty, got getContentFromParameterMap. content:" + new String(content));
      }
      String requestEncoding = delegate.getCharacterEncoding();
      logger.info("getContent requestEncoding:" + requestEncoding);
      String normalizedContent = StringUtils.normalizeSpace(new String(content, requestEncoding != null ? requestEncoding : StandardCharsets.UTF_8.name()));
      if (!Arrays.equals(content, normalizedContent.getBytes())) {
        logger.info("getContent normalizedContent not equal to content:" + normalizedContent);
      } else {
        logger.info("getContent normalizedContent is same as content");
      }
      return normalizedContent;
      // return StringUtils.isBlank(normalizedContent) ? "[EMPTY]" : normalizedContent;
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException();
    }
  }

  private byte[] getContentFromParameterMap(Map<String, String[]> parameterMap) {

    List<String> result = new ArrayList<String>();
    for (Map.Entry<String, String[]>  e: parameterMap.entrySet()) {
      String[] value = e.getValue();
      result.add(e.getKey() + "=" + (value.length == 1 ? value[0] : Arrays.toString(value)));
    }
    return StringUtils.join(result, "&").getBytes();
  }
  
  // Wrapper function to addHeader
  public Map<String, String> addHeader(String headerKey, String headerValue) {
	  Map<String, String> headers = new HashMap<String, String>(0);
	  headers = getHeaders();
	  headers.put(headerKey, headerValue);
	  // Remove header as the case is not preserved
	  headers.remove("x-moesif-transaction-id");
	  return headers;
  }

  public Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<String, String>(0);
    Enumeration<String> headerNames = getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();

      if (headerName != null) {
        headers.put(headerName, StringUtils.join(Collections.list(getHeaders(headerName)), ","));
      }
    }
    return headers;
  }

  public boolean isFormPost() {
    String contentType = getContentType();
    if (contentType != null && METHOD_POST.equalsIgnoreCase(getMethod())) {
      for (String formType: FORM_CONTENT_TYPE) {
        if (contentType.toLowerCase().contains(formType)) {
          return true;
        }
      }
    }
    return false;
  }

  private class ParamNameEnumeration implements Enumeration<String> {

    private final Iterator<String> iterator;

    private ParamNameEnumeration(Set<String> values) {
      Iterator<String> emptyIterator = Collections.emptyIterator();
      this.iterator = values != null ? values.iterator() : emptyIterator;
    }

    @Override
    public boolean hasMoreElements() {
      return iterator.hasNext();
    }

    @Override
    public String nextElement() {
      return iterator.next();
    }
  }

  private class LoggingServletInputStream extends ServletInputStream {

    private final InputStream is;

    private LoggingServletInputStream(byte[] content) {
      logger.info("LoggingServletInputStream content is " + new String(content));
      this.is = new ByteArrayInputStream(content);
    }

    @Override
    public boolean isFinished() {
      logger.info("LoggingServletInputStream isFinished");
      return true;
    }

    @Override
    public boolean isReady() {
      logger.info("LoggingServletInputStream isReady");
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      logger.info("LoggingServletInputStream setReadListener");
    }

    @Override
    public int read() throws IOException {
      logger.info("LoggingServletInputStream read");
      return this.is.read();
    }

    @Override
    public void close() throws IOException {
      super.close();
      logger.info("LoggingServletInputStream close");
      is.close();
    }
  }
}
