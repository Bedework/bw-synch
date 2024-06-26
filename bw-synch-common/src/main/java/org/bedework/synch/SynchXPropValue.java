/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.synch;

import org.bedework.synch.shared.exception.SynchException;

import org.apache.commons.codec.binary.Base64;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/** We encode a number of values as a single x-property by Base 64 encoding
 * the result of the Properties store method
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class SynchXPropValue extends Properties {
  /**
   * @return encoded values
   */
  public String encode() {
    final StringWriter sw = new StringWriter();

    try {
      store(sw, null);

      final String s = sw.toString();

      return Base64.encodeBase64String(s.getBytes());
    } catch (final Exception t) {
      throw new SynchException(t);
    }
  }

  /**
   * @param val encoded values
   */
  public void decode(final String val) {
    try {
      final StringReader sr =
              new StringReader(new String(Base64.decodeBase64(val)));

      clear();
      load(sr);
    } catch (final Exception t) {
      throw new SynchException(t);
    }
  }
}
