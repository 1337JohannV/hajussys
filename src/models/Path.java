package models;

public class Path {
    private String id;
    private Address download;
    private Address file;
    private boolean resolved = false;

    public Path(String id, Address download, Address file) {
        this.id = id;
        this.download = download;
        this.file = file;
    }

    public String getId() {
        return id;
    }

    public Address getDownload() {
        return download;
    }

    public void setDownload(Address download) {
        this.download = download;
    }

    public Address getFile() {
        return file;
    }

    public void setFile(Address file) {
        this.file = file;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public boolean equals(Object obj) {
        String objectString = (String) obj;
        if (obj == this) {
            return true;
        }
        return this.id.equals(objectString);
    }

    @Override
    public String toString() {
        return "Path{" +
                "id='" + id + '\'' +
                ", download=" + download +
                ", file=" + file +
                ", resolved=" + resolved +
                '}';
    }
}
