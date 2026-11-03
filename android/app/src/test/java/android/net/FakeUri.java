package android.net;

import java.util.Collections;
import java.util.List;

// JVM unit tests only: android.jar stubs leave Uri.EMPTY null and Uri's constructor
// package-private, so tests build MediaItem with this inert Uri instead.
public final class FakeUri extends Uri {
    public static final FakeUri INSTANCE = new FakeUri();

    private FakeUri() {}

    @Override public Builder buildUpon() { return null; }
    @Override public String getAuthority() { return null; }
    @Override public String getEncodedAuthority() { return null; }
    @Override public String getEncodedFragment() { return null; }
    @Override public String getEncodedPath() { return null; }
    @Override public String getEncodedQuery() { return null; }
    @Override public String getEncodedSchemeSpecificPart() { return null; }
    @Override public String getEncodedUserInfo() { return null; }
    @Override public String getFragment() { return null; }
    @Override public String getHost() { return null; }
    @Override public String getLastPathSegment() { return null; }
    @Override public String getPath() { return null; }
    @Override public List<String> getPathSegments() { return Collections.emptyList(); }
    @Override public int getPort() { return -1; }
    @Override public String getQuery() { return null; }
    @Override public String getScheme() { return null; }
    @Override public String getSchemeSpecificPart() { return null; }
    @Override public String getUserInfo() { return null; }
    @Override public boolean isHierarchical() { return false; }
    @Override public boolean isRelative() { return true; }
    @Override public String toString() { return "fake://"; }
    @Override public boolean equals(Object other) { return other == this; }
    @Override public int hashCode() { return 0; }
    @Override public int compareTo(Uri other) { return other == this ? 0 : -1; }
    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(android.os.Parcel dest, int flags) {}
}
