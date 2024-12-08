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
package org.bedework.synch.cnctrs.exchange;

import org.bedework.synch.intf.Defs;
import org.bedework.synch.shared.exception.SynchException;
import org.bedework.util.calendar.XcalUtil.TzGetter;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.BodyType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.EmailAddressType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttendeesType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import com.microsoft.schemas.exchange.services._2006.types.SingleRecipientType;
import com.microsoft.schemas.exchange.services._2006.types.TimeZoneDefinitionType;
import ietf.params.xml.ns.icalendar_2.ActionPropType;
import ietf.params.xml.ns.icalendar_2.ArrayOfComponents;
import ietf.params.xml.ns.icalendar_2.ArrayOfParameters;
import ietf.params.xml.ns.icalendar_2.ArrayOfProperties;
import ietf.params.xml.ns.icalendar_2.AttendeePropType;
import ietf.params.xml.ns.icalendar_2.BaseComponentType;
import ietf.params.xml.ns.icalendar_2.BasePropertyType;
import ietf.params.xml.ns.icalendar_2.CalAddressPropertyType;
import ietf.params.xml.ns.icalendar_2.CategoriesPropType;
import ietf.params.xml.ns.icalendar_2.ClassPropType;
import ietf.params.xml.ns.icalendar_2.CnParamType;
import ietf.params.xml.ns.icalendar_2.DateDatetimePropertyType;
import ietf.params.xml.ns.icalendar_2.DescriptionPropType;
import ietf.params.xml.ns.icalendar_2.DtendPropType;
import ietf.params.xml.ns.icalendar_2.DtstartPropType;
import ietf.params.xml.ns.icalendar_2.DuePropType;
import ietf.params.xml.ns.icalendar_2.IcalendarType;
import ietf.params.xml.ns.icalendar_2.LocationPropType;
import ietf.params.xml.ns.icalendar_2.ObjectFactory;
import ietf.params.xml.ns.icalendar_2.OrganizerPropType;
import ietf.params.xml.ns.icalendar_2.PartstatParamType;
import ietf.params.xml.ns.icalendar_2.RoleParamType;
import ietf.params.xml.ns.icalendar_2.StatusPropType;
import ietf.params.xml.ns.icalendar_2.SummaryPropType;
import ietf.params.xml.ns.icalendar_2.TranspPropType;
import ietf.params.xml.ns.icalendar_2.TriggerPropType;
import ietf.params.xml.ns.icalendar_2.TzidParamType;
import ietf.params.xml.ns.icalendar_2.UidPropType;
import ietf.params.xml.ns.icalendar_2.ValarmType;
import ietf.params.xml.ns.icalendar_2.VcalendarType;
import ietf.params.xml.ns.icalendar_2.VeventType;
import ietf.params.xml.ns.icalendar_2.VjournalType;
import ietf.params.xml.ns.icalendar_2.VtodoType;
import ietf.params.xml.ns.icalendar_2.XBedeworkExsynchLastmodPropType;
import ietf.params.xml.ns.icalendar_2.XMicrosoftCdoBusystatusPropType;

import java.util.TimeZone;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

/** This class manages conversions to and from Xml ICalendar to Exchange.
*
* @author Mike Douglass
*/
public class XmlIcalConvert implements Logged, Defs {
  private final ObjectFactory xcalOF = new ObjectFactory();

  private final TzGetter tzGetter;

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

  public XmlIcalConvert(final TzGetter tzGetter) {
    this.tzGetter = tzGetter;
  }

  /**
   * @param cal the xml calendar
   * @return Icalendar
   */
  public IcalendarType toXml(final CalendarItemType cal) {
    /* TODO
     * Transparency - derived from what?
     */
    String transpVal = null;
    String statusVal = null;

    final IcalendarType ical = new IcalendarType();
    final VcalendarType vcal = new VcalendarType();

    ical.getVcalendar().add(vcal);
    vcal.setComponents(new ArrayOfComponents());

    final JAXBElement<? extends BaseComponentType> el;

    final ItemType itemType = makeItemType(cal.getItemClass());

    if (itemType == ItemType.Event) {
      el = xcalOF.createVevent(new VeventType());
    } else if (itemType == ItemType.Task) {
      el = xcalOF.createVtodo(new VtodoType());
    } else if (itemType == ItemType.Journal) {
      el = xcalOF.createVjournal(new VjournalType());
    } else {
      throw new SynchException(SynchException.unknownCalendarItemType);
    }

    final BaseComponentType comp = el.getValue();

    /* the component property list */
    final ArrayOfProperties aop = new ArrayOfProperties();
    final var pl = aop.getBasePropertyOrTzid();

    comp.setProperties(aop);

    vcal.getComponents().getBaseComponent().add(el);

    /* ============ Read only properties - may not need preserving ========== */
    /* ======================= cal:OriginalStart ============================ */
    /* ================= cal:ConflictingMeetingCount ======================== */

    /* ============================== UID =================================== */
    final UidPropType uid = new UidPropType();
    uid.setText(cal.getUID());
    final var jaxbUid = xcalOF.createUid(uid);
    pl.add(jaxbUid);

    /* ========================= DateTimeStamp ============================== */

    /* ======================= StartTimeZone ============================== */
    /* ======================= EndTimeZone ============================== */
    /* ================ TimeZone, Start, End, IsAllDayEvent ================= */
    /* The timezone is applied to all zoned times,
     *   dtstart, dtend, recurrenceid.
     * It is an MS id which we need to map on to an Olson style
     */
    final String extzid = cal.getTimeZone();
    if (debug()) {
      debug("exchange tzid=" + extzid);
    }

    /* Tz service will map the id for us */
    final TzStuff startTz = getTz(cal.getStartTimeZone(), extzid);
    final TzStuff endTz = getTz(cal.getEndTimeZone(), extzid);

    JAXBElement<? extends BasePropertyType> ddp = makeDateProp(startTz.tz, cal.getStart(),
                                                               cal.isIsAllDayEvent(),
                                                Dtype.start);
    if (ddp != null) {
      pl.add(ddp);
    }

    final Dtype dtype;

    if (itemType == ItemType.Task) {
      dtype = Dtype.due;
    } else {
      dtype = Dtype.end;
    }

    ddp = makeDateProp(endTz.tz, cal.getEnd(),
                       cal.isIsAllDayEvent(),
                       dtype);

    if (ddp != null) {
      pl.add(ddp);
    }

    /*
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
    */
    /* ======================= cal:DateTimeStamp ============================ */
    if (debug()) {
      debug("dtstamp=" + cal.getDateTimeStamp().toXMLFormat());
    }

    /* ==================== cal:LegacyFreeBusyStatus ======================== */
    /* Free, Tentative, Busy, OOF (OutOfOffice), NoData */
    if (cal.getLegacyFreeBusyStatus() != null) {
      final XMicrosoftCdoBusystatusPropType bsp = new XMicrosoftCdoBusystatusPropType();
      bsp.setText(cal.getLegacyFreeBusyStatus().value());

      final var jaxb1 = xcalOF.createXMicrosoftCdoBusystatus(bsp);
      pl.add(jaxb1);
      /* X-MICROSOFT-CDO-INTENDEDSTATUS Specifies the busy status that the
       * organizer intends for the attendee's copy of the meeting.
       */
      //addXprop("X-MICROSOFT-CDO-INTENDEDSTATUS", ?);
    }

    /* ========================== cal:Location  ============================= */
    if ((cal.getLocation() != null) && (!cal.getLocation().isEmpty())) {
      final LocationPropType loc = new LocationPropType();

      loc.setText(cal.getLocation());

      final JAXBElement<LocationPropType> jaxb = xcalOF.createLocation(loc);
      pl.add(jaxb);
    }

    /* ============================ cal:When ================================ */
    /* ========================= cal:IsMeeting ============================== */
    /* ======================== cal:IsCancelled ============================= */
    if (cal.isIsCancelled()) {
      statusVal = "CANCELLED";
      transpVal = "TRANSPARENT";
    }

    /* ======================== cal:IsRecurring ============================= */
    /* =================== cal:MeetingRequestWasSent ======================== */
    /* =================== cal:IsResponseRequested ========================== */
    /* ===================== cal:CalendarItemType =========================== */
    /* ====================== cal:MyResponseType ============================ */

    /* ======================================================================
     *                    Scheduling properties
     * ====================================================================== */

    /* ========================= cal:Organizer ============================== */
    /* ==================== cal:RequiredAttendees =========================== */
    /* ==================== cal:OptionalAttendees =========================== */

    final NonEmptyArrayOfAttendeesType optAtts = cal.getOptionalAttendees();
    final NonEmptyArrayOfAttendeesType reqAtts = cal.getRequiredAttendees();

    if (((optAtts != null) && !optAtts.getAttendee().isEmpty()) ||
        ((reqAtts != null) && !reqAtts.getAttendee().isEmpty())) {
      // It's a meeting - add organizer and attendees
      pl.add(makeOrganizer(cal.getOrganizer(), true));

      if (optAtts != null) {
        for (final AttendeeType att: optAtts.getAttendee()) {
          pl.add(makeAttendee(att, true));
        }
      }

      if (reqAtts != null) {
        for (final AttendeeType att: optAtts.getAttendee()) {
          pl.add(makeAttendee(att, false));
        }
      }
    } else {
      // Not a meeting - save the MS organizer in an x-prop
      pl.add(makeOrganizer(cal.getOrganizer(), false));
    }

    /* ========================= cal:Resources ============================== */
    /* =================== cal:AdjacentMeetingCount ========================= */
    /* =================== cal:ConflictingMeetings ========================== */
    /* ===================== cal:AdjacentMeetings =========================== */
    /* ======================== cal:Duration ================================ */
    /* =================== cal:AppointmentReplyTime ========================= */
    /* ================= cal:AppointmentSequenceNumber ====================== */
    /* ==================== cal:AppointmentState ============================ */

    /* ======================================================================
     *                    Recurrring events
     * ====================================================================== */

    /* ==================== task:IsRecurring ================================ */
    /* ========================== RecurrenceId ============================== */
    /* ====================== cal:Recurrence ================================ */
    /* =================== cal:FirstOccurrence ============================== */
    /* =================== cal:LastOccurrence =============================== */
    /* ================== cal:ModifiedOccurrences =========================== */
    /* ================== cal:DeletedOccurrences ============================ */

    if (cal.isIsRecurring()) {

    }

    /* ==================== cal:MeetingTimeZone ============================= */
    /* ===================== cal:ConferenceType ============================= */
    /* =================== cal:AllowNewTimeProposal ========================= */
    /* ===================== cal:IsOnlineMeeting ============================ */
    /* ==================== cal:MeetingWorkspaceUrl ========================= */
    /* ====================== cal:NetShowUrl ================================ */

    /* ====================== Task items task: ============================== */
    /* ==================== task:ActualWork ================================= */
    /* ==================== task:AssignedTime =============================== */
    /* ==================== task:BillingInformation ========================= */
    /* ==================== task:ChangeCount ================================ */
    /* ==================== task:Companies ================================== */
    /* ==================== task:CompleteDate =============================== */
    /* ==================== task:Contacts =================================== */
    /* ==================== task:DelegationState ============================ */
    /* ==================== task:Delegator ================================== */
    /* ==================== task:DueDate ==================================== */
    /* ==================== task:IsAssignmentEditable ======================= */
    /* ==================== task:IsComplete ================================= */
    /* ==================== task:IsTeamTask ================================= */
    /* ==================== task:Mileage ==================================== */
    /* ==================== task:Owner ====================================== */
    /* ==================== task:PercentComplete ============================ */
    /* ==================== task:Recurrence ================================= */
    /* ==================== task:StartDate ================================== */
    /* ==================== task:Status ===================================== */
    /* status:
     * NotStarted
     * InProgress
     * Completed
     * WaitingOnOthers
     * Deferred
     * Setting CompleteDate has the same effect as setting PercentComplete to 100
     *  or Status to Completed. In a request that sets at least two of these
     *  properties, the last processed property will determine the value that
     *  is set for these elements. For example, if PercentComplete is 100,
     *  CompleteDate is 1/1/2007, and Status is NotStarted, and the
     *  properties are streamed in this order, the effect will be to set the
     *  Status of the task to NotStarted, the CompleteDate to null, and the
     *  PercentComplete to 0.
     */
    /* ==================== task:StatusDescription ========================== */
    /* ==================== task:TotalWork ================================== */

    /* ======================== item:MimeContent ============================ */
    /* ========================= item:ItemId ================================ */
    if (debug()) {
      debug("id: id=" + cal.getItemId().getId() +
            " ckey=" + cal.getItemId().getChangeKey());
    }

    /* ====================== item:ParentFolderId =========================== */

    /* ======================== item:ItemClass ============================== */

    if ((cal.getItemClass() != null) &
        !"PUBLIC".equalsIgnoreCase(cal.getItemClass())) {
      final ClassPropType cl = new ClassPropType();

      cl.setText(cal.getItemClass());

      final JAXBElement<ClassPropType> jaxb = xcalOF.createClass(cl);
      pl.add(jaxb);
    }

    /* ======================= item:Culture ================================= */
    /* ======================== item:Subject ================================ */

    final SummaryPropType sum = new SummaryPropType();
    /* XXX skip for the moment
    if (cal.getCulture() != null) {
      sum.setParameters(new ArrayOfParameters());

      LanguageParamType l = new LanguageParamType();
      l.setText(cal.getCulture());
      sum.getParameters().getBaseParameters().add(xcalOF.createLanguage(l));
    }
    */

    sum.setText(cal.getSubject());

    final var jaxbSum = xcalOF.createSummary(sum);
    pl.add(jaxbSum);

    /* ====================== item:Sensitivity ============================== */

    /* ========================= item:Body ================================== */

    final BodyType body = cal.getBody();

    if (body != null) {
      if (body.getValue() != null) {
        final DescriptionPropType desc = new DescriptionPropType();

        /* XXX Skip this for the moment - we need some chnages at the far end
        if (cal.getCulture() != null) {
          desc.setParameters(new ArrayOfParameters());

          LanguageParamType l = new LanguageParamType();
          l.setText(cal.getCulture());
          desc.getParameters().getBaseParameters().add(xcalOF.createLanguage(l));
        }
        */

        desc.setText(body.getValue());

        final var jaxbDesc = xcalOF.createDescription(desc);
        pl.add(jaxbDesc);
      }
    }

    /* ======================== item:Attachments ============================ */
    /* ====================== item:DateTimeReceived ========================= */
    /* ========================== item:Size ================================= */

    /* ======================= item:Categories ============================== */

    if ((cal.getCategories() != null) &&
        (!cal.getCategories().getString().isEmpty())) {
      final CategoriesPropType cp = new CategoriesPropType();
      for (final String s: cal.getCategories().getString()) {
        cp.getText().add(s);
      }

      final var jaxbCat = xcalOF.createCategories(cp);
      pl.add(jaxbCat);
    }

    /* ======================= item:Importance ============================== */
    /* ======================= item:InReplyTo =============================== */
    /* ======================= item:IsSubmitted ============================= */
    /* ========================= item:IsDraft =============================== */
    /* ======================== item:IsFromMe =============================== */
    /* ======================== item:IsResend =============================== */
    /* ====================== item:IsUnmodified ============================= */
    /* =================== item:InternetMessageHeaders ====================== */
    /* ====================== item:DateTimeSent ============================= */
    /* ===================== item:DateTimeCreated =========================== */
    /* ===================== item:ResponseObjects =========================== */
    /* ====================== item:ReminderDueBy ============================ */

    /* ====================== item:ReminderIsSet
                         item:ReminderMinutesBeforeStart ==================== */

    if (cal.isReminderIsSet()) {
      final ValarmType al = new ValarmType();
      final ArrayOfProperties alAop = new ArrayOfProperties();
      final var props = alAop.getBasePropertyOrTzid();

      al.setProperties(alAop);

      final ActionPropType act = new ActionPropType();
      act.setText("DISPLAY");
      final var jaxbAction = xcalOF.createAction(act);
      props.add(jaxbAction);

      final DescriptionPropType d = new DescriptionPropType();
      d.setText("REMINDER");
      final var jaxbDesc = xcalOF.createDescription(d);
      props.add(jaxbDesc);

      final TriggerPropType t = new TriggerPropType();
      t.setDuration("-PT" + cal.getReminderMinutesBeforeStart() +"M");
      final var jaxbTrig = xcalOF.createTrigger(t);
      props.add(jaxbTrig);

      final ArrayOfComponents comps = new ArrayOfComponents();
      comps.getBaseComponent().add(xcalOF.createValarm(al));

      if ((itemType == ItemType.Event) ||
          (itemType == ItemType.Task)) {
        comp.setComponents(comps);
      }
    }

    /* ======================= item:DisplayCc =============================== */
    /* ======================= item:DisplayTo =============================== */
    /* ===================== item:HasAttachments ============================ */
    /* ==================== item:ExtendedProperty =========================== */
    /* ==================== item:EffectiveRights ============================ */

    /* ==================== item:LastModifiedName =========================== */
    /* ==================== item:LastModifiedTime =========================== */

    final var lm  = new XBedeworkExsynchLastmodPropType();

    lm.setText(cal.getLastModifiedTime().toXMLFormat());
    final var jaxblm = xcalOF.createXBedeworkExsynchLastmod(lm);
    pl.add(jaxblm);

    /* ===================== item:IsAssociated ============================== */
    /* ================= item:WebClientReadFormQueryString ================== */
    /* ================= item:WebClientEditFormQueryString ================== */
    /* ====================== item:ConversationId =========================== */
    /* ====================== item:UniqueBody =============================== */

    if (statusVal != null) {
      final StatusPropType st = new StatusPropType();

      st.setText(statusVal);

      final var jaxb = xcalOF.createStatus(st);
      pl.add(jaxb);
    }

    if (transpVal != null) {
      final TranspPropType tr = new TranspPropType();

      tr.setText(transpVal);

      final JAXBElement<TranspPropType> jaxb = xcalOF.createTransp(tr);
      pl.add(jaxb);
    }

    /* ========================= MS X-props ========================== * /

    addXprop(pl, "X-MICROSOFT-CDO-APPT-SEQUENCE", cal.getAppointmentSequenceNumber());
    //addXprop("X-MICROSOFT-CDO-OWNERAPPTID", ?);
    addXprop(pl, "X-MICROSOFT-CDO-ALLDAYEVENT", cal.isIsAllDayEvent());
    //addXprop("X-MICROSOFT-CDO-IMPORTANCE:1", this);
    //addXprop("X-MICROSOFT-CDO-INSTTYPE:0", this);
    //addXprop("X-MICROSOFT-DISALLOW-COUNTER:FALSE", this);
     *
     */
    return ical;
  }

  private ItemType makeItemType(final String val) {
    final String uval = val.toUpperCase();

    return switch (uval) {
      case "IPM.NOTE" -> ItemType.Note;
      case "IPM.POST" -> ItemType.Post;
      case "IPM.APPOINTMENT" -> ItemType.Event;
      case "IPM.TASK" -> ItemType.Task;
      case "IPM.CONTACT" -> ItemType.Contact;
      case "IPM.ACTIVITY" -> ItemType.Journal;
      case "IPM.DISTLIST" -> ItemType.DistList;
      case "IPM.STICKYNOTE" -> ItemType.StickyNote;
      default -> throw new SynchException(
              SynchException.unknownCalendarItemType);
    };

  }

  /**
   * @author douglm
   */
  public static class TzStuff {
    String id;
    TimeZone tz;
  }

  /**
   * @param tzdef timezone definition
   * @param extzid
   * @return tz stuff
   */
  public TzStuff getTz(final TimeZoneDefinitionType tzdef,
                       final String extzid) {
    try {
      final TzStuff t = new TzStuff();

      if (tzdef != null) {
        t.id = tzdef.getId();
        if ((extzid != null) && (extzid.equals(t.id))) {
          t.id = null;
        } else {
          t.tz = tzGetter.getTz(t.id);
          return t;
        }
      }

      if (extzid != null) {
        t.tz = tzGetter.getTz(extzid);
      }

      return t;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  private JAXBElement<? extends OrganizerPropType>
                        makeOrganizer(final SingleRecipientType org,
                                      final boolean realOrg) {
    final OrganizerPropType prop = new OrganizerPropType();

    setNameAndAddress(prop, org.getMailbox());

    if (realOrg) {
      return xcalOF.createOrganizer(prop);
    }

    return xcalOF.createXBedeworkExsynchOrganizer(prop);
  }

  private JAXBElement<? extends AttendeePropType>
                        makeAttendee(final AttendeeType att,
                                     final boolean optional) {
    final AttendeePropType prop = new AttendeePropType();

    setNameAndAddress(prop, att.getMailbox());

    String partStat = null;
    if (att.getResponseType() == ResponseTypeType.TENTATIVE) {
      partStat = "TENTATIVE";
    } else if (att.getResponseType() == ResponseTypeType.ACCEPT) {
      partStat = "ACCEPTED";
    } else if (att.getResponseType() == ResponseTypeType.DECLINE) {
      partStat = "DECLINED";
    }

    if (partStat != null) {
      final PartstatParamType p = new PartstatParamType();
      p.setText(partStat);
      getParameters(prop).getBaseParameter().add(xcalOF.createPartstat(p));
    }

    if (optional) {
      final RoleParamType r = new RoleParamType();
      r.setText("OPT-PARTICIPANT");
      getParameters(prop).getBaseParameter().add(xcalOF.createRole(r));
    }

    return xcalOF.createAttendee(prop);
  }

  private void setNameAndAddress(final CalAddressPropertyType prop,
                                 final EmailAddressType ea) {
    final String name = ea.getName();

    if (name != null) {
      final CnParamType c = new CnParamType();
      c.setText(name);
      getParameters(prop).getBaseParameter().add(xcalOF.createCn(c));
    }

    String email = ea.getEmailAddress();
    if (!email.toLowerCase().startsWith("mailto:")) {
      email = "mailto:" + email;
    }

    prop.setCalAddress(email);
  }

  private ArrayOfParameters getParameters(final BasePropertyType prop) {
    ArrayOfParameters aop = prop.getParameters();

    if (aop == null) {
      aop = new ArrayOfParameters();
      prop.setParameters(aop);
    }

    return aop;
  }

  private enum Dtype {start, end, due;}

  private JAXBElement<? extends BasePropertyType> makeDateProp(final TimeZone tz,
                                      final XMLGregorianCalendar dt,
                                      final boolean allDay,
                                      final Dtype dtype) {
    try {
      if (dt == null) {
        return null;
      }

      final DateDatetimePropertyType prop;
      final JAXBElement<? extends BasePropertyType> jaxbProp;

      if (dtype == Dtype.start) {
        prop = new DtstartPropType();
        jaxbProp = xcalOF.createDtstart((DtstartPropType)prop);
      } else if (dtype == Dtype.due) {
        prop = new DuePropType();
        jaxbProp = xcalOF.createDue((DuePropType)prop);
      } else {
        prop = new DtendPropType();
        jaxbProp = xcalOF.createDtend((DtendPropType)prop);
      }

      if (allDay) {
        prop.setDate(dt);
      } else {
        prop.setDateTime(dt);
      }

      if (!allDay && (tz != null)) {
        final ArrayOfParameters aop = new ArrayOfParameters();

        final TzidParamType t = new TzidParamType();
        t.setText(tz.getID());
        aop.getBaseParameter().add(xcalOF.createTzid(t));

        prop.setParameters(aop);
      }

      return jaxbProp;
    } catch (final Throwable t) {
      throw new SynchException(t);
    }
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
