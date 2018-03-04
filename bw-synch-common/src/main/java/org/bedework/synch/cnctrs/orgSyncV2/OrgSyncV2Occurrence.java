/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.synch.cnctrs.orgSyncV2;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User: mike Date: 10/18/17 Time: 00:35
 */
public class OrgSyncV2Occurrence {
  @JsonProperty("starts_at")
  private String startsAt;

  @JsonProperty("ends_at")
  private String endsAt;

  @JsonProperty("is_all_day")
  private boolean isAllDay;

  public String getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(final String val) {
    startsAt = val;
  }

  public String getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(final String val) {
    endsAt = val;
  }

  public boolean isAllDay() {
    return isAllDay;
  }

  public void setAllDay(final boolean val) {
    isAllDay = val;
  }
}
