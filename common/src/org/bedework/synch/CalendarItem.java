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
package org.bedework.exchgsynch;

import org.bedework.exchgsynch.intf.Defs;
import org.bedework.exchgsynch.intf.SynchException;

import edu.rpi.sss.util.Util;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Language;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;

import org.apache.log4j.Logger;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.microsoft.schemas.exchange.services._2006.types.ArrayOfStringsType;
import com.microsoft.schemas.exchange.services._2006.types.BodyType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemTypeType;
import com.microsoft.schemas.exchange.services._2006.types.EffectiveRightsType;
import com.microsoft.schemas.exchange.services._2006.types.ExtendedPropertyType;
import com.microsoft.schemas.exchange.services._2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ImportanceChoicesType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services._2006.types.LegacyFreeBusyType;
import com.microsoft.schemas.exchange.services._2006.types.MimeContentType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAllItemsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttachmentsType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttendeesType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfDeletedOccurrencesType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfInternetHeadersType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfOccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfResponseObjectsType;
import com.microsoft.schemas.exchange.services._2006.types.OccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.RecurrenceType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import com.microsoft.schemas.exchange.services._2006.types.SensitivityChoicesType;
import com.microsoft.schemas.exchange.services._2006.types.SingleRecipientType;
import com.microsoft.schemas.exchange.services._2006.types.TimeZoneDefinitionType;
import com.microsoft.schemas.exchange.services._2006.types.TimeZoneType;

/** A calendar Item whose source may be Exchange or the backing system in the
 * form of an iCal4j object.
 *
 * <p>CalendarItemTypeType has one of the following (Case insensitive?) String values<ul>
 * <li><em>IPM.Note</em>: Mail message</li>
 * <li><em>IPM.Post</em>: Post (inbox items?)</li>
 * <li><em>IPM.Appointment</em>: Appointment and Meeting Request entries</li>
 * <li><em>IPM.Task</em>: Task</li>
 * <li><em>IPM.Contact</em>: Contact</li>
 * <li><em>IPM.Activity</em>: Journal entry</li>
 * <li><em>IPM.Distlist</em>: Distribution List</li>
 * <li><em>IPM.StickyNote</em>: Note</li>
 * </ul>
 */
public class CalendarItem extends CalendarItemType implements Defs {
  private Logger logger;

  protected boolean debug;

  /**
   */
  public enum ItemType {
    /** */
    Note,
    /** */
    Post,
    /** */
    Event,
    /** */
    Task,
    /** */
    Contact,
    /** */
    Journal,
    /** */
    DistList,
    /** */
    StickyNote
  }

  private ItemType itemType;

  private CalendarItemType citem;

  private CalendarComponent comp;

  /** Create an object based on a CalendarItemType
   *
   * @param citem
   * @throws SynchException
   */
  public CalendarItem(final CalendarItemType citem) throws SynchException {
    this.citem = citem;

    makeItemType(citem.getItemClass());
  }

  /** Create an object based on an ical4j component
   *
   * @param comp
   * @throws SynchException
   */
  public CalendarItem(final CalendarComponent comp) throws SynchException {
    this.comp = comp;

    makeItemType(comp);
  }

  /**
   * @return type of the item - event etc.
   */
  public ItemType getItemType() {
    return itemType;
  }

  /**
   * @return current or new CalendarItemType
   */
  public CalendarItemType getCitem() {
    if (citem == null) {
      citem = new CalendarItemType();
    }

    return citem;
  }

  /** Make an ical4j component from the calendar item.
   *
   * @return component.
   * @throws SynchException
   */
  public CalendarComponent toComp() throws SynchException {
    makeComp();

    return comp;
  }

  /** Make a calendar item from the ical4j component.
   *
   * @return calendar item.
   * @throws SynchException
   */
  public CalendarItemType toCitem() throws SynchException {
    return citem;
  }

  /* ====================================================================
   *  Delegate methods.
   * ==================================================================== */

  @Override
  public void setAdjacentMeetingCount(final Integer value) {
    getCitem().setAdjacentMeetingCount(value);
  }

  @Override
  public Integer getAdjacentMeetingCount() {
    return getCitem().getAdjacentMeetingCount();
  }

  @Override
  public void setAdjacentMeetings(final NonEmptyArrayOfAllItemsType value) {
    getCitem().setAdjacentMeetings(value);
  }

  @Override
  public NonEmptyArrayOfAllItemsType getAdjacentMeetings() {
    return getCitem().getAdjacentMeetings();
  }

  @Override
  public XMLGregorianCalendar getAppointmentReplyTime() {
    return getCitem().getAppointmentReplyTime();
  }

  @Override
  public void setAppointmentSequenceNumber(final Integer value) {
    getCitem().setAppointmentSequenceNumber(value);
  }

  @Override
  public Integer getAppointmentSequenceNumber() {
    return getCitem().getAppointmentSequenceNumber();
  }

  @Override
  public Integer getAppointmentState() {
    return getCitem().getAppointmentState();
  }

  @Override
  public NonEmptyArrayOfAttachmentsType getAttachments() {
    return getCitem().getAttachments();
  }

  @Override
  public BodyType getBody() {
    return getCitem().getBody();
  }

  @Override
  public void setCalendarItemType(final CalendarItemTypeType value) {
    getCitem().setCalendarItemType(value);
  }

  @Override
  public CalendarItemTypeType getCalendarItemType() {
    return getCitem().getCalendarItemType();
  }

  @Override
  public ArrayOfStringsType getCategories() {
    return getCitem().getCategories();
  }

  @Override
  public Integer getConferenceType() {
    return getCitem().getConferenceType();
  }

  @Override
  public Integer getConflictingMeetingCount() {
    return getCitem().getConflictingMeetingCount();
  }

  @Override
  public NonEmptyArrayOfAllItemsType getConflictingMeetings() {
    return getCitem().getConflictingMeetings();
  }

  @Override
  public ItemIdType getConversationId() {
    return getCitem().getConversationId();
  }

  @Override
  public String getCulture() {
    return getCitem().getCulture();
  }

  @Override
  public XMLGregorianCalendar getDateTimeCreated() {
    return getCitem().getDateTimeCreated();
  }

  @Override
  public XMLGregorianCalendar getDateTimeReceived() {
    return getCitem().getDateTimeReceived();
  }

  @Override
  public XMLGregorianCalendar getDateTimeSent() {
    return getCitem().getDateTimeSent();
  }

  @Override
  public XMLGregorianCalendar getDateTimeStamp() {
    return getCitem().getDateTimeStamp();
  }

  @Override
  public NonEmptyArrayOfDeletedOccurrencesType getDeletedOccurrences() {
    return getCitem().getDeletedOccurrences();
  }

  @Override
  public String getDisplayCc() {
    return getCitem().getDisplayCc();
  }

  @Override
  public String getDisplayTo() {
    return getCitem().getDisplayTo();
  }

  @Override
  public String getDuration() {
    return getCitem().getDuration();
  }

  @Override
  public EffectiveRightsType getEffectiveRights() {
    return getCitem().getEffectiveRights();
  }

  @Override
  public XMLGregorianCalendar getEnd() {
    return getCitem().getEnd();
  }

  @Override
  public void setEndTimeZone(final TimeZoneDefinitionType value) {
    getCitem().setEndTimeZone(value);
  }

  @Override
  public TimeZoneDefinitionType getEndTimeZone() {
    return getCitem().getEndTimeZone();
  }

  @Override
  public List<ExtendedPropertyType> getExtendedProperty() {
    return getCitem().getExtendedProperty();
  }

  @Override
  public OccurrenceInfoType getFirstOccurrence() {
    return getCitem().getFirstOccurrence();
  }

  @Override
  public ImportanceChoicesType getImportance() {
    return getCitem().getImportance();
  }

  @Override
  public String getInReplyTo() {
    return getCitem().getInReplyTo();
  }

  @Override
  public NonEmptyArrayOfInternetHeadersType getInternetMessageHeaders() {
    return getCitem().getInternetMessageHeaders();
  }

  @Override
  public String getItemClass() {
    return getCitem().getItemClass();
  }

  @Override
  public ItemIdType getItemId() {
    return getCitem().getItemId();
  }

  @Override
  public String getLastModifiedName() {
    return getCitem().getLastModifiedName();
  }

  @Override
  public XMLGregorianCalendar getLastModifiedTime() {
    return getCitem().getLastModifiedTime();
  }

  @Override
  public OccurrenceInfoType getLastOccurrence() {
    return getCitem().getLastOccurrence();
  }

  @Override
  public void setLegacyFreeBusyStatus(final LegacyFreeBusyType value) {
    getCitem().setLegacyFreeBusyStatus(value);
  }

  @Override
  public LegacyFreeBusyType getLegacyFreeBusyStatus() {
    return getCitem().getLegacyFreeBusyStatus();
  }

  @Override
  public String getLocation() {
    return getCitem().getLocation();
  }

  @Override
  public TimeZoneType getMeetingTimeZone() {
    return getCitem().getMeetingTimeZone();
  }

  @Override
  public String getMeetingWorkspaceUrl() {
    return getCitem().getMeetingWorkspaceUrl();
  }

  @Override
  public MimeContentType getMimeContent() {
    return getCitem().getMimeContent();
  }

  @Override
  public NonEmptyArrayOfOccurrenceInfoType getModifiedOccurrences() {
    return getCitem().getModifiedOccurrences();
  }

  @Override
  public ResponseTypeType getMyResponseType() {
    return getCitem().getMyResponseType();
  }

  @Override
  public String getNetShowUrl() {
    return getCitem().getNetShowUrl();
  }

  @Override
  public NonEmptyArrayOfAttendeesType getOptionalAttendees() {
    return getCitem().getOptionalAttendees();
  }

  @Override
  public SingleRecipientType getOrganizer() {
    return getCitem().getOrganizer();
  }

  @Override
  public XMLGregorianCalendar getOriginalStart() {
    return getCitem().getOriginalStart();
  }

  @Override
  public FolderIdType getParentFolderId() {
    return getCitem().getParentFolderId();
  }

  @Override
  public RecurrenceType getRecurrence() {
    return getCitem().getRecurrence();
  }

  @Override
  public XMLGregorianCalendar getRecurrenceId() {
    return getCitem().getRecurrenceId();
  }

  @Override
  public XMLGregorianCalendar getReminderDueBy() {
    return getCitem().getReminderDueBy();
  }

  @Override
  public String getReminderMinutesBeforeStart() {
    return getCitem().getReminderMinutesBeforeStart();
  }

  @Override
  public NonEmptyArrayOfAttendeesType getRequiredAttendees() {
    return getCitem().getRequiredAttendees();
  }

  @Override
  public NonEmptyArrayOfAttendeesType getResources() {
    return getCitem().getResources();
  }

  @Override
  public NonEmptyArrayOfResponseObjectsType getResponseObjects() {
    return getCitem().getResponseObjects();
  }

  @Override
  public SensitivityChoicesType getSensitivity() {
    return getCitem().getSensitivity();
  }

  @Override
  public Integer getSize() {
    return getCitem().getSize();
  }

  @Override
  public XMLGregorianCalendar getStart() {
    return getCitem().getStart();
  }

  @Override
  public void setStartTimeZone(final TimeZoneDefinitionType value) {
    getCitem().setStartTimeZone(value);
  }

  @Override
  public TimeZoneDefinitionType getStartTimeZone() {
    return getCitem().getStartTimeZone();
  }

  @Override
  public String getSubject() {
    return getCitem().getSubject();
  }

  @Override
  public String getTimeZone() {
    return getCitem().getTimeZone();
  }

  @Override
  public void setUID(final String value) {
    getCitem().setUID(value);
  }

  @Override
  public String getUID() {
    return getCitem().getUID();
  }

  @Override
  public BodyType getUniqueBody() {
    return getCitem().getUniqueBody();
  }

  @Override
  public String getWebClientEditFormQueryString() {
    return getCitem().getWebClientEditFormQueryString();
  }

  @Override
  public String getWebClientReadFormQueryString() {
    return getCitem().getWebClientReadFormQueryString();
  }

  @Override
  public String getWhen() {
    return getCitem().getWhen();
  }

  @Override
  public int hashCode() {
    return getCitem().hashCode();
  }

  @Override
  public Boolean isAllowNewTimeProposal() {
    return getCitem().isAllowNewTimeProposal();
  }

  @Override
  public Boolean isHasAttachments() {
    return getCitem().isHasAttachments();
  }

  @Override
  public void setIsAllDayEvent(final Boolean value) {
    getCitem().setIsAllDayEvent(value);
  }

  @Override
  public Boolean isIsAllDayEvent() {
    return getCitem().isIsAllDayEvent();
  }

  @Override
  public Boolean isIsAssociated() {
    return getCitem().isIsAssociated();
  }

  @Override
  public Boolean isIsCancelled() {
    return getCitem().isIsCancelled();
  }

  @Override
  public Boolean isIsDraft() {
    return getCitem().isIsDraft();
  }

  @Override
  public Boolean isIsFromMe() {
    return getCitem().isIsFromMe();
  }

  @Override
  public Boolean isIsMeeting() {
    return getCitem().isIsMeeting();
  }

  @Override
  public Boolean isIsOnlineMeeting() {
    return getCitem().isIsOnlineMeeting();
  }

  @Override
  public Boolean isIsRecurring() {
    return getCitem().isIsRecurring();
  }

  @Override
  public Boolean isIsResend() {
    return getCitem().isIsResend();
  }

  @Override
  public Boolean isIsResponseRequested() {
    return getCitem().isIsResponseRequested();
  }

  @Override
  public Boolean isIsSubmitted() {
    return getCitem().isIsSubmitted();
  }

  @Override
  public Boolean isIsUnmodified() {
    return getCitem().isIsUnmodified();
  }

  @Override
  public Boolean isMeetingRequestWasSent() {
    return getCitem().isMeetingRequestWasSent();
  }

  @Override
  public void setReminderIsSet(final Boolean value) {
    getCitem().setReminderIsSet(value);
  }

  @Override
  public Boolean isReminderIsSet() {
    return getCitem().isReminderIsSet();
  }

  @Override
  public void setAllowNewTimeProposal(final Boolean value) {
    getCitem().setAllowNewTimeProposal(value);
  }

  @Override
  public void setAppointmentReplyTime(final XMLGregorianCalendar value) {
    getCitem().setAppointmentReplyTime(value);
  }

  @Override
  public void setAppointmentState(final Integer value) {
    getCitem().setAppointmentState(value);
  }

  @Override
  public void setAttachments(final NonEmptyArrayOfAttachmentsType value) {
    getCitem().setAttachments(value);
  }

  @Override
  public void setBody(final BodyType value) {
    getCitem().setBody(value);
  }

  @Override
  public void setCategories(final ArrayOfStringsType value) {
    getCitem().setCategories(value);
  }

  @Override
  public void setConferenceType(final Integer value) {
    getCitem().setConferenceType(value);
  }

  @Override
  public void setConflictingMeetingCount(final Integer value) {
    getCitem().setConflictingMeetingCount(value);
  }

  @Override
  public void setConflictingMeetings(final NonEmptyArrayOfAllItemsType value) {
    getCitem().setConflictingMeetings(value);
  }

  @Override
  public void setConversationId(final ItemIdType value) {
    getCitem().setConversationId(value);
  }

  @Override
  public void setCulture(final String value) {
    getCitem().setCulture(value);
  }

  @Override
  public void setDateTimeCreated(final XMLGregorianCalendar value) {
    getCitem().setDateTimeCreated(value);
  }

  @Override
  public void setDateTimeReceived(final XMLGregorianCalendar value) {
    getCitem().setDateTimeReceived(value);
  }

  @Override
  public void setDateTimeSent(final XMLGregorianCalendar value) {
    getCitem().setDateTimeSent(value);
  }

  @Override
  public void setDateTimeStamp(final XMLGregorianCalendar value) {
    getCitem().setDateTimeStamp(value);
  }

  @Override
  public void setDeletedOccurrences(final NonEmptyArrayOfDeletedOccurrencesType value) {
    getCitem().setDeletedOccurrences(value);
  }

  @Override
  public void setDisplayCc(final String value) {
    getCitem().setDisplayCc(value);
  }

  @Override
  public void setDisplayTo(final String value) {
    getCitem().setDisplayTo(value);
  }

  @Override
  public void setDuration(final String value) {
    getCitem().setDuration(value);
  }

  @Override
  public void setEffectiveRights(final EffectiveRightsType value) {
    getCitem().setEffectiveRights(value);
  }

  @Override
  public void setEnd(final XMLGregorianCalendar value) {
    getCitem().setEnd(value);
  }

  @Override
  public void setFirstOccurrence(final OccurrenceInfoType value) {
    getCitem().setFirstOccurrence(value);
  }

  @Override
  public void setHasAttachments(final Boolean value) {
    getCitem().setHasAttachments(value);
  }

  @Override
  public void setImportance(final ImportanceChoicesType value) {
    getCitem().setImportance(value);
  }

  @Override
  public void setInReplyTo(final String value) {
    getCitem().setInReplyTo(value);
  }

  @Override
  public void setInternetMessageHeaders(final NonEmptyArrayOfInternetHeadersType value) {
    getCitem().setInternetMessageHeaders(value);
  }

  @Override
  public void setIsAssociated(final Boolean value) {
    getCitem().setIsAssociated(value);
  }

  @Override
  public void setIsCancelled(final Boolean value) {
    getCitem().setIsCancelled(value);
  }

  @Override
  public void setIsDraft(final Boolean value) {
    getCitem().setIsDraft(value);
  }

  @Override
  public void setIsFromMe(final Boolean value) {
    getCitem().setIsFromMe(value);
  }

  @Override
  public void setIsMeeting(final Boolean value) {
    getCitem().setIsMeeting(value);
  }

  @Override
  public void setIsOnlineMeeting(final Boolean value) {
    getCitem().setIsOnlineMeeting(value);
  }

  @Override
  public void setIsRecurring(final Boolean value) {
    getCitem().setIsRecurring(value);
  }

  @Override
  public void setIsResend(final Boolean value) {
    getCitem().setIsResend(value);
  }

  @Override
  public void setIsResponseRequested(final Boolean value) {
    getCitem().setIsResponseRequested(value);
  }

  @Override
  public void setIsSubmitted(final Boolean value) {
    getCitem().setIsSubmitted(value);
  }

  @Override
  public void setIsUnmodified(final Boolean value) {
    getCitem().setIsUnmodified(value);
  }

  @Override
  public void setItemClass(final String value) {
    getCitem().setItemClass(value);
  }

  @Override
  public void setItemId(final ItemIdType value) {
    getCitem().setItemId(value);
  }

  @Override
  public void setLastModifiedName(final String value) {
    getCitem().setLastModifiedName(value);
  }

  @Override
  public void setLastModifiedTime(final XMLGregorianCalendar value) {
    getCitem().setLastModifiedTime(value);
  }

  @Override
  public void setLastOccurrence(final OccurrenceInfoType value) {
    getCitem().setLastOccurrence(value);
  }

  @Override
  public void setLocation(final String value) {
    getCitem().setLocation(value);
  }

  @Override
  public void setMeetingRequestWasSent(final Boolean value) {
    getCitem().setMeetingRequestWasSent(value);
  }

  @Override
  public void setMeetingTimeZone(final TimeZoneType value) {
    getCitem().setMeetingTimeZone(value);
  }

  @Override
  public void setMeetingWorkspaceUrl(final String value) {
    getCitem().setMeetingWorkspaceUrl(value);
  }

  @Override
  public void setMimeContent(final MimeContentType value) {
    getCitem().setMimeContent(value);
  }

  @Override
  public void setModifiedOccurrences(final NonEmptyArrayOfOccurrenceInfoType value) {
    getCitem().setModifiedOccurrences(value);
  }

  @Override
  public void setMyResponseType(final ResponseTypeType value) {
    getCitem().setMyResponseType(value);
  }

  @Override
  public void setNetShowUrl(final String value) {
    getCitem().setNetShowUrl(value);
  }

  @Override
  public void setOptionalAttendees(final NonEmptyArrayOfAttendeesType value) {
    getCitem().setOptionalAttendees(value);
  }

  @Override
  public void setOrganizer(final SingleRecipientType value) {
    getCitem().setOrganizer(value);
  }

  @Override
  public void setOriginalStart(final XMLGregorianCalendar value) {
    getCitem().setOriginalStart(value);
  }

  @Override
  public void setParentFolderId(final FolderIdType value) {
    getCitem().setParentFolderId(value);
  }

  @Override
  public void setRecurrence(final RecurrenceType value) {
    getCitem().setRecurrence(value);
  }

  @Override
  public void setRecurrenceId(final XMLGregorianCalendar value) {
    getCitem().setRecurrenceId(value);
  }

  @Override
  public void setReminderDueBy(final XMLGregorianCalendar value) {
    getCitem().setReminderDueBy(value);
  }

  @Override
  public void setReminderMinutesBeforeStart(final String value) {
    getCitem().setReminderMinutesBeforeStart(value);
  }

  @Override
  public void setRequiredAttendees(final NonEmptyArrayOfAttendeesType value) {
    getCitem().setRequiredAttendees(value);
  }

  @Override
  public void setResources(final NonEmptyArrayOfAttendeesType value) {
    getCitem().setResources(value);
  }

  @Override
  public void setResponseObjects(final NonEmptyArrayOfResponseObjectsType value) {
    getCitem().setResponseObjects(value);
  }

  @Override
  public void setSensitivity(final SensitivityChoicesType value) {
    getCitem().setSensitivity(value);
  }

  @Override
  public void setSize(final Integer value) {
    getCitem().setSize(value);
  }

  @Override
  public void setStart(final XMLGregorianCalendar value) {
    getCitem().setStart(value);
  }

  @Override
  public void setSubject(final String value) {
    getCitem().setSubject(value);
  }

  @Override
  public void setTimeZone(final String value) {
    getCitem().setTimeZone(value);
  }

  @Override
  public void setUniqueBody(final BodyType value) {
    getCitem().setUniqueBody(value);
  }

  @Override
  public void setWebClientEditFormQueryString(final String value) {
    getCitem().setWebClientEditFormQueryString(value);
  }

  @Override
  public void setWebClientReadFormQueryString(final String value) {
    getCitem().setWebClientReadFormQueryString(value);
  }

  @Override
  public void setWhen(final String value) {
    getCitem().setWhen(value);
  }

  /**
   * @param sb
   */
  public void toStringSegment(final StringBuilder sb) {
  }

  /**
   * @throws SynchException
   */
  public void makeComp() throws SynchException {
    ComponentList cl = null;

    if (itemType == ItemType.Event) {
      comp = new VEvent();
      cl = ((VEvent)comp).getAlarms();
    } else if (itemType == ItemType.Task) {
      comp = new VToDo();
      cl = ((VToDo)comp).getAlarms();
    } else if (itemType == ItemType.Journal) {
      comp = new VJournal();
    } else {
      throw new SynchException(SynchException.unknownCalendarItemType);
    }

    PropertyList pl = comp.getProperties();

    /* =========================== UID ================================== */
    pl.add(new Uid(getUID()));

    /* ===================== Tzid, DtStart, DtEnd ======================= */
    /* The timezone is applied to all zoned times,
     *   dtstart, dtend, recurrenceid.
     * It is an MS id which we need to map on to an Olson style
     */
    String extzid = getTimeZone();

    /* Tz service will map the id for us */
    TzStuff startTz = getTz(getStartTimeZone(), extzid);
    TzStuff endTz = getTz(getEndTimeZone(), extzid);

    DateProperty dp = makeDateProp(startTz.tz, getStart(),
                                   Boolean.valueOf(isIsAllDayEvent()),
                                   true);
    if (dp != null) {
      pl.add(dp);
    }

    dp = makeDateProp(endTz.tz, getEnd(),
                      Boolean.valueOf(isIsAllDayEvent()),
                      false);

    if (dp != null) {
      pl.add(dp);
    }

    if (extzid != null) {
      pl.add(new XProperty(xpMSTzid, extzid));
    }

    if ((startTz != null) && (startTz.id != null) &&
        (!Util.equalsString(extzid, startTz.id))) {
      pl.add(new XProperty(xpMSStartTzid, startTz.id));
    }

    if ((endTz != null) && (endTz.id != null) &&
        (!Util.equalsString(extzid, endTz.id))) {
      pl.add(new XProperty(xpMSEndTzid, endTz.id));
    }

    pl.add(new DtStamp());

    /* ========================== Reminder =========================== */
    if ((cl != null) && isReminderIsSet()) {
      VAlarm al = new VAlarm();

      al.getProperties().add(Action.DISPLAY);
      al.getProperties().add(new Description("REMINDER"));
      al.getProperties().add(new Trigger(new Dur(
                                         "-PT" + getReminderMinutesBeforeStart() +"M")));

      cl.add(al);
    }

    /* =========================== Summary =========================== */

    Language lang = null;
    if (getCulture() != null) {
      lang = new Language(getCulture());
    }

    pl.add(new Summary(makePlist(lang), getSubject()));

    /* ======================== Description ========================== */

    BodyType body = getBody();

    if (body != null) {
      if (body.getValue() != null) {
        pl.add(new Description(makePlist(lang), body.getValue()));
      }
    }

    /* ========================= MS Lastmod ========================== */

    addXprop(pl, xpMSLastmod, getLastModifiedTime());

    /* ========================= MS X-props ========================== */

    addXprop(pl, "X-MICROSOFT-CDO-APPT-SEQUENCE", getAppointmentSequenceNumber());
    //addXprop("X-MICROSOFT-CDO-OWNERAPPTID", ?);
    addXprop(pl, "X-MICROSOFT-CDO-BUSYSTATUS", getLegacyFreeBusyStatus());
    //addXprop("X-MICROSOFT-CDO-INTENDEDSTATUS", ?);
    addXprop(pl, "X-MICROSOFT-CDO-ALLDAYEVENT", isIsAllDayEvent());
    //addXprop("X-MICROSOFT-CDO-IMPORTANCE:1", this);
    //addXprop("X-MICROSOFT-CDO-INSTTYPE:0", this);
    //addXprop("X-MICROSOFT-DISALLOW-COUNTER:FALSE", this);
  }

  /* ====================================================================
   *  Protected methods.
   * ==================================================================== */

  protected Logger getLogger() {
    if (logger == null) {
      logger = Logger.getLogger(this.getClass());
    }

    return logger;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  /* ====================================================================
   *  Private methods.
   * ==================================================================== */

  private void makeItemType(final Component val) throws SynchException {
    if (val instanceof VEvent) {
      itemType = ItemType.Event;
      return;
    }

    if (val instanceof VToDo) {
      itemType = ItemType.Task;
      return;
    }

    if (val instanceof VJournal) {
      itemType = ItemType.Journal;
      return;
    }

    throw new SynchException(SynchException.unknownCalendarItemType);
  }

  private void makeItemType(final String val) throws SynchException {
    String uval = val.toUpperCase();

    if (uval.equals("IPM.NOTE")) {
      itemType = ItemType.Note;
      return;
    }

    if (uval.equals("IPM.POST")) {
      itemType = ItemType.Post;
      return;
    }

    if (uval.equals("IPM.APPOINTMENT")) {
      itemType = ItemType.Event;
      return;
    }

    if (uval.equals("IPM.TASK")) {
      itemType = ItemType.Task;
      return;
    }

    if (uval.equals("IPM.CONTACT")) {
      itemType = ItemType.Contact;
      return;
    }

    if (uval.equals("IPM.ACTIVITY")) {
      itemType = ItemType.Journal;
      return;
    }

    if (uval.equals("IPM.DISTLIST")) {
      itemType = ItemType.DistList;
      return;
    }

    if (uval.equals("IPM.STICKYNOTE")) {
      itemType = ItemType.StickyNote;
      return;
    }

    throw new SynchException(SynchException.unknownCalendarItemType);
  }

  private ParameterList makePlist(final Parameter p) {
    ParameterList pl = new ParameterList();

    if (p == null) {
      return pl;
    }

    pl.add(p);

    return pl;
  }

  private void addXprop(final PropertyList pl, final String name, final Object val) {
    if (val == null) {
      return;
    }

    pl.add(new XProperty(name, String.valueOf(val)));
  }

  /**
   * @author douglm
   */
  public static class TzStuff {
    String id;
    TimeZone tz;
  }

  /**
   * @param tzdef
   * @param extzid
   * @return tz stuff
   * @throws SynchException
   */
  public static TzStuff getTz(final TimeZoneDefinitionType tzdef,
                              final String extzid) throws SynchException {
    TzStuff t = new TzStuff();

    if (tzdef != null) {
      t.id = tzdef.getId();
      if ((extzid != null) && (extzid.equals(t.id))) {
        t.id = null;
      } else {
        t.tz = ExchangeSynch.getTz(t.id);
        return t;
      }
    }

    if (extzid != null) {
      t.tz = ExchangeSynch.getTz(extzid);
    }

    return t;
  }

  private DateProperty makeDateProp(final TimeZone tz,
                                    final XMLGregorianCalendar dt,
                                    final boolean allDay,
                                    final boolean start) throws SynchException {
    try {
      if (dt == null) {
        return null;
      }

      String dtval = dt.toXMLFormat();

      StringBuilder sb = new StringBuilder();

      sb.append(dtval.substring(0, 4)); // yyyy
      sb.append(dtval.substring(5, 7)); // mm
      sb.append(dtval.substring(8, 10)); // dd

      if (!allDay) {
        sb.append("T");
        sb.append(dtval.substring(11, 13)); // hh
        sb.append(dtval.substring(14, 16)); // mm
        sb.append(dtval.substring(17, 19)); // ss
        sb.append("Z");
      }

      DateProperty dp;

      if (start) {
        dp = new DtStart(sb.toString());
      } else {
        dp = new DtEnd(sb.toString());
      }

      if (!allDay && (tz != null)) {
        dp.setUtc(false);
        dp.setTimeZone(tz);
      }

      return dp;
    } catch (Throwable t) {
      throw new SynchException(t);
    }
  }
}
