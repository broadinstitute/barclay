package org.broadinstitute.barclay.utils;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Simple Pair class
 * @param left left of pair
 * @param right right of pair
 * @param <L> type of left
 * @param <R> type of right
 */
public record Pair<L,R>(L left, R right) implements Map.Entry<L,R>, Comparable<Pair<L,R>>, Serializable {

    @Serial
    private static final long serialVersionUID= 1;

    /**
     * @return left()
     */
    public L getLeft(){
        return left;
    }

    /**
     * @return right()
     */
    public R getRight() {
        return right;

    }

    /**
     * @return left()
     */
    @Override
    public L getKey() {
        return left;
    }

    /**
     * @return right()
     */
    @Override
    public R getValue() {
        return right;
    }

    /**
     * This Pair is immutable so the value cannot be set.
     *
     * @param value is irrelevant since this is immutable
     * @return UnsupporedOperationException
     * @throws UnsupportedOperationException
     */
    @Override
    public R setValue(final R value) {
        throw new UnsupportedOperationException("Pair is an immutable record");
    }


    /**
     * @return a new Pair of left and right
     */
    public static <L,R> Pair<L,R> of(L left, R right){
        return new Pair<>(left, right);
    }


    /**
     * First compare left, then compare right
     * @param other the Pair to be compared.
     */
    @Override
    public int compareTo(final Pair<L,R> other) {
        //This implemntation is take out of the commons lang Pair for consistency reasons
        return new CompareToBuilder().append(getLeft(), other.left)
                .append(getRight(), other.right).toComparison();
    }
}
