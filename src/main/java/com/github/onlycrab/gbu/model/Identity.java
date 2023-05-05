package com.github.onlycrab.gbu.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Gitlab user LDAP identity.
 *
 * @author Roman Rynkovich
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Identity {
    private String provider;

    @SerializedName("extern_uid")
    private String externUid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return Objects.equals(provider, identity.provider) &&
                Objects.equals(externUid, identity.externUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, externUid);
    }
}
