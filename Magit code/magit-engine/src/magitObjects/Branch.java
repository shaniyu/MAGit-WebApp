package magitObjects;

public class Branch {
    private String name;
    private Commit commit;
    private Boolean isRemote = false;

    // ctor for empty repository, no commits exist yes
    public Branch(String name) {
        this.name = name;
        commit = null;
    }

    // ctor fot repository that has at least one commit
    public Branch(String name, Commit commit)
    {
        this.name = name;
        this.commit = commit;
    }

    public Branch(){}

    public void setName(String name) {
        this.name = name;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public String getName() {
        return name;
    }

    public Commit getCommit() {
        return commit;
    }

    @Override
    public boolean equals(Object obj) {
        return ( obj instanceof Branch &&
                this.getName().equals(((Branch)obj).getName()) );
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(name);
    }

    public void setIsRemote(Boolean remote) {
        isRemote = remote;
    }

    public Boolean getIsRemote() {
        return isRemote;
    }
}
