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
package org.bedework.synch.cnctrs.exchange.messages;

import javax.xml.bind.JAXBElement;

import com.microsoft.schemas.exchange.services._2006.types.BasePathToElementType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfPathsToElementType;
import com.microsoft.schemas.exchange.services._2006.types.PathToUnindexedFieldType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;


/** Base request class.
 *
 *   @author Mike Douglass   douglm rpi.edu
 *
 * @param <T>
 */
public class BaseRequest<T>
    extends com.microsoft.schemas.exchange.services._2006.messages.ObjectFactory {
  protected com.microsoft.schemas.exchange.services._2006.types.ObjectFactory types =
    new com.microsoft.schemas.exchange.services._2006.types.ObjectFactory();

  protected T request;

  protected BaseRequest() { }

  /**
   * @return T
   */
  public T getRequest() {
    return request;
  }

  protected void addShapeProp(final NonEmptyArrayOfPathsToElementType additionalProperties,
                              final UnindexedFieldURIType URI) {
    PathToUnindexedFieldType pathToUnindexedField = new PathToUnindexedFieldType();
    pathToUnindexedField.setFieldURI(URI);
    JAXBElement<? extends BasePathToElementType> path =
      types.createFieldURI(pathToUnindexedField);

    additionalProperties.getPath().add(path);
  }
}