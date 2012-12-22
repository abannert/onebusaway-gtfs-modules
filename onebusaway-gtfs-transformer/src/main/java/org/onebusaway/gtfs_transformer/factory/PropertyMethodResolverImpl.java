/**
 * Copyright (C) 2012 Google, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs_transformer.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.collections.beans.DefaultPropertyMethodResolver;
import org.onebusaway.collections.beans.PropertyMethod;
import org.onebusaway.csv_entities.schema.EntitySchema;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.SingleFieldMapping;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

class PropertyMethodResolverImpl extends DefaultPropertyMethodResolver {

  private GtfsRelationalDao _dao;

  /**
   * Mapping from CSV field names to
   */
  private Map<Class<?>, Map<String, String>> _entityTypesAndcsvFieldNamesToObjectFieldNames = new HashMap<Class<?>, Map<String, String>>();

  /**
   * 
   * @param dao
   */
  public PropertyMethodResolverImpl(GtfsRelationalDao dao) {
    _dao = dao;
  }

  public void addCsvFieldMappings(EntitySchema schema) {
    Map<String, String> csvFieldNamesToObjectFieldNames = _entityTypesAndcsvFieldNamesToObjectFieldNames.get(schema.getEntityClass());
    if (csvFieldNamesToObjectFieldNames == null) {
      csvFieldNamesToObjectFieldNames = new HashMap<String, String>();
      _entityTypesAndcsvFieldNamesToObjectFieldNames.put(
          schema.getEntityClass(), csvFieldNamesToObjectFieldNames);
    }
    for (FieldMapping fieldMapping : schema.getFields()) {
      if (fieldMapping instanceof SingleFieldMapping) {
        SingleFieldMapping singleFieldMapping = (SingleFieldMapping) fieldMapping;
        csvFieldNamesToObjectFieldNames.put(
            singleFieldMapping.getCsvFieldName(),
            singleFieldMapping.getObjFieldName());
      }
    }
  }

  @Override
  public PropertyMethod getPropertyMethod(Class<?> targetType,
      String propertyName) {

    if (targetType.equals(Trip.class)) {
      if (propertyName.equals("stopTimes")) {
        return new StopTimesForTripPropertyMethod(_dao);
      } else if (propertyName.equals("calendar")) {
        return new ServiceCalendarForTripPropertyMethod(_dao);
      }
    }
    Map<String, String> csvFieldNamesToObjectFieldNames = _entityTypesAndcsvFieldNamesToObjectFieldNames.get(targetType);
    if (csvFieldNamesToObjectFieldNames != null) {
      String objectFieldName = csvFieldNamesToObjectFieldNames.get(propertyName);
      if (objectFieldName != null) {
        propertyName = objectFieldName;
      }
    }
    return super.getPropertyMethod(targetType, propertyName);
  }

  private static class StopTimesForTripPropertyMethod implements PropertyMethod {

    private final GtfsRelationalDao _dao;

    private StopTimesForTripPropertyMethod(GtfsRelationalDao dao) {
      _dao = dao;
    }

    @Override
    public Object invoke(Object value) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException {
      return _dao.getStopTimesForTrip((Trip) value);
    }

    @Override
    public Class<?> getReturnType() {
      return List.class;
    }
  }

  private static class ServiceCalendarForTripPropertyMethod implements
      PropertyMethod {

    private final GtfsRelationalDao _dao;

    public ServiceCalendarForTripPropertyMethod(GtfsRelationalDao dao) {
      _dao = dao;
    }

    @Override
    public Object invoke(Object value) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException {
      return _dao.getCalendarForServiceId(((Trip) value).getServiceId());
    }

    @Override
    public Class<?> getReturnType() {
      return ServiceCalendar.class;
    }
  }
}