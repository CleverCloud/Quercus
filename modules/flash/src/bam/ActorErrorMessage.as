/*
 * Copyright (c) 2001-2009 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Emil Ong
 */

package bam
{
  [RemoteClass(alias="com.caucho.bam.ActorError")]
  public class ActorErrorMessage {
    /**
     * Retry after providing credentials
     */
    public static const TYPE_AUTH:String = "auth";

    /**
     * Do not retry, i.e. unrecoverable
     */
    public static const TYPE_CANCEL:String = "cancel";

    /**
     * proceed, i.e. this was a warning
     */
    public static const TYPE_CONTINUE:String = "continue";

    /**
     * change the request data and try again
     */
    public static const TYPE_MODIFY:String = "modify";

    /**
     * Retry after waiting
     */
    public static const TYPE_WAIT:String = "wait";

    /**
     * The remote server could not be contacted ("wait")
     */
    public static const CONNECTION_TIMEOUT:String = "connection-timeout";

    /**
     * The remote server could not be contacted ("wait")
     */
    public static const REMOTE_CONNECTION_FAILED:String 
      = "remote-connection-failed";

    /**
     * Malformed or unprocessable request, e.g. bad type.  ("cancel")
     */
    public static const BAD_REQUEST:String = "bad-request";

    /**
     * An existing resource or session already exists.  ("cancel")
     */
    public static const CONFLICT:String = "conflict";

    /**
     * The feature is nor implemented, e.g. bad query class.  ("cancel")
     */
    public static const FEATURE_NOT_IMPLEMENTED:String
      = "feature-not-implemented";

    /**
     * The requstor does not have proper authorization.  ("auth")
     */
    public static const FORBIDDEN:String = "forbidden";

    /**
     * The target or server is no longer at this address.  ("modify")
     */
    public static const GONE:String = "gone"; // xs:string

    /**
     * An internal server error ("wait")
     */
    public static const INTERNAL_SERVER_ERROR:String = "internal-server-error";

    /**
     * The target jid doesn't exist ("cancel")
     */
    public static const ITEM_NOT_FOUND:String = "item-not-found";

    /**
     * The target jid syntax is invalid ("modify")
     */
    public static const JID_MALFORMED:String = "jid-malformed";

    /**
     * The request is forbidden because of application policy, i.e. it's
     * a valid request, but not allowed ("modify")
     */
    public static const NOT_ACCEPTABLE:String = "not-acceptable";

    /**
     * The server does not any entity to perform the action ("cancel")
     */
    public static const NOT_ALLOWED:String = "not-allowed";

    /**
     * The entity does not have the proper credentials ("auth")
     */
    public static const NOT_AUTHORIZED:String = "not-authorized";

    /**
     * The entity does not have payment to the service ("auth")
     */
    public static const PAYMENT_REQUIRED:String = "payment-required";

    /**
     * The recipient exists, but is currently not attached ("wait")
     */
    public static const RECIPIENT_UNAVAILABLE:String = "recipient-unavailable";

    /**
     * The recipient is at a new address ("modify")
     */
    public static const REDIRECT:String = "redirect"; // xs:string

    /**
     * The entity does not have proper registration for the service ("auth")
     */
    public static const REGISTRATION_REQUIRED:String = "registration-required";

    /**
     * The remote server does not exist ("cancel")
     */
    public static const REMOTE_SERVER_NOT_FOUND:String
      = "remote-server-not-found";

    /**
     * The remote server could not be contacted ("wait")
     */
    public static const REMOTE_SERVER_TIMEOUT:String = "remote-server-timeout";

    /**
     * The remote service does not have resources to process 
     * the request ("wait")
     */
    public static const RESOURCE_CONSTRAINT:String = "resource-constraint";

    /**
     * The remote server does not provide the service ("cancel")
     */
    public static const SERVICE_UNAVAILABLE:String = "service-unavailable";

    /**
     * The resource required a subscription before use ("auth")
     */
    public static const SUBSCRIPTION_REQUIRED:String = "subscription-required";

    /**
     * An error outside this list (should have an _extra field)
     */
    public static const UNDEFINED_CONDITION:String = "undefined-condition";

    /**
     * The request was syntactically correct, but out-of-order ("wait")
     */
    public static const UNEXPECTED_REQUEST:String = "unexpected-request";

    // to be serializable in flash, we need these to be public

    public var _type:String;
    public var _group:String;
    public var _text:String;

    public var _data:Object;
    public var _extra:Object;

    public function ActorErrorMessage(type:String = null, 
                                      group:String = null, 
                                      text:String = null)
    {
      _type = type;
      _group = group;
      _text = text;
    }

    public function get type():String
    {
      return _type;
    }

    public function get group():String
    {
      return _group;
    }

    public function get text():String
    {
      return _text;
    }

    public function get data():Object
    {
      return _data;
    }

    public function set data(d:Object):void
    {
      _data = d;
    }

    public function get extra():Object
    {
      return _extra;
    }

    public function set extra(e:Object):void
    {
      _extra = e;
    }

    public function toString():String
    {
      return "ActorErrorMessage[type=" + _type + 
                               ", group=" + _group + 
                               ", text=" + _text + 
                               ", data=" + _data + 
                               ", extra=" + _extra + "]";
    }
  }
}
