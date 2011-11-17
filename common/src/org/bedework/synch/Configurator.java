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

import org.bedework.synch.db.SynchConfig;
import org.bedework.synch.db.SynchDb;
import org.bedework.synch.exception.SynchException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/** Read and update configuration.
 *
 * @author douglm
 */
public class Configurator {
  private static final String synchConfigPath = "/properties/synch/synch-config.xml";

  /* Initial config bean name */
  private static String synchConfigName = "synchConfig";

  private SynchDb db;

  private SynchConfig synchConfig;

  /**
   * @param db
   */
  public Configurator(final SynchDb db) {
    this.db = db;
  }

  /**
   * @return SynchConfig
   * @throws SynchException
   */
  public SynchConfig getSynchConfig() throws SynchException {
    if (synchConfig != null) {
      return synchConfig;
    }

    try {
      db.open();

      synchConfig = db.getConfig();

      if (synchConfig != null) {
        return synchConfig;
      }

      /* Initialize from bean info */

      initFromBeans();

      db.add(synchConfig);
    } finally {
      db.close();
    }

    return synchConfig;
  }

  /**
   * @throws SynchException
   */
  public void updateSynchConfig() throws SynchException {
    try {
      db.open();
      db.update(synchConfig);
    } finally {
      db.close();
    }
  }

  private void initFromBeans() throws SynchException {
    ApplicationContext appContext =
         new ClassPathXmlApplicationContext(synchConfigPath);

    synchConfig = (SynchConfig)appContext.getBean(synchConfigName);

    if ((synchConfig.getCallbackURI() != null) &&
        !synchConfig.getCallbackURI().endsWith("/")) {
      throw new SynchException("callbackURI MUST end with '/'");
    }

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    try {
      sb.append(getSynchConfig());
    } catch (Throwable t) {
      sb.append("Error getting config: ");
      sb.append(t.getMessage());
    }

    sb.append("}");

    return sb.toString();
  }
}
