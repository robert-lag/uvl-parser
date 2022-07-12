package de.vill.model;

public class EquivalenceConstraint extends Constraint{
    private Constraint left;
    private Constraint right;

    public EquivalenceConstraint(Constraint left, Constraint right){
        this.left = left;
        this.right = right;
    }

    public Constraint getLeft() {
        return left;
    }

    public Constraint getRight() {
        return right;
    }

    @Override
    public String toString(boolean withSubmodels){
        StringBuilder result = new StringBuilder();
        result.append(left.toString(withSubmodels));
        result.append(" <=> ");
        result.append(right.toString(withSubmodels));
        return result.toString();
    }
}
