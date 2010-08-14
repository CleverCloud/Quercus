/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.security;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.server.dispatch.ServletConfigException;
import com.caucho.server.security.CachingPrincipal;
import com.caucho.util.L10N;

/**
 * An authenticator using JDBC.
 *
 * <p>The default table schema looks something like:
 * <pre>
 * CREATE TABLE LOGIN (
 *   username VARCHAR(250) NOT NULL,
 *   password VARCHAR(250),
 *   cookie VARCHAR(250),
 *   PRIMARY KEY (username)
 * );
 * </pre>
 *
 * <code><pre>
 * &lt;security:DatabaseAuthenticator data-source="jdbc/user"/>
 * </pre></code>
 */

@SuppressWarnings("serial")
public class DatabaseAuthenticator extends AbstractCookieAuthenticator {
  private static final Logger log
    = Logger.getLogger(DatabaseAuthenticator.class.getName());
  private static final L10N L = new L10N(DatabaseAuthenticator.class);
  
  private DataSource _dataSource;

  private String _passwordQuery = "SELECT password FROM LOGIN WHERE username=?";

  private String _cookieUpdate = "UPDATE LOGIN SET cookie=? WHERE username=?";
  
  private String _cookieQuery = "SELECT username FROM LOGIN where cookie=?";
  private boolean _cookieLogout;
  
  private String _roleQuery;
  
  protected boolean _useCookie;
  
  private String _authCookieName = "resinauthid";
  
  protected int _cookieVersion = -1;
  protected String _cookieDomain;
  protected long _cookieMaxAge = 365L * 24L * 3600L * 1000L;
  
  public DatabaseAuthenticator()
  {
  }
    
  /**
   * Gets the database
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the database pool name.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the password query.
   *
   * <p>Example:
   * <pre><code>
   * SELECT password FROM LOGIN WHERE username=?
   * </code></pre>
   */
  public String getPasswordQuery()
  {
    return _passwordQuery;
  }

  /**
   * Sets the password query.
   */
  public void setPasswordQuery(String query)
  {
    _passwordQuery = query;
  }

  /**
   * Gets the cookie auth query.
   */
  public String getCookieAuthQuery()
  {
    return _cookieQuery;
  }

  /**
   * Sets the cookie auth query.
   */
  public void setCookieAuthQuery(String query)
  {
    _cookieQuery = query;
  }

  /**
   * Gets the cookie update query.
   */
  public String getCookieAuthUpdate()
  {
    return _cookieUpdate;
  }

  /**
   * Sets the cookie update query.
   */
  public void setCookieAuthUpdate(String query)
  {
    _cookieUpdate = query;
  }

  /**
   * If true, the cookie is removed on logout
   */
  public void setCookieLogout(boolean cookieLogout)
  {
    _cookieLogout = cookieLogout;
  }
  
  /**
   * Gets the role query.
   */
  public String getRoleQuery()
  {
    return _roleQuery;
  }

  /**
   * Sets the role query.
   */
  public void setRoleQuery(String query)
  {
    _roleQuery = query;
  }

  /**
   * Returns true if Resin should generate the resinauth cookie by default.
   */
  public boolean getUseCookie()
  {
    return _useCookie;
  }

  /**
   * Set true if Resin should generate the resinauth cookie by default.
   */
  public void setUseCookie(boolean useCookie)
  {
    _useCookie = useCookie;
  }

  /**
   * Returns the version for a login cookie.
   */
  public int getCookieVersion()
  {
    return _cookieVersion;
  }

  /**
   * Sets the version for a login cookie.
   */
  public void setCookieVersion(int version)
  {
    _cookieVersion = version;
  }

  /**
   * Returns the domain for a login cookie.
   */
  public String getCookieDomain()
  {
    return _cookieDomain;
  }

  /**
   * Sets the domain for a login cookie.
   */
  public void setCookieDomain(String cookieDomain)
  {
    _cookieDomain = cookieDomain;
  }

  /**
   * Returns the max-age for a login cookie.
   */
  public long getCookieMaxAge()
  {
    return _cookieMaxAge;
  }

  /**
   * Sets the max age for a login cookie.
   */
  public void setCookieMaxAge(Period cookieMaxAge)
  {
    _cookieMaxAge = cookieMaxAge.getPeriod();
  }

  /**
   * Initialize the authenticator.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    super.init();

    if (_dataSource == null)
      throw new ConfigException(L.l("JdbcAuthenticator requires a 'data-source' attribute, because it depends on a JDBC database"));

    int i = _passwordQuery.indexOf('?');
    if (i < 0)
      throw new ConfigException(L.l("'password-query' expects a parameter"));

    if (_cookieQuery != null) {
      i = _cookieQuery.indexOf('?');
      if (i < 0)
        throw new ConfigException(L.l("'cookie-auth-query' expects a parameter"));
    }
    
    if (_cookieUpdate != null) {
      i = _cookieUpdate.indexOf('?');
      if (i < 0)
        throw new ConfigException(L.l("'cookie-auth-update' expects two parameters for the cookie id and the user id."));
      int j = _cookieUpdate.indexOf('?', i + 1);
      if (j < 0)
        throw new ConfigException(L.l("'cookie-auth-update' expects two parameters"));
    }

    if (_cookieUpdate != null && _cookieQuery == null)
      throw new ServletConfigException(L.l("<cookie-auth-update> expects 'cookie-query' because both update and select queries are needed."));

    if (_roleQuery != null) {
      i = _roleQuery.indexOf('?');
      if (i < 0)
        throw new ConfigException(L.l("'role-query' expects a parameter"));
    }
  }

  /**
   * Main authenticator API.
   */
  protected Principal authenticate(Principal principal,
                                   PasswordCredentials cred,
                                   Object details)
  {
    char []password = cred.getPassword();
    char []digest = getPasswordDigest(principal.getName(), password);

    return authenticate(principal.getName(),
                        new String(digest),
                        (HttpServletRequest) details);
  }

  /**
   * Authenticates the user given the request.
   *
   * @param username the user name for the login
   * @param password the password for the login
   *
   * @return the authenticated user or null for a failure
   */
  public Principal authenticate(String username,
                                String password,
                                HttpServletRequest request)
  {
    Principal user = loginImpl(username, password);

    if (_cookieQuery == null || user == null)
      return user;

    String cookieAuth = (String) request.getAttribute("j_use_cookie_auth");
    if (cookieAuth == null)
      cookieAuth = (String) request.getParameter("j_use_cookie_auth");

    if ("true".equals(cookieAuth) || "on".equals(cookieAuth)
        || _useCookie && cookieAuth == null) {
      addAuthCookie(user, request);
    }

    return user;
  }

  /**
   * Returns the authentication cookie
   */
  @Override
  public boolean isCookieSupported(String jUseCookieAuth)
  {
    if (_cookieQuery == null)
      return false;
    else if ("false".equals(jUseCookieAuth) || "off".equals(jUseCookieAuth))
      return false;
    else if (_useCookie)
      return true;
    else
      return ("true".equals(jUseCookieAuth) || "on".equals(jUseCookieAuth));
  }
  
   /**
    * Adds a cookie to store authentication.
    */
  protected void addAuthCookie(Principal user,
                               HttpServletRequest request)
  {
    /*
    Application app = (Application) application;
    SessionManager sm = app.getSessionManager();
    String id;
       
    id = sm.createSessionId(request);
       
    if (updateCookie(user, id)) {
      Cookie cookie = new Cookie("resinauthid", id);
      cookie.setPath("/");

      if (getCookieVersion() >= 0)
        cookie.setVersion(getCookieVersion());
      else
        cookie.setVersion(sm.getCookieVersion());

      if (_cookieDomain != null)
        cookie.setDomain(_cookieDomain);
      else if (getCookieDomain() != null)
        cookie.setDomain(getCookieDomain());
      else
        cookie.setDomain(sm.getCookieDomain());
 
      if (_cookieMaxAge > 0)
        cookie.setMaxAge((int) (_cookieMaxAge / 1000L));

      response.addCookie(cookie);
    }
    */
  }

  /**
   * Authenticates the user given the request.
   *
   * @param username the user name for the login
   * @param password the password for the login
   *
   * @return the authenticated user or null for a failure
   */
  public Principal loginImpl(String username, String password)
  {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      conn = _dataSource.getConnection();
      stmt = conn.prepareStatement(_passwordQuery);

      stmt.setString(1, username);

      rs = stmt.executeQuery();
      if (! rs.next()) {
        if (log.isLoggable(Level.FINE))
          log.fine("no such user:" + username);
        
        return null;
      }
      
      String dbPassword = rs.getString(1);

      if (dbPassword != null && dbPassword.equals(password)) {
        return new CachingPrincipal(username);
      }
      else {
        if (log.isLoggable(Level.FINE))
          log.fine("mismatched password:" + username);
        
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      try {
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
      }
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException e) {
      }
      try {
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
      }
    }
  }
  
  /**
   * Returns the password for authenticators too lazy to calculate the
   * digest.
   */
  @Override
  protected PasswordUser getPasswordUser(String username)
  {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
      
    try {
      conn = _dataSource.getConnection();
      stmt = conn.prepareStatement(_passwordQuery);

      stmt.setString(1, username);

      rs = stmt.executeQuery();
      if (! rs.next()) {
        if (log.isLoggable(Level.FINE))
          log.fine("no such user:" + username);
        
        return null;
      }
      
      String dbPassword = rs.getString(1);

      return new PasswordUser(new BasicPrincipal(username),
                              dbPassword.toCharArray(),
                              false,
                              false,
                              new String[0]);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
      }
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException e) {
      }
      try {
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
      }
    }
  }

  /**
   * Authenticate based on a cookie.
   *
   * @param cookieValue the value of the resin-auth cookie
   *
   * @return the user for the cookie.
   */
  public Principal authenticateByCookie(String cookieValue)
  {
    if (_cookieQuery == null)
      return null;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    
    try {
      conn = _dataSource.getConnection();
      stmt = conn.prepareStatement(_cookieQuery);
      stmt.setString(1, cookieValue);

      rs = stmt.executeQuery();
      if (! rs.next())
        return null;
      
      String user = rs.getString(1);

      if (user != null)
        return new CachingPrincipal(user);
      else
        return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
      }
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException e) {
      }
      try {
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
      }
    }
  }

  /**
   * Associates a user with a persistent cookie.
   *
   * @param user the user for the cookie
   * @param cookieValue the value of the resin-auth cookie
   *
   * @return true if the cookie value is valid, i.e. it's unique
   */
  public boolean associateCookie(Principal user, String cookieValue)
  {
    if (_cookieUpdate == null || user == null || cookieValue == null)
      return true;
    
    Connection conn = null;
    PreparedStatement stmt = null;
    
    try {
      conn = _dataSource.getConnection();
      stmt = conn.prepareStatement(_cookieUpdate);
      stmt.setString(1, cookieValue);
      stmt.setString(2, user.getName());

      stmt.executeUpdate();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException e) {
      }
      try {
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
      }
    }

    return true;
  }

  public boolean isUserInRole(Principal principal, String role)
  {
    if (_roleQuery == null)
      return principal != null && "user".equals(role);
    else if (principal == null || role == null)
      return false;

    CachingPrincipal cachingPrincipal = null;

    if (principal instanceof CachingPrincipal) {
      cachingPrincipal = (CachingPrincipal) principal;

      Boolean isInRole = cachingPrincipal.isInRole(role);

      if (isInRole != null)
        return isInRole.equals(Boolean.TRUE);
    }

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
      
    try {
      conn = _dataSource.getConnection();
      stmt = conn.prepareStatement(_roleQuery);
      stmt.setString(1, principal.getName());

      boolean inRole = false;
      
      rs = stmt.executeQuery();
      while (rs.next()) {
        String dbRole = rs.getString(1);

        if (cachingPrincipal != null)
          cachingPrincipal.addRole(dbRole);

        if (role.equals(dbRole))
          inRole = true;
      }
      
      return inRole;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    } finally {
      try {
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
      }
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException e) {
      }
      try {
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
      }
    }
  }

  class JdbcUser implements AuthenticatedUser {
    private Principal _user;

    JdbcUser(Principal user)
    {
      _user = user;
    }

    public String getName()
    {
      return _user.getName();
    }
    
    public Principal getPrincipal()
    {
      return _user;
    }
  
    public boolean isUserInRole(String role)
    {
      return DatabaseAuthenticator.this.isUserInRole(_user, role);
    }

    public void logout()
    {
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + getName() + "]";
    }
  }
}
