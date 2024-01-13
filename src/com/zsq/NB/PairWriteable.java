package com.zsq.NB;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class PairWriteable implements WritableComparable<PairWriteable> {

    public abstract class Pair<F, S> {
        private F first;
        private S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }

    private String first;
    private String second;

    public PairWriteable() {
        this.first = "";
    }

    public PairWriteable(String a, String b) {
        set(a, b);
    }

    public void set(String a, String b) {
        this.first = a;
        this.second = b;
    }

    public String get() {
        return this.toString();
    }

    public String getFirst() {
        return this.first;
    }

    public String getSecond() {
        return this.second;
    }
    // get

    @Override
    public void readFields(DataInput in) throws IOException {
        first = in.readUTF();
        second = in.readUTF();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(first);
        out.writeUTF(second);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PairWriteable) {
            PairWriteable that = (PairWriteable) o;
            return first.equals(that.first) && second.equals(that.second);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }

    @Override
    public int compareTo(PairWriteable o) {
        String thisValue = this.toString();
        String thatValue = o.toString();
        return thisValue.compareTo(thatValue);
    }

    @Override
    public String toString() {
        return first + "\t" + second;
    }

    public static class Comparator extends WritableComparator {
        public Comparator() {
            super(PairWriteable.class);
        }

        @Override
        public int compare(byte[] b1, int s1, int l1,
                byte[] b2, int s2, int l2) {
            String thisValue = new String(b1, s1, l1);
            String thatValue = new String(b2, s2, l2);
            return thisValue.compareTo(thatValue);
        }
    }

    public static class DecreasingComparator extends Comparator {

        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return super.compare(b, a);
        }

        @Override
        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return super.compare(b2, s2, l2, b1, s1, l1);
        }
    }

    static { // register default comparator
        WritableComparator.define(PairWriteable.class, new Comparator());
    }
}
