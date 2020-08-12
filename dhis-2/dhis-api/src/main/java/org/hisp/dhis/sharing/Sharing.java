package org.hisp.dhis.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Sharing
    implements Serializable
{
    private static final long serialVersionUID = 6977793211734844477L;

    @JsonProperty
    private String owner;

    @JsonProperty
    private String publicAccess;

    @JsonProperty
    private boolean external;

    @JsonProperty
    private Map<String, UserSharing> users;

    @JsonProperty
    private Map<String, UserGroupSharing> userGroups;

}

