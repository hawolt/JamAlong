package com.hawolt.os.process;

public abstract class ProcessReference {
    protected String name;
    protected int pid;

    public ProcessReference(String line) {
        this.configure(line);
    }

    public abstract void configure(String line);

    public int getPID() {
        return pid;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ProcessReference{" +
                "name='" + name + '\'' +
                ", pid=" + pid +
                '}';
    }
}
