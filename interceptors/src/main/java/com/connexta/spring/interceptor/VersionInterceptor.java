/**
 * Copyright (c) Connexta
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package com.connexta.spring.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring interceptor capable of validating the version provided by the client with the version
 * implemented by the server and throws a {@link UnsupportedVersionException} exception if the
 * client version is not supported. It will also automatically add the <code>Content-Version</code>
 * header with the server version for the API called. This only happens in cases where the
 * controller was called and generated a response and no other interceptors failed.
 *
 * <p>The server version for the API is retrieved from the dependency.properties file that must be
 * included in the generated spring API using Apache's ServiceMix depends maven plugin.
 */
@Component
public class VersionInterceptor implements HandlerInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(VersionInterceptor.class);

  private static final String ACCEPT_VERSION = "Accept-Version";
  private static final String CONTENT_VERSION = "Content-Version";

  private static final String DEPENDENCIES_FILE = "META-INF/maven/dependencies.properties";
  private static final String VERSION = "version";

  private static final int CLIENT_VERSION_PARSING_ERROR = 501001;
  private static final int MAJOR_VERSION_TOO_OLD = 501002;
  private static final int MAJOR_VERSION_TOO_RECENT = 501003;
  private static final int MINOR_VERSION_TOO_RECENT = 501004;

  private static final Map<Method, String> VERSIONS = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (handler instanceof HandlerMethod) {
      final HandlerMethod handlerMethod = (HandlerMethod) handler;
      final String serverVersion = VersionInterceptor.getVersion(handlerMethod.getMethod());
      final String clientVersion = request.getHeader(VersionInterceptor.ACCEPT_VERSION);

      LOGGER.debug(
          "VersionInterceptor.preHandle(handler: {}, server: {}, client: {}",
          handlerMethod.getShortLogMessage(),
          serverVersion,
          clientVersion);
      if (serverVersion != null) {
        response.addHeader(VersionInterceptor.CONTENT_VERSION, serverVersion);
        VersionInterceptor.verifyCompatibility(serverVersion, clientVersion);
      }
    }
    return true;
  }

  private static void verifyCompatibility(String serverVersion, String clientVersion) {
    if (serverVersion.equals(clientVersion)) {
      return;
    }
    final String[] serverParts = serverVersion.split("\\.");
    final String[] clientParts = clientVersion.split("\\.");

    for (int i = 0; i < 2; i++) {
      final String serverPart = (serverParts.length > i) ? serverParts[i].trim() : "0";
      final String clientPart = (clientParts.length > i) ? clientParts[i].trim() : "0";

      if (serverPart.equals(clientPart)) {
        continue;
      }
      final Integer serverValue = Integer.parseInt(serverPart); // let parsing errors bubble out
      final Integer clientValue;

      try {
        clientValue = Integer.parseInt(clientPart);
      } catch (NumberFormatException e) { // if we get garbage than bail out
        LOGGER.debug("failed to parse client version: {}", clientVersion, e);
        throw new UnsupportedVersionException(
            VersionInterceptor.CLIENT_VERSION_PARSING_ERROR, clientVersion, serverVersion, e);
      }
      final int compare = serverValue.compareTo(clientValue);

      if (i == 0) { // major must be equal
        if (compare > 0) {
          throw new UnsupportedVersionException(
              VersionInterceptor.MAJOR_VERSION_TOO_OLD, clientVersion, serverVersion);
        } else if (compare < 0) {
          throw new UnsupportedVersionException(
              VersionInterceptor.MAJOR_VERSION_TOO_RECENT, clientVersion, serverVersion);
        }
      } else { // server minor must be greater or equal than client
        if (compare < 0) {
          throw new UnsupportedVersionException(
              VersionInterceptor.MINOR_VERSION_TOO_RECENT, clientVersion, serverVersion);
        }
      }
    }
  }

  /**
   * Gets the Maven project version for the specfied class.
   *
   * @param clazz the class for which to retrieve the maven project version
   * @return the corresponding maven project version or <code>null</code> if not found
   */
  @Nullable
  public static String getVersion(Class<?> clazz) {
    final Properties p = VersionInterceptor.getDependenciesFrom(clazz);

    return (p != null) ? p.getProperty(VersionInterceptor.VERSION) : null;
  }

  @Nullable
  private static String getVersion(Method method) {
    return VersionInterceptor.VERSIONS.computeIfAbsent(
        method,
        m ->
            VersionInterceptor.getVersion(
                VersionInterceptor.getParentMethodOf(m).getDeclaringClass()));
  }

  /**
   * Finds and retrieves the method that would have been overridden by the specified one. This
   * method would be either defined in the top-most class or interface.
   *
   * @param method the method for which to find a parent overridden one
   * @return the parent method <code>method</code> is overriding or <code>method</code> if none
   *     found
   */
  private static Method getParentMethodOf(Method method) {
    final Method m = VersionInterceptor.getParentMethodOf(method, method.getDeclaringClass());

    return (m != null) ? m : method;
  }

  @Nullable
  private static Method getParentMethodOf(Method method, Class<?> fromClass) {
    // start by checking all interfaces recursively up the hierarchy
    for (final Class<?> clazz : fromClass.getInterfaces()) {
      try {
        return VersionInterceptor.getParentMethodOf(
            clazz.getDeclaredMethod(method.getName(), method.getParameterTypes()));
      } catch (NoSuchMethodException e) {
        final Method m = VersionInterceptor.getParentMethodOf(method, clazz);

        if (m != null) {
          return m;
        }
      }
    }
    final Class<?> superClass = fromClass.getSuperclass();

    if (superClass != null) {
      try {
        return VersionInterceptor.getParentMethodOf(
            superClass.getDeclaredMethod(method.getName(), method.getParameterTypes()));
      } catch (NoSuchMethodException e) {
        return VersionInterceptor.getParentMethodOf(method, superClass);
      }
    }
    return null;
  }

  /**
   * Gets the information from the <code>dependencies.properties</code> file defined in the same
   * location as the specified class.
   *
   * @param clazz the class where to locate the dependencies information
   * @return the corresponding dependencies or <code>null</code> if not found
   */
  @SuppressWarnings(
      "squid:CallToDeprecatedMethod" /* perfectly acceptable to not care about errors closing the
                                     file once we have retrieved the info we want from it */)
  @Nullable
  private static Properties getDependenciesFrom(Class<?> clazz) {
    final ProtectionDomain domain = clazz.getProtectionDomain();

    if (domain != null) {
      final CodeSource codesource = domain.getCodeSource();
      InputStream is = null;

      try {
        is =
            VersionInterceptor.getResourceAsStreamFromCodeSource(
                VersionInterceptor.DEPENDENCIES_FILE, codesource);
        if (is != null) {
          final Properties dependencies = new Properties();

          dependencies.load(is);
          return dependencies;
        }
      } catch (IOException e) { // ignored
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
    return null;
  }

  /**
   * Gets an input stream for a resource defined in a given code source location.
   *
   * @param name the name of the resource to retrieve
   * @param codesource the code source from which to retrieve it
   * @return the corresponding input stream or <code>null</code> if not found
   */
  @SuppressWarnings("squid:S2095" /* the classloader is being closed by the stream itself */)
  @Nullable
  private static InputStream getResourceAsStreamFromCodeSource(
      String name, @Nullable CodeSource codesource) {
    if (codesource == null) {
      return null;
    }
    final URL location = codesource.getLocation();

    if (location != null) {
      final URLClassLoader classloader =
          new URLClassLoader(new URL[] {location}) {
            @Override
            public URL getResource(String name) {
              // don't bother checking the parent classloader as we will do that in a different step
              return super.findResource(name);
            }
          };
      final InputStream is = classloader.getResourceAsStream(name);

      if (is != null) {
        return new ProxyInputStream(is) {
          @Override
          public void close() throws IOException {
            super.close();
            classloader.close();
          }
        };
      }
    }
    return null;
  }
}
