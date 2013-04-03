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

import org.bedework.synch.db.SerializableProperties;
import org.bedework.synch.exception.SynchException;
import org.bedework.synch.wsmessages.SynchPropertyInfoType;

import edu.rpi.sss.util.ToString;

import java.util.ArrayList;
import java.util.List;

/** Help handling properties. This class will hold property info for the parent
 * object. This is built by calls to add.
 *
 * It will also build a new PropertiesInfo object based on a set of properties
 * and a set of current values. This can then be used to transmit the current
 * values to a remote client.
 *
 * @author douglm
 */
public class PropertiesInfo {
  private List<SynchPropertyInfo> propInfo =
      new ArrayList<SynchPropertyInfo>();

  /* ====================================================================
   *                   Some common properties
   * ==================================================================== */

  /** URI
   *
   * @param description null for default description
   */
  public void requiredUri(final String description) {
    prop(BaseSubscriptionInfo.propnameUri,
         SynchPropertyInfo.typeUri,
         description,
         true);
  }

  /** URI
   *
   * @param description null for default description
   */
  public void optionalUri(final String description) {
    prop(BaseSubscriptionInfo.propnameUri,
         SynchPropertyInfo.typeUri,
         description,
         false);
  }

  /** Principal
   *
   * @param description null for default description
   */
  public void requiredPrincipal(final String description) {
    prop(BaseSubscriptionInfo.propnamePrincipal,
         SynchPropertyInfo.typeString,
         description,
         true);
  }

  /** Principal
   *
   * @param description null for default description
   */
  public void optionalPrincipal(final String description) {
    prop(BaseSubscriptionInfo.propnamePrincipal,
         SynchPropertyInfo.typeString,
         description,
         false);
  }

  /** Password
   *
   * @param description null for default description
   */
  public void requiredPassword(final String description) {
    prop(BaseSubscriptionInfo.propnamePassword,
         SynchPropertyInfo.typePassword,
         description,
         true);
  }

  /** Password
   *
   * @param description null for default description
   */
  public void optionalPassword(final String description) {
    prop(BaseSubscriptionInfo.propnamePassword,
         SynchPropertyInfo.typePassword,
         description,
         false);
  }

  /** No-value form
   *
   * @param name - name for the property
   * @param secure - true if this property value should be hidden, e.g password
   * @param type - type of the property - see above
   * @param description - of the property
   * @param required - true if this property is required
   */
  public void add(final String name,
                  final boolean secure,
                  final String type,
                  final String description,
                  final boolean required) {
    propInfo.add(new SynchPropertyInfo(name,
                                       secure,
                                       type,
                                       description,
                                       required));
  }

  /** value form
   *
   * @param name - name for the property
   * @param secure - true if this property value should be hidden, e.g password
   * @param type - type of the property - see above
   * @param description - of the property
   * @param required - true if this property is required
   * @param value - the default or current value as a Java string representation
   */
  public void add(final String name,
                  final boolean secure,
                  final String type,
                  final String description,
                  final boolean required,
                  final String value) {
    propInfo.add(new SynchPropertyInfo(name,
                                       secure,
                                       type,
                                       description,
                                       required,
                                       value));
  }

  /** Add an optional property of type CalProcessing with no default value.
   *
   * @param name - name for the property
   * @param description - of the property
   */
  public void optionCalProcessing(final String name,
                                  final String description) {
    propInfo.add(new SynchPropertyInfo(name,
                                       false,
                                       SynchPropertyInfo.typeCalProcessing,
                                       description,
                                       false));
  }

  /** Add an optional property of type CalProcessing with the given default value.
   *
   * @param name - name for the property
   * @param description - of the property
   * @param value - the default or current value as a Java string representation
   */
  public void optionCalProcessing(final String name,
                                  final String description,
                                  final String value) {
    propInfo.add(new SynchPropertyInfo(name,
                                       false,
                                       SynchPropertyInfo.typeCalProcessing,
                                       description,
                                       false,
                                       value));
  }

  /** Ensure info properties are valid for a new subscription
   *
   * @param info
   * @return true if all ok
   * @throws SynchException
   */
  public boolean validSubscribeInfoProperties(final BaseSubscriptionInfo info) throws SynchException {
    for (SynchPropertyInfo spi: propInfo) {
      if (spi.isRequired() &&
          (info.getProperty(spi.getName()) == null)) {
        return false;
      }
    }

    return true;
  }

  /** Ensure info properties are valid for an existing subscription
   *
   * @param info - current properties
   * @param props - properties in a request to be validated
   * @return true if all ok
   * @throws SynchException
   */
  public boolean validRequestProperties(final BaseSubscriptionInfo info,
                                        final SerializableProperties props) throws SynchException {
    for (SynchPropertyInfo spi: propInfo) {
      if (!spi.isRequired()) {
        continue;
      }

      String propName = spi.getName();
      String subVal = info.getProperty(propName);

      if (subVal == null) {
        // It should never be null - but a change of requirement s may have caused this
        continue;
      }

      String unsubVal = props.getProperty(propName);

      if ((unsubVal == null) || !unsubVal.equals(subVal)) {
        return false;
      }
    }

    return true;
  }

  /** Add all the properties in this list to the parameter
   *
   * @param l
   */
  public void addAllToList(final List<SynchPropertyInfoType> l) {
    l.addAll(propInfo);
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    ts.append("propInfo", propInfo);

    return ts.toString();
  }

  private void prop(final String name,
                    final String type,
                    final String description,
                    final boolean required) {
    String desc = description;

    if (desc == null) {
      desc = "A valid principal - usually of current user";
    }

    add(name,
        false,
        type,
        desc,
        required);
  }
}
