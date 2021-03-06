package com.ibm.airlock.common.util;

/**
 * Created by Denis Voloshin on 02/11/2017.
 */

@SuppressWarnings("ClassOnlyUsedInOnePackage")
public class Pair<L, R> {

    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static Pair create(String left, String right) {
        return new Pair(left, right);
    }

    private L getLeft() { return left; }

    private R getRight() { return right; }

    @Override
    public int hashCode() { return left.hashCode() ^ right.hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair pairo = (Pair) o;
        return this.left.equals(pairo.getLeft()) &&
                this.right.equals(pairo.getRight());
    }
}
