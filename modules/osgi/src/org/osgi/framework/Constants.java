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

package org.osgi.framework;

import java.util.EventListener;

/**
 * Represents the constants
 */
public interface Constants
{
  public static final String SYSTEM_BUNDLE_LOCATION
    = "System Bundle";
  
  public static final String SYSTEM_BUNDLE_SYMBOLICNAME
    = "system.bundle";
  
  public static final String BUNDLE_CATEGORY
    = "Bundle-Category";
  
  public static final String BUNDLE_CLASSPATH
    = "Bundle-ClassPath";
  
  public static final String BUNDLE_COPYRIGHT
    = "Bundle-Copyright";
  
  public static final String BUNDLE_DESCRIPTION
    = "Bundle-Description";
  
  public static final String BUNDLE_NAME
    = "Bundle-Name";
  
  public static final String BUNDLE_NATIVECODE
    = "Bundle-NativeCode";
  
  public static final String EXPORT_PACKAGE
    = "Export-Package";

  /**
   * @deprecated
   */
  public static final String EXPORT_SERVICE
    = "Export-Service";
  
  public static final String IMPORT_PACKAGE
    = "Import-Package";
  
  public static final String DYNAMICIMPORT_PACKAGE
    = "DynamicImport-Package";

  /**
   * @deprecated
   */
  public static final String IMPORT_SERVICE
    = "Import-Service";
  
  public static final String BUNDLE_VENDOR
    = "Bundle-Vendor";
  
  public static final String BUNDLE_VERSION
    = "Bundle-Version";
  
  public static final String BUNDLE_DOCURL
    = "Bundle-DocURL";
  
  public static final String BUNDLE_CONTACTADDRESS
    = "Bundle-ContactAddress";
  
  public static final String BUNDLE_ACTIVATOR
    = "Bundle-Activator";
  
  public static final String BUNDLE_UPDATELOCATION
    = "Bundle-UpdateLocation";
  
  public static final String PACKAGE_SPECIFICATION_VERSION
    = "specification-version";
  
  public static final String BUNDLE_NATIVECODE_PROCESSOR
    = "processor";
  
  public static final String BUNDLE_NATIVECODE_OSNAME
    = "osname";
  
  public static final String BUNDLE_NATIVECODE_OSVERSION
    = "osversion";
  
  public static final String BUNDLE_NATIVECODE_LANGUAGE
    = "language";
  
  public static final String BUNDLE_REQUIREDEXECUTIONENVIRONMENT
    = "Bundle-RequiredExecutionEnvironment";
  
  public static final String FRAMEWORK_VERSION
    = "org.osgi.framework.version";
  
  public static final String FRAMEWORK_VENDOR
    = "org.osgi.framework.vendor";
  
  public static final String FRAMEWORK_LANGUAGE
    = "org.osgi.framework.language";
  
  public static final String FRAMEWORK_OS_NAME
    = "org.osgi.framework.os.name";
  
  public static final String FRAMEWORK_OS_VERSION
    = "org.osgi.framework.os.version";
  
  public static final String FRAMEWORK_PROCESSOR
    = "org.osgi.framework.processor";
  
  public static final String FRAMEWORK_EXECUTIONENVIRONMENT
    = "org.osgi.framework.executionenvironment";
  
  public static final String FRAMEWORK_BOOTDELEGATION
    = "org.osgi.framework.bootdelegation";
  
  public static final String FRAMEWORK_SYSTEMPACKAGES
    = "org.osgi.framework.system.packages";
  
  public static final String SUPPORTS_FRAMEWORK_EXTENSION
    = "org.osgi.supports.framework.extension";
  
  public static final String SUPPORTS_BOOTCLASSPATH_EXTENSION
    = "org.osgi.supports.bootclasspath.extension";
  
  public static final String SUPPORTS_FRAMEWORK_FRAGMENT
    = "org.osgi.supports.framework.fragment";
  
  public static final String SUPPORTS_FRAMEWORK_REQUIREBUNDLE
    = "org.osgi.supports.framework.requirebundle";
  
  public static final String OBJECTCLASS
    = "objectClass";
  
  public static final String SERVICE_ID
    = "service.id";
  
  public static final String SERVICE_PID
    = "service.pid";
  
  public static final String SERVICE_RANKING
    = "service.ranking";
  
  public static final String SERVICE_VENDOR
    = "service.vendor";
  
  public static final String SERVICE_DESCRIPTION
    = "service.description";
  
  public static final String BUNDLE_SYMBOLICNAME
    = "Bundle-SymbolicName";
  
  public static final String SINGLETON_DIRECTIVE
    = "singleton";
  
  public static final String FRAGMENT_ATTACHMENT_DIRECTIVE
    = "fragment-attachment";
  
  public static final String FRAGMENT_ATTACHMENT_ALWAYS
    = "always";
  
  public static final String FRAGMENT_ATTACHMENT_RESOLVETIME
    = "resolve-time";
  
  public static final String FRAGMENT_ATTACHMENT_NEVER
    = "never";
  
  public static final String BUNDLE_LOCALIZATION
    = "Bundle-Localization";
  
  public static final String BUNDLE_LOCALIZATION_DEFAULT_BASENAME
    = "OSGI-INF/l10n/bundle";
  
  public static final String REQUIRE_BUNDLE
    = "Require-Bundle";
  
  public static final String BUNDLE_VERSION_ATTRIBUTE
    = "bundle-version";
  
  public static final String FRAGMENT_HOST
    = "Fragment-Host";
  
  public static final String SELECTION_FILTER_ATTRIBUTE
    = "selection-filter";
  
  public static final String BUNDLE_MANIFESTVERSION
    = "Bundle-ManifestVersion";
  
  public static final String VERSION_ATTRIBUTE
    = "version";
  
  public static final String BUNDLE_SYMBOLICNAME_ATTRIBUTE
    = "bundle-symbolic-name";
  
  public static final String RESOLUTION_DIRECTIVE
    = "resolution";
  
  public static final String RESOLUTION_MANDATORY
    = "mandatory";
  
  public static final String RESOLUTION_OPTIONAL
    = "optional";
  
  public static final String USES_DIRECTIVE
    = "uses";
  
  public static final String INCLUDE_DIRECTIVE
    = "include";
  
  public static final String EXCLUDE_DIRECTIVE
    = "exclude";
  
  public static final String MANDATORY_DIRECTIVE
    = "mandatory";
  
  public static final String VISIBILITY_DIRECTIVE
    = "visibility";
  
  public static final String VISIBILITY_PRIVATE_DIRECTIVE
    = "private";
  
  public static final String VISIBILITY_REEXPORT_DIRECTIVE
    = "reexport";
  
  public static final String EXTENSION_DIRECTIVE
    = "extension";
  
  public static final String EXTENSION_FRAMEWORK
    = "framework";
  
  public static final String EXTENSION_BOOTCLASSPATH
    = "bootclasspath";
  
  public static final String BUNDLE_ACTIVATIONPOLICY
    = "Bundle-ActivationPolicy";
  
  public static final String ACTIVATION_LAZY
    = "lazy";
}
