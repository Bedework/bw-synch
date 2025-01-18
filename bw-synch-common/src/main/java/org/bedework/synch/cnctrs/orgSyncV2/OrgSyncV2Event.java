/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.synch.cnctrs.orgSyncV2;

import org.bedework.base.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * User: mike Date: 10/18/17 Time: 00:35
 */
public class OrgSyncV2Event {
  private int id;

  @JsonProperty("is_public")
  private boolean isPublic;

  private String name;

  private String location;

  @JsonProperty("is_approved")
  private boolean isApproved;

  private OrgSyncV2Category category;

  @JsonProperty("umbrella_category")
  private OrgSyncV2Category umbrellaCategory;

  private String description;

  @JsonProperty("html_description")
  private String htmlDescription;

  private int rsvps;

  @JsonProperty("org_id")
  private int orgId;

  private List<OrgSyncV2Occurrence> occurrences;

  public int getId() {
    return id;
  }

  public void setId(final int val) {
    id = val;
  }

  public boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(final boolean val) {
    isPublic = val;
  }

  public String getName() {
    return name;
  }

  public void setName(final String val) {
    name = val;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String val) {
    location = val;
  }

  public boolean isApproved() {
    return isApproved;
  }

  public void setApproved(final boolean val) {
    isApproved = val;
  }

  public OrgSyncV2Category getCategory() {
    return category;
  }

  public void setCategory(
          final OrgSyncV2Category val) {
    category = val;
  }

  public OrgSyncV2Category getUmbrellaCategory() {
    return umbrellaCategory;
  }

  public void setUmbrellaCategory(
          final OrgSyncV2Category val) {
    umbrellaCategory = val;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String val) {
    description = val;
  }

  public String getHtmlDescription() {
    return htmlDescription;
  }

  public void setHtmlDescription(final String val) {
    htmlDescription = val;
  }

  public int getRsvps() {
    return rsvps;
  }

  public void setRsvps(final int val) {
    rsvps = val;
  }

  public int getOrgId() {
    return orgId;
  }

  public void setOrgId(final int val) {
    orgId = val;
  }

  public List<OrgSyncV2Occurrence> getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(
          final List<OrgSyncV2Occurrence> val) {
    occurrences = val;
  }

  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("id", getId());
    ts.append("isPublic", getIsPublic());
    ts.append("name", getName());
    ts.append("location", getLocation());
    ts.append("isApproved", isApproved());
    ts.append("category", getCategory());
    ts.append("umbrellaCategory", getUmbrellaCategory());
    ts.append("description", getDescription());
    ts.append("htmlDescription", getHtmlDescription());
    ts.append("rsvps", getRsvps());
    ts.append("orgId", getOrgId());
    ts.append("occurrences", getOccurrences());

    return ts.toString();
  }
}
