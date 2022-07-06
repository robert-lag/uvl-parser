package de.vill.model;

public class EqualEquationConstraint extends Constraint{
    private Expression left;
    private Expression right;

    public EqualEquationConstraint(Expression left, Expression right){
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append(left.toString());
        result.append(" == ");
        result.append(right.toString());
        return result.toString();
    }
}