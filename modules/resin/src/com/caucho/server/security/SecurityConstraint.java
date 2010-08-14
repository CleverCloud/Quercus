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

package com.caucho.server.security;

import com.caucho.server.dispatch.UrlMap;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Configuration for the security-constraint.
 */
public class SecurityConstraint {
  static final Logger log
    = Logger.getLogger(SecurityConstraint.class.getName());
  static L10N L = new L10N(SecurityConstraint.class);

  private AbstractConstraint _constraint;
  
  private ContainerConstraint _containerConstraint;
  private RoleConstraint _roleConstraint;

  private Pattern _regexp;
  private IPConstraint _oldStyleIpConstraint;

  private boolean _isFallthrough;

  private ArrayList<WebResourceCollection> _webResourceCollectionList;

  /**
   * Creates the security-constraint.
   */
  public SecurityConstraint()
  {
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the display-name.
   */
  public void setDisplayName(String displayName)
  {
  }

  public void setFallthrough(boolean isFallthrough)
  {
  }

  /**
   * Sets the url-pattern
   */
  public void addURLPattern(String pattern)
  {
    String regexpPattern = UrlMap.urlPatternToRegexpPattern(pattern);

    int flags = (CauchoSystem.isCaseInsensitive() ?
                 Pattern.CASE_INSENSITIVE :
                 0);
    try {
      _regexp = Pattern.compile(regexpPattern, flags);
    } catch (PatternSyntaxException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Adds a web-resource-collection
   */
  public void addWebResourceCollection(WebResourceCollection resource)
  {
    if (_webResourceCollectionList == null)
      _webResourceCollectionList = new ArrayList<WebResourceCollection>();

    _webResourceCollectionList.add(resource);
  }

  /**
   * Sets the role-name
   */
  public void addRoleName(String roleName)
  {
    if (_roleConstraint == null) {
      _roleConstraint = new RoleConstraint();
      addConstraint(_roleConstraint);
    }
    
    _roleConstraint.addRoleName(roleName);
  }

  /**
   * Adds the auth-constraint
   */
  public void addAuthConstraint(AuthConstraint auth)
  {
    if (_roleConstraint == null) {
      _roleConstraint = new RoleConstraint();
      addConstraint(_roleConstraint);
    }
    
    ArrayList<String> list = auth.getRoleList();

    for (int i = 0; i < list.size(); i++)
      addRoleName(list.get(i));
  }

  /**
   * Sets the user-data-constraint
   */
  public void setUserDataConstraint(UserDataConstraint constraint)
  {
    String transportGuarantee = constraint.getTransportGuarantee();

    if (transportGuarantee != null)
      addConstraint(new TransportConstraint(transportGuarantee));
  }

  /**
   * Add an ip-constraint
   */
  public void addIPConstraint(IPConstraint constraint)
  {
    if (!constraint.isOldStyle()) {
      addConstraint(constraint);
    }
    else {
      /**
       * The old style was to simply allow:
       * <security-constraint>
       *   <ip-constraint>network</ip-constraint>
       *   <ip-constraint>network</ip-constraint>
       * </security-constraint>
       *
       * which was effectively the same as using allow.  The compiliction
       * is that when the old style is used, there should be only one
       * IPConstraint object 
       */ 
      if (_oldStyleIpConstraint == null) {
        addConstraint(constraint);
        _oldStyleIpConstraint = constraint;
      }
      else {
        constraint.copyInto(_oldStyleIpConstraint);
      }
    }
  }

  /**
   * Sets a custom constraint
   */
  public void addConstraint(AbstractConstraint constraint)
  {
    if (_constraint == null)
      _constraint = constraint;
    else if (_containerConstraint == null) {
      _containerConstraint = new ContainerConstraint();
      _containerConstraint.addConstraint(_constraint);
      _constraint = _containerConstraint;
      
      _containerConstraint.addConstraint(constraint);
    }
    else
      _containerConstraint.addConstraint(constraint);
  }

  /**
   * initialize
   */
  @PostConstruct
  public void init()
  {
  }

  /**
   * Returns true if the URL matches.
   */
  public boolean isMatch(String url)
  {
    if (_regexp != null && _regexp.matcher(url).find()) {
      return true;
    }
    
    for (int i = 0;
         _webResourceCollectionList != null
           && i < _webResourceCollectionList.size();
         i++) {
      WebResourceCollection resource = _webResourceCollectionList.get(i);

      if (resource.isMatch(url))
        return true;
    }

    return false;
  }

  /**
   * Returns true for a fallthrough.
   */
  public boolean isFallthrough()
  {
    return _isFallthrough;
  }

  /**
   * Returns the applicable methods if the URL matches.
   */
  public ArrayList<String> getMethods(String url)
  {
    for (int i = 0;
         _webResourceCollectionList != null &&
           i < _webResourceCollectionList.size();
         i++) {
      WebResourceCollection resource = _webResourceCollectionList.get(i);

      if (resource.isMatch(url))
        return resource.getMethods();
    }

    return null;
  }

  /**
   * return the constraint
   */
  public AbstractConstraint getConstraint()
  {
    return _constraint;
  }
}
